/*
 * Copyright 2020-2022 47 Degrees <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.mu.rpc.srcgen.avro.rewrites

import scalafix.v1._
import scala.meta._
import scala.meta.Type.{ApplyInfix => Infix}

class ReplaceShapelessCoproduct extends SyntacticRule("ReplaceShapelessCoproduct") {

  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    /*
     * This rule rewrites shapeless Coproducts, e.g.
     *
     * {{{
     * Int :+: String :+: MyRecord :+: CNil
     * }}}
     *
     * to Mu's wrappers of Scala 3 union types:
     *
     * {{{
     * AvroUnion3[String, Int, MyRecord]
     * }}}
     *
     */
    doc.tree.collect {
      // format: off

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", t"CNil"))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion2[$t1, $t2]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", Infix(t3, t":+:", t"CNil")))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion3[$t1, $t2, $t3]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", Infix(t3, t":+:", Infix(t4, t":+:", t"CNil"))))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion4[$t1, $t2, $t3, $t4]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", Infix(t3, t":+:", Infix(t4, t":+:", Infix(t5, t":+:", t"CNil")))))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion5[$t1, $t2, $t3, $t4, $t5]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", Infix(t3, t":+:", Infix(t4, t":+:", Infix(t5, t":+:", Infix(t6, t":+:", t"CNil"))))))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion6[$t1, $t2, $t3, $t4, $t5, $t6]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", Infix(t3, t":+:", Infix(t4, t":+:", Infix(t5, t":+:", Infix(t6, t":+:", Infix(t7, t":+:", t"CNil")))))))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion7[$t1, $t2, $t3, $t4, $t5, $t6, $t7]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      case t @ Term.Param(
        _,
        _,
        Some(
          Infix(t1, t":+:", Infix(t2, t":+:", Infix(t3, t":+:", Infix(t4, t":+:", Infix(t5, t":+:", Infix(t6, t":+:", Infix(t7, t":+:", Infix(t8, t":+:", t"CNil"))))))))
        ),
        _
      ) =>
        val unionType = t"_root_.higherkindness.mu.rpc.avro.AvroUnion8[$t1, $t2, $t3, $t4, $t5, $t6, $t7, $t8]"
        Patch.replaceTree(t, t.copy(decltpe = Some(unionType)).syntax)

      // format: on
    }.asPatch
  }

}
