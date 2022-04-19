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
     * @@[BigDecimal, ((shapeless.Nat._1, shapeless.Nat._6), shapeless.Nat._2)]
     * @@[BigDecimal, ((shapeless.Nat._1, shapeless.Nat._6), (shapeless.Nat._1, shapeless.Nat._2))]
     * }}}
     *
     * to Mu's tagged decimal type:
     *
     * {{{
     * TaggedDecimal[6, 2]
     * TaggedDecimal[16, 2]
     * TaggedDecimal[16, 12]
     * }}}
     *
     */
    doc.tree.collect {
      // format: off

      case t @ Type.Apply(
        t"@@",
        List(
          t"scala.math.BigDecimal",
          Type.Tuple(
            List(
              Type.Select(_, precisionNat),
              Type.Select(_, scaleNat)
            )
          )
        )
      ) =>
        val precision = precisionNat.value.stripPrefix("_").toInt
        val scale = scaleNat.value.stripPrefix("_").toInt
        Patch.replaceTree(t, taggedDecimalType(precision, scale))

      case t @ Type.Apply(
        t"@@",
        List(
          t"scala.math.BigDecimal",
          Type.Tuple(
            List(
              Type.Tuple(Type.Select(_, precisionNat1) :: Type.Select(_, precisionNat2) :: Nil),
              Type.Select(_, scaleNat)
            )
          )
        )
      ) =>
        val precision = precisionNat1.value.stripPrefix("_").toInt * 10 + precisionNat2.value.stripPrefix("_").toInt
        val scale = scaleNat.value.stripPrefix("_").toInt
        Patch.replaceTree(t, taggedDecimalType(precision, scale))

      case t @ Type.Apply(
        t"@@",
        List(
          t"scala.math.BigDecimal",
          Type.Tuple(
            List(
              Type.Tuple(Type.Select(_, precisionNat1) :: Type.Select(_, precisionNat2) :: Nil),
              Type.Tuple(Type.Select(_, scaleNat1) :: Type.Select(_, scaleNat2) :: Nil)
            )
          )
        )
      ) =>
        val precision = precisionNat1.value.stripPrefix("_").toInt * 10 + precisionNat2.value.stripPrefix("_").toInt
        val scale = scaleNat1.value.stripPrefix("_").toInt * 10 + scaleNat2.value.stripPrefix("_").toInt
        Patch.replaceTree(t, taggedDecimalType(precision, scale))

      // format: on
    }.asPatch
  }

  private def taggedDecimalType(precision: Int, scale: Int): String =
    s"_root_.higherkindness.mu.rpc.avro.Decimals.TaggedDecimal[$precision, $scale]"

}
