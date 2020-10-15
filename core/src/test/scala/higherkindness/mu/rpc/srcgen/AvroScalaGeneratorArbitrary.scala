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

import higherkindness.mu.rpc.srcgen.Model.SerializationType._
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.skeuomorph.mu.CompressionType
import org.scalacheck._

trait AvroScalaGeneratorArbitrary {

  case class Scenario(
      inputResourcePath: String,
      expectedOutput: List[String],
      expectedOutputFilePath: String,
      serializationType: SerializationType,
      compressionType: CompressionType,
      streamingImplementation: StreamingImplementation,
      useIdiomaticEndpoints: Boolean = true
  )

  def generateOutput(
      serializationType: SerializationType,
      compressionType: CompressionType,
      useIdiomaticEndpoints: Boolean = true
  ): List[String] = {

    val imports: String = "import _root_.higherkindness.mu.rpc.protocol._"

    val serviceParams: String = Seq(
      serializationType.toString,
      s"compressionType = $compressionType",
      if (useIdiomaticEndpoints) s"""namespace = Some("foo.bar")"""
      else "namespace = None"
    ).mkString(", ")

    s"""
         |package foo.bar
         |
         |$imports
         |
         |final case class HelloRequest(arg1: _root_.java.lang.String, arg2: _root_.scala.Option[_root_.java.lang.String], arg3: _root_.scala.List[_root_.java.lang.String])
         |
         |final case class HelloResponse(arg1: _root_.java.lang.String, arg2: _root_.scala.Option[_root_.java.lang.String], arg3: _root_.scala.List[_root_.java.lang.String])
         |

         |@service($serviceParams) trait MyGreeterService[F[_]] {
         |
         |  def sayHelloAvro(req: _root_.foo.bar.HelloRequest): F[_root_.foo.bar.HelloResponse]
         |
         |  def sayNothingAvro(req: _root_.higherkindness.mu.rpc.protocol.Empty.type): F[_root_.higherkindness.mu.rpc.protocol.Empty.type]
         |
         |}""".stripMargin.split("\n").filter(_.length > 0).toList
  }

  val importSliceGen: Gen[String] =
    Gen.choose(4, 10).flatMap(Gen.listOfN(_, Gen.alphaLowerChar).map(_.mkString("")))

  val customMarshallersImportsGen: Gen[MarshallersImport] =
    Gen
      .choose(1, 5)
      .flatMap(
        Gen.listOfN(_, importSliceGen).map(_.mkString(".") + "._").map(CustomMarshallersImport)
      )

  implicit val scenarioArb: Arbitrary[Scenario] = Arbitrary {
    for {
      inputResourcePath       <- Gen.oneOf("/avro/GreeterService.avpr", "/avro/GreeterService.avdl")
      serializationType       <- Gen.const(Avro)
      compressionType         <- Gen.oneOf(CompressionType.Gzip, CompressionType.Identity)
      streamingImplementation <- Gen.oneOf(MonixObservable, Fs2Stream)
      useIdiomaticEndpoints   <- Arbitrary.arbBool.arbitrary
    } yield Scenario(
      inputResourcePath,
      generateOutput(
        serializationType,
        compressionType,
        useIdiomaticEndpoints
      ),
      "foo/bar/MyGreeterService.scala",
      serializationType,
      compressionType,
      streamingImplementation,
      useIdiomaticEndpoints
    )
  }

}

object AvroScalaGeneratorArbitrary extends AvroScalaGeneratorArbitrary
