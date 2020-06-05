/*
 * Copyright 2020 47 Degrees <https://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen

import org.scalatest.OptionValues
import java.io.File

import higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator
import higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator.HttpImpl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Matchers._

class OpenApiSrcGenTests extends AnyFlatSpec with OptionValues {
  val module: String       = new java.io.File(".").getCanonicalPath
  val resourcesFiles: File = new File(module + "/core/src/test/resources/")
  val openApiFile          = new File(resourcesFiles.getPath() ++ "/openapi/bookstore/book.yaml")

  it should "generate correct Scala" in {
    val (_, path, code) = OpenApiSrcGenerator(HttpImpl.Http4sV20, resourcesFiles.toPath())
      .generateFrom(Set(openApiFile), Model.SerializationType.Custom)
      .headOption
      .value
    path should ===("bookstore/book.scala")
    code should ===(
      List(
        "package bookstore",
        """|object models {
           |import shapeless.{:+:, CNil}
           |import shapeless.Coproduct
           |final case class Book(isbn: Long, title: String)
           |object Book {
           |
           |  import io.circe._
           |  import io.circe.generic.semiauto._
           |  import org.http4s.{EntityEncoder, EntityDecoder}
           |  import org.http4s.circe._
           |  import cats.Applicative
           |  import cats.effect.Sync
           |  implicit val BookEncoder: Encoder[Book] = deriveEncoder[Book]
           |  implicit val BookDecoder: Decoder[Book] = deriveDecoder[Book]
           |  implicit def BookEntityEncoder[F[_]:Applicative]: EntityEncoder[F, Book] = jsonEncoderOf[F, Book]
           |  implicit def OptionBookEntityEncoder[F[_]:Applicative]: EntityEncoder[F, Option[Book]] = jsonEncoderOf[F, Option[Book]]
           |  implicit def BookEntityDecoder[F[_]:Sync]: EntityDecoder[F, Book] = jsonOf[F, Book]
           |
           |}
           |}""".stripMargin
      )
    )
  }

}
