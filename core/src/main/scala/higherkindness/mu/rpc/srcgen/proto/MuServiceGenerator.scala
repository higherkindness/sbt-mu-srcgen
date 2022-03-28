package higherkindness.mu.rpc.srcgen.proto

import cats.syntax.either._
import com.google.protobuf.ExtensionRegistry
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler.{DescriptorImplicits, GeneratorParams}
import scalapb.options.Scalapb
import scala.collection.JavaConverters._
import scala.util.Try
import higherkindness.mu.rpc.srcgen.Model.CompressionTypeGen
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
        params.size == 3,
        (),
        new IllegalArgumentException(s"Expected exactly 3 arguments, got: $params")
      )
      idiomaticEndpoints <- Try(params(0).toBoolean).toEither
      compressionType    <- CompressionTypeGen.fromString(params(1))
      scala3             <- Try(params(2).toBoolean).toEither
    } yield MuServiceParams(idiomaticEndpoints, compressionType, scala3)

}
