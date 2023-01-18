/*
 * Copyright 2020-2023 47 Degrees <https://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen.proto

import cats.syntax.either._
import com.google.protobuf.ExtensionRegistry
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler.{DescriptorImplicits, GeneratorParams}
import scalapb.options.Scalapb
import scala.collection.JavaConverters._
import scala.util.Try
import higherkindness.mu.rpc.srcgen.Model.{CompressionTypeGen, SerializationType}
import higherkindness.mu.rpc.srcgen.service.MuServiceParams

/**
 * A source generator to generate Mu service traits from protobuf definitions.
 */
object MuServiceGenerator extends CodeGenApp {

  override def registerExtensions(registry: ExtensionRegistry): Unit =
    Scalapb.registerAllExtensions(registry)

  /**
   * The entrypoint to the source generator.
   *
   * The arguments are passed as strings, so we first parse them and then use a [[MuServicePrinter]]
   * to generate the source code.
   */
  def process(request: CodeGenRequest): CodeGenResponse =
    parseParams(request) match {
      case Right(muServiceParams) =>
        val implicits = DescriptorImplicits.fromCodeGenRequest(GeneratorParams(), request)

        val results =
          for {
            file    <- request.filesToGenerate
            service <- file.getServices.asScala
            printer = new MuServicePrinter(service, muServiceParams, implicits)
          } yield printer.result

        CodeGenResponse.succeed(results)
      case Left(error) =>
        CodeGenResponse.fail(error)
    }

  private def parseParams(
      request: CodeGenRequest
  ): Either[String, MuServiceParams] =
    for {
      scalapbParseResult <- GeneratorParams.fromStringCollectUnrecognized(request.parameter)
      (_, rawMuParams) = scalapbParseResult
      muParams <- parseMuParams(rawMuParams).leftMap(_.getMessage)
    } yield muParams

  private def parseMuParams(params: Seq[String]): Either[Throwable, MuServiceParams] =
    for {
      _ <- Either.cond(
        params.size == 4,
        (),
        new IllegalArgumentException(s"Expected exactly 4 arguments, got: $params")
      )
      idiomaticEndpoints <- Try(params(0).toBoolean).toEither
      compressionType    <- CompressionTypeGen.fromString(params(1))
      serializationType  <- SerializationType.fromString(params(2))
      scala3             <- Try(params(3).toBoolean).toEither
    } yield MuServiceParams(idiomaticEndpoints, compressionType, serializationType, scala3)

}
