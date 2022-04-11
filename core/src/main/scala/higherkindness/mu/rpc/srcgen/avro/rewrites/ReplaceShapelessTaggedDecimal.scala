package higherkindness.mu.rpc.srcgen.avro.rewrites

import scalafix.v1._
import scala.meta._

class ReplaceShapelessTaggedDecimal extends SyntacticRule("ReplaceShapelessTaggedDecimal") {

  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    /*
     * This rule rewrites shapeless-tagged BigDecimal, e.g.
     *
     * {{{
     * @@[BigDecimal, (shapeless.Nat._6, shapeless.Nat._2)]
     * }}}
     *
     * to Mu's tagged decimal type:
     *
     * {{{
     * TaggedDecimal[6, 2]
     * }}}
     *
     */
    doc.tree.collect {
      // format: off

      case t @ Type.Apply(
        t"@@",
        t"scala.math.BigDecimal" :: Type.Tuple(Type.Select(_, precisionNat) :: Type.Select(_, scaleNat) :: Nil) :: Nil
      ) =>
        val precision = precisionNat.value.stripPrefix("_")
        val scale = scaleNat.value.stripPrefix("_")
        val taggedDecimalType = s"_root_.higherkindness.mu.rpc.avro.Decimals.TaggedDecimal[$precision, $scale]"
        Patch.replaceTree(t, taggedDecimalType)

      // format: on
    }.asPatch
  }

}
