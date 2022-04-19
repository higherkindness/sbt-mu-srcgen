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

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import higherkindness.mu.rpc.srcgen.AvroScalaGeneratorArbitrary._
import higherkindness.mu.rpc.srcgen.Generator.Result
import higherkindness.mu.rpc.srcgen.Model.SerializationType.Avro
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.avro._
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

import java.io.File

class AvrohuggerSrcGenTests
    extends AnyWordSpec
    with Matchers
    with OneInstancePerTest
    with Checkers {

  def generateOutput(
      serializationType: SerializationType,
      marshallersImports: List[MarshallersImport],
      messagesAsImportFile: Boolean = true
  ): List[String] = {

    val imports: String = marshallersImports
      .map(mi => s"import ${mi.marshallersImport}")
      .sorted
      .mkString("\n")

    val packageAndImports =
      s"""
         |package foo.bar
         |
         |$imports
         |
         |""".stripMargin

    val messages =
      """
        |final case class HelloRequest(arg1: String, arg2: Option[String], arg3: Seq[String])
        |
        |final case class HelloResponse(arg1: String, arg2: Option[String], arg3: Seq[String])
        |
        |""".stripMargin

    val serviceTrait =
      s"""
         |trait MyGreeterService[F[_]] {
         |
         |  def sayHelloAvro(arg: foo.bar.HelloRequest): F[foo.bar.HelloResponse]
         |
         |  def sayNothingAvro(arg: _root_.higherkindness.mu.rpc.protocol.Empty.type): F[_root_.higherkindness.mu.rpc.protocol.Empty.type]
         |}
         |
         |""".stripMargin

    // Checking the whole content of the companion object would be pretty
    // painful and would duplicate the checks we do in
    // CompanionObjectGeneratorSpec and the scripted tests, so we only check
    // the first couple of lines
    val startOfCompanionObject =
      s"""
         |object MyGreeterService {
         |  import _root_.higherkindness.mu.rpc.internal.encoders.${serializationType.toString.toLowerCase}._
         |""".stripMargin

    if (messagesAsImportFile)
      (packageAndImports ++ serviceTrait ++ startOfCompanionObject)
        .split("\n")
        .filter(_.nonEmpty)
        .toList
    else
      (packageAndImports ++ messages ++ serviceTrait ++ startOfCompanionObject)
        .split("\n")
        .filter(_.nonEmpty)
        .toList
  }

  implicit val scenarioArb: Arbitrary[Scenario] =
    scenarioArbitrary(generateOutput)

  "Avro Scala Generator" should {

    "generate correct Scala classes" in {
      check {
        forAll { scenario: Scenario => test(scenario) }
      }
    }

    "return a non-empty list of errors instead of generating code from an invalid IDL file" in {
      val actual :: Nil = {
        AvrohuggerSrcGenerator(
          marshallersImports = List(BigDecimalTaggedAvroMarshallers),
          compressionType = NoCompressionGen,
          serializationType = Avro,
          useIdiomaticEndpoints = true,
          scala3 = false
        ).generateFromFiles(
          Set(new File(getClass.getResource("/avro/Invalid.avdl").toURI))
        )
      }

      actual.inputFile.getPath should endWith("/avro/Invalid.avdl")

      actual.output shouldEqual Invalid(
        NonEmptyList.one(
          "RPC method response parameter has non-record response type 'STRING'"
        )
      )
    }
  }

  private def test(scenario: Scenario): Boolean = {
    val output =
      AvrohuggerSrcGenerator(
        scenario.marshallersImports,
        compressionType = NoCompressionGen,
        serializationType = scenario.serializationType,
        useIdiomaticEndpoints = true,
        scala3 = false
      ).generateFromFiles(
        scenario.inputResourcesPath.map(path => new File(getClass.getResource(path).toURI))
      )
    output should not be empty
    output forall { case Result(_, contents) =>
      contents.map(_.path.toString) shouldBe Valid(scenario.expectedOutputFilePath)
      contents.map(_.contents.filter(_.nonEmpty).take(scenario.expectedOutput.size)) shouldBe Valid(
        scenario.expectedOutput
      )
      true
    }
  }

}
