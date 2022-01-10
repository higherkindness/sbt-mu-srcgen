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

package higherkindness.mu.rpc.srcgen

import java.io.File
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.proto.ProtoSrcGenerator
import higherkindness.skeuomorph.ProtobufCompilationException
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProtoSrcGenTests extends AnyWordSpec with Matchers with OneInstancePerTest with OptionValues {

  val protocVersion  = Some("3.19.1")
  val module: String = new java.io.File(".").getCanonicalPath
  def protoFile(filename: String): File =
    new File(s"$module/core/src/test/resources/proto/$filename.proto")

  "Proto Scala Generator" should {

    "generate the expected Scala code (FS2 stream)" in {
      val result: Option[(String, String)] =
        ProtoSrcGenerator(Fs2Stream, protocVersion = protocVersion)
          .generateFrom(
            files = Set(protoFile("book")),
            serializationType = SerializationType.Protobuf
          )
          .flatMap { r =>
            r.output.toOption.map(o => (o.path.toString, o.contents.mkString("\n").clean))
          }
          .headOption

      val expectedFileContent = bookExpectation(tpe => s"_root_.fs2.Stream[F, $tpe]").clean
      result shouldBe Some(("com/proto/book.scala", expectedFileContent.clean))
    }

    "generate the expected Scala code (Monix Observable)" in {
      val result: Option[(String, String)] =
        ProtoSrcGenerator(MonixObservable, protocVersion = protocVersion)
          .generateFrom(
            files = Set(protoFile("book")),
            serializationType = SerializationType.Protobuf
          )
          .flatMap { r =>
            r.output.toOption.map(o => (o.path.toString, o.contents.mkString("\n").clean))
          }
          .headOption

      val expectedFileContent =
        bookExpectation(tpe => s"_root_.monix.reactive.Observable[$tpe]").clean

      result shouldBe Some(("com/proto/book.scala", expectedFileContent))
    }

    "throw an exception on an invalid Protobuf schema" in {
      assertThrows[ProtobufCompilationException] {
        ProtoSrcGenerator(MonixObservable, protocVersion = protocVersion)
          .generateFrom(
            files = Set(protoFile("broken")),
            serializationType = SerializationType.Protobuf
          )
      }
    }

  }

  def bookExpectation(streamOf: String => String): String =
    s"""package com.proto
       |
       |import _root_.higherkindness.mu.rpc.protocol._
       |
       |object book {
       |
       |final case class Book(
       |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long,
       |  @_root_.pbdirect.pbIndex(2) title: _root_.java.lang.String,
       |  @_root_.pbdirect.pbIndex(3) author: _root_.scala.List[_root_.com.proto.author.Author],
       |  @_root_.pbdirect.pbIndex(9) binding_type: _root_.scala.Option[_root_.com.proto.book.BindingType]
       |)
       |final case class GetBookRequest(
       |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long
       |)
       |final case class GetBookViaAuthor(
       |  @_root_.pbdirect.pbIndex(1) author: _root_.scala.Option[_root_.com.proto.author.Author]
       |)
       |final case class BookStore(
       |  @_root_.pbdirect.pbIndex(1) name: _root_.java.lang.String,
       |  @_root_.pbdirect.pbIndex(2) books: _root_.scala.Predef.Map[_root_.scala.Long, _root_.java.lang.String],
       |  @_root_.pbdirect.pbIndex(3) genres: _root_.scala.List[_root_.com.proto.book.Genre],
       |  @_root_.pbdirect.pbIndex(4,5,6,7) payment_method: _root_.scala.Option[
       |    _root_.shapeless.:+:[
       |      _root_.scala.Long,
       |      _root_.shapeless.:+:[
       |        _root_.scala.Int,
       |        _root_.shapeless.:+:[
       |          _root_.java.lang.String,
       |          _root_.shapeless.:+:[
       |            _root_.com.proto.book.Book,
       |            _root_.shapeless.CNil]]]]]
       |)
       |
       |sealed abstract class Genre(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
       |object Genre extends _root_.enumeratum.values.IntEnum[Genre] {
       |  case object UNKNOWN extends Genre(0)
       |  case object SCIENCE_FICTION extends Genre(1)
       |  case object POETRY extends Genre(2)
       |
       |  val values = findValues
       |}
       |
       |sealed abstract class BindingType(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
       |object BindingType extends _root_.enumeratum.values.IntEnum[BindingType] {
       |  case object HARDCOVER extends BindingType(0)
       |  case object PAPERBACK extends BindingType(1)
       |
       |  val values = findValues
       |}
       |
       |@service(Protobuf, compressionType=Identity, namespace=Some("com.proto")) trait BookService[F[_]] {
       |  def GetBook(req: _root_.com.proto.book.GetBookRequest): F[_root_.com.proto.book.Book]
       |  def GetBooksViaAuthor(req: _root_.com.proto.book.GetBookViaAuthor): F[${streamOf(
      "_root_.com.proto.book.Book"
    )}]
       |  def GetGreatestBook(req: ${streamOf(
      "_root_.com.proto.book.GetBookRequest"
    )}): F[_root_.com.proto.book.Book]
       |  def GetBooks(req: ${streamOf("_root_.com.proto.book.GetBookRequest")}): F[${streamOf(
      "_root_.com.proto.book.Book"
    )}]
       |}
       |
       |}""".stripMargin

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
