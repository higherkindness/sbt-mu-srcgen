package higherkindness.mu.rpc.srcgen.avro.rewrites

import scalafix.v1._
import scala.meta._

class ReplaceShapelessCoproduct extends SyntacticRule("ReplaceShapelessCoproduct") {

  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case t @ Term.Param(
        _,
        _,
        Some(
          Type.ApplyInfix(
            t1,
            Type.Name(":+:"),
            Type.ApplyInfix(
              t2,
              Type.Name(":+:"),
              Type.ApplyInfix(
                t3,
                Type.Name(":+:"),
                Type.Name("CNil")
              )
            )
          )
        ),
        _
      ) =>
        println(s"Replacing a shapeless Coproduct: ${t.syntax}")
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion3[$t1, $t2, $t3]"
        val updatedParam = t.copy(decltpe = Some(unionType))
        Patch.replaceTree(t, updatedParam.syntax)
    }.asPatch
  }

}
