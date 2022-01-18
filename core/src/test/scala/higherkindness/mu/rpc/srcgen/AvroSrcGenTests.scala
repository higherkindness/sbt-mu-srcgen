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
import higherkindness.mu.rpc.srcgen.Generator.Result
import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import higherkindness.mu.rpc.srcgen.AvroScalaGeneratorArbitrary._
import higherkindness.mu.rpc.srcgen.Model.SerializationType.Avro
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.avro._
import higherkindness.skeuomorph.mu.CompressionType
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class AvroSrcGenTests extends AnyWordSpec with Matchers with OneInstancePerTest with Checkers {

  def generateOutput(
      serializationType: SerializationType,
      marshallersImports: List[MarshallersImport],
      compressionType: CompressionType,
      useIdiomaticEndpoints: Boolean = true,
      messagesAsImportFile: Boolean = true
  ): List[String] = {

    val _ = marshallersImports

    val imports: String = "import _root_.higherkindness.mu.rpc.protocol._"

    val serviceParams: String = Seq(
      serializationType.toString,
      s"compressionType = $compressionType",
      if (useIdiomaticEndpoints) s"""namespace = Some("foo.bar")"""
      else "namespace = None"
    ).mkString(", ")

    val packageAndImports =
      s"""
         |package foo.bar
         |
         |$imports
         |
         |""".stripMargin

    val messages =
      """
        |final case class HelloRequest(arg1: _root_.java.lang.String, arg2: _root_.scala.Option[_root_.java.lang.String], arg3: _root_.scala.List[_root_.java.lang.String])
        |
        |final case class HelloResponse(arg1: _root_.java.lang.String, arg2: _root_.scala.Option[_root_.java.lang.String], arg3: _root_.scala.List[_root_.java.lang.String])
        |
        |""".stripMargin

    val service =
      s"""
        |@service($serviceParams) trait MyGreeterService[F[_]] {
        |
        |  def sayHelloAvro(req: _root_.foo.bar.HelloRequest): F[_root_.foo.bar.HelloResponse]
        |
        |  def sayNothingAvro(req: _root_.higherkindness.mu.rpc.protocol.Empty.type): F[_root_.higherkindness.mu.rpc.protocol.Empty.type]
        |}
        |
        |""".stripMargin

    if (messagesAsImportFile)
      (packageAndImports ++ service).split("\n").filter(_.nonEmpty).toList
    else
      (packageAndImports ++ messages ++ service).split("\n").filter(_.nonEmpty).toList
  }

  implicit val scenarioArb: Arbitrary[Scenario] = scenarioArbitrary(generateOutput)

  "Avro Scala Generator" should {

    "generate correct Scala classes" in {
      check {
        forAll { scenario: Scenario =>
          test(scenario)
        }
      }
    }

    "return a non-empty list of errors instead of generating code from an invalid IDL file" in {
      val actual :: Nil = {
        AvroSrcGenerator(CompressionType.Identity, true).generateFrom(
          Set(new File(getClass.getResource("/avro/Invalid.avdl").toURI)),
          Avro
        )
      }

      actual.inputFile.getPath should endWith("/avro/Invalid.avdl")

      actual.output shouldEqual Invalid(
        NonEmptyList.one(
          "Encountered an unsupported response type: Skeuomorph only supports Record types for Avro responses. Encountered response schema with type STRING"
        )
      )
    }
  }

  private def test(scenario: Scenario): Boolean = {
    val output =
      AvroSrcGenerator(
        scenario.compressionType,
        scenario.useIdiomaticEndpoints
      ).generateFrom(
        scenario.inputResourcesPath.map(path => new File(getClass.getResource(path).toURI)),
        scenario.serializationType
      )
    output should not be empty
    output forall { case Result(_, contents) =>
      contents.map(_.path.toString) shouldBe Valid(scenario.expectedOutputFilePath)
      contents.map(_.contents) shouldBe Valid(scenario.expectedOutput)
      true
    }
  }

}
