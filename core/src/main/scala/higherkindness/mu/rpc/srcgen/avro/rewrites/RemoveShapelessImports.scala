package higherkindness.mu.rpc.srcgen.avro.rewrites

import scalafix.v1._
import scala.meta._

class RemoveShapelessImports extends SyntacticRule("RemoveShapelessImports") {

  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case t @ Import(importersnel) if importersnel.head.syntax.startsWith("shapeless.") =>
        Patch.removeTokens(t.tokens)
    }.asPatch
  }

}
