package higherkindness.mu.rpc.srcgen.avro.rewrites

import scalafix.v1._
import scala.meta._

class AddAvroOrderingAnnotations extends SyntacticRule("AddAvroOrderingAnnotations") {

  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    /*
     * This rule rewrites:
     *
     * {{{
     * object Suit {
     *   case object SPADES extends Suit
     *   case object HEARTS extends Suit
     *   case object DIAMONDS extends Suit
     *   case object CLUBS extends Suit
     * }
     * }}}
     *
     * to:
     *
     * {{{
     * object Suit {
     *   @AvroSortPriority(0) case object SPADES extends Suit
     *   @AvroSortPriority(1) case object HEARTS extends Suit
     *   @AvroSortPriority(2) case object DIAMONDS extends Suit
     *   @AvroSortPriority(3) case object CLUBS extends Suit
     * }
     * }}}
     *
     * so that avro4s preserves the correct order of the Avro enum.
     */
    doc.tree.collect {
      case Defn.Object(
            _,
            Term.Name(objName),
            Template(
              _,
              _,
              _,
              stats
            )
          ) =>
        val patches = stats.zipWithIndex.map {
          case (
                t @ Defn.Object(
                  mods,
                  _,
                  Template(
                    _,
                    Init(Type.Name(parentName), _, _) :: Nil,
                    _,
                    _
                  )
                ),
                i
              ) if mods.exists(_.is[Mod.Case]) && parentName == objName =>
            Patch.addLeft(t, s"@_root_.com.sksamuel.avro4s.AvroSortPriority($i) ")
          case _ =>
            Patch.empty
        }
        Patch.fromIterable(patches)
    }.asPatch
  }

}
