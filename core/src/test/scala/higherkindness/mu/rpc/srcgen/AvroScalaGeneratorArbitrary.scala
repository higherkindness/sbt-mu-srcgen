/*
 * Copyright 2020-2021 47 Degrees <https://www.47deg.com>
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
      marshallersImports: List[MarshallersImport],
      compressionType: CompressionType,
      streamingImplementation: StreamingImplementation,
      useIdiomaticEndpoints: Boolean = true
  )

  def marshallersImportGen(serializationType: SerializationType): Gen[MarshallersImport] =
    serializationType match {
      case Avro | AvroWithSchema =>
        Gen.oneOf(
          Gen.const(BigDecimalAvroMarshallers),
          Gen.const(JodaDateTimeAvroMarshallers),
          customMarshallersImportsGen
        )
      case Protobuf =>
        Gen.oneOf(
          Gen.const(BigDecimalProtobufMarshallers),
          Gen.const(JavaTimeDateAvroMarshallers),
          Gen.const(JavaTimeDateProtobufMarshallers),
          Gen.const(JodaDateTimeProtobufMarshallers),
          customMarshallersImportsGen
        )
      case _ => customMarshallersImportsGen
    }

  val importSliceGen: Gen[String] =
    Gen.choose(4, 10).flatMap(Gen.listOfN(_, Gen.alphaLowerChar).map(_.mkString("")))

  val customMarshallersImportsGen: Gen[MarshallersImport] =
    Gen
      .choose(1, 5)
      .flatMap(
        Gen.listOfN(_, importSliceGen).map(_.mkString(".") + "._").map(CustomMarshallersImport)
      )

  type GenerateOutput =
    (SerializationType, List[MarshallersImport], CompressionType, Boolean) => List[String]

  def scenarioArbirary(generateOutput: GenerateOutput): Arbitrary[Scenario] = Arbitrary {
    for {
      inputResourcePath       <- Gen.oneOf("/avro/GreeterService.avpr", "/avro/GreeterService.avdl")
      serializationType       <- Gen.const(Avro)
      marshallersImports      <- Gen.listOf(marshallersImportGen(serializationType))
      compressionType         <- Gen.oneOf(CompressionType.Gzip, CompressionType.Identity)
      streamingImplementation <- Gen.oneOf(MonixObservable, Fs2Stream)
      useIdiomaticEndpoints   <- Arbitrary.arbBool.arbitrary
    } yield Scenario(
      inputResourcePath,
      generateOutput(
        serializationType,
        marshallersImports,
        compressionType,
        useIdiomaticEndpoints
      ),
      "foo/bar/MyGreeterService.scala",
      serializationType,
      marshallersImports,
      compressionType,
      streamingImplementation,
      useIdiomaticEndpoints
    )
  }

}

object AvroScalaGeneratorArbitrary extends AvroScalaGeneratorArbitrary
