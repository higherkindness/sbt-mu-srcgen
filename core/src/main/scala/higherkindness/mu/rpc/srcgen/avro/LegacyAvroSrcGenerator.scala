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

package higherkindness.mu.rpc.srcgen.avro

import avrohugger.format.Standard
import avrohugger.types._
import cats.data.NonEmptyList
import cats.implicits._
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen._
import higherkindness.mu.rpc.srcgen.service._

import java.io.File
import org.apache.avro._

import java.nio.file.{Path, Paths}
import scala.collection.JavaConverters._
import scala.util.Right

final case class LegacyAvroSrcGenerator(
    marshallersImports: List[MarshallersImport],
    bigDecimalTypeGen: BigDecimalTypeGen,
    compressionType: CompressionTypeGen = NoCompressionGen,
    useIdiomaticEndpoints: Boolean = true,
    scala3: Boolean
) extends Generator {

  private val avroBigDecimal: AvroScalaDecimalType = bigDecimalTypeGen match {
    case ScalaBigDecimalGen       => ScalaBigDecimal(None)
    case ScalaBigDecimalTaggedGen => ScalaBigDecimalWithPrecision(None)
  }
  private val avroScalaCustomTypes = Standard.defaultTypes.copy(
    enum = if (scala3) ScalaCaseObjectEnum else ScalaEnumeration,
    decimal = avroBigDecimal
  )
  private val mainGenerator =
    avrohugger.Generator(Standard, avroScalaCustomTypes = Some(avroScalaCustomTypes))

  private val adtGenerator = mainGenerator.copy(avroScalaCustomTypes =
    Some(mainGenerator.avroScalaTypes.copy(protocol = ScalaADT))
  ) // ScalaADT: sealed trait hierarchies

  val idlType: IdlType = IdlType.Avro

  def inputFiles(files: Set[File]): List[File] = {
    val avprFiles = files.filter(_.getName.endsWith(AvprExtension))
    val avdlFiles = files.filter(_.getName.endsWith(AvdlExtension))
    // Using custom FileSorter that can process imports outside the initial fileset
    // Note: this will add all imported files to our fileset, even those from other modules
    avprFiles.toList ++ AvdlFileSorter.sortSchemaFiles(avdlFiles)
  }

  // We must process all inputs including imported files from outside our initial fileset,
  // so we then reduce our output to that based on this fileset
  override def generateFrom(
      files: Set[File],
      serializationType: SerializationType
  ): List[Generator.Result] =
    super
      .generateFrom(files, serializationType)
      .filter(output => files.contains(output.inputFile))

  def generateFrom(
      inputFile: File,
      serializationType: SerializationType
  ): ErrorsOr[Generator.Output] =
    generateFromSchemaProtocols(
      mainGenerator.fileParser
        .getSchemaOrProtocols(
          inputFile,
          mainGenerator.format,
          mainGenerator.classStore,
          mainGenerator.classLoader
        ),
      serializationType
    )

  def generateFrom(
      input: String,
      serializationType: SerializationType
  ): ErrorsOr[Generator.Output] =
    generateFromSchemaProtocols(
      mainGenerator.stringParser
        .getSchemaOrProtocols(input, mainGenerator.schemaStore),
      serializationType
    )

  private def generateFromSchemaProtocols(
      schemasOrProtocols: List[Either[Schema, Protocol]],
      serializationType: SerializationType
  ): ErrorsOr[Generator.Output] =
    Some(schemasOrProtocols)
      .filter(_.nonEmpty)
      .flatMap(_.last match {
        case Right(p) => Some(p)
        case _        => None
      })
      .toValidNel(s"No protocol definition found")
      .andThen(generateFrom(_, serializationType))

  def generateFrom(
      protocol: Protocol,
      serializationType: SerializationType
  ): ErrorsOr[Generator.Output] = {

    val schemaGenerator = if (protocol.getMessages.isEmpty) adtGenerator else mainGenerator

    // TODO rewrite most of the code below, once we have scripted tests in place to check the file contents

    val schemaLines = schemaGenerator
      .protocolToStrings(protocol)
      .mkString
      .split('\n')
      .toSeq
      .tail                 // remove top comment and get package declaration on first line
      .filterNot(_ == "()") // https://github.com/julianpeeters/sbt-avrohugger/issues/33

    val packageLines = List(schemaLines.head, "")

    // TODO decide what to do about custom marshallers
    val importLines =
      if (scala3)
        Nil
      else
        marshallersImports
          .map(mi => s"import ${mi.marshallersImport}")
          .sorted

    val messageLines = (schemaLines.tail :+ "").toList

    val requestLines = protocol.getMessages.asScala.toList.flatTraverse { case (name, message) =>
      val comment = Option(message.getDoc).map(doc => s"  /** $doc */").toList
      buildMethodSignature(name, message.getRequest, message.getResponse).map { content =>
        comment ++ List(content, "")
      }
    }

    def buildMethodDefn(methodName: String, message: Protocol#Message): MethodDefn = MethodDefn(
      name = methodName,
      in = fullyQualifiedRequestType(message.getRequest),
      out = fullyQualifiedResponseType(message.getResponse),
      clientStreaming = false,
      serverStreaming = false
    )

    val outputCode = requestLines.map { requests =>
      val (serviceTraitLines, companionObjectLines) = requests match {
        case Nil =>
          (Nil, Nil)
        case _ =>
          val traitLines =
            List(
              s"trait ${protocol.getName}[F[_]] {",
              ""
            ) ++ requests :+ "}"

          val serviceDefn = ServiceDefn(
            name = protocol.getName,
            fullName = s"${protocol.getNamespace}.${protocol.getName}",
            methods = protocol.getMessages.asScala.toList.map { case (methodName, message) => buildMethodDefn(methodName, message) }
          )

          val params = MuServiceParams(
            idiomaticEndpoints = useIdiomaticEndpoints,
            compressionType = compressionType,
            serializationType = serializationType,
            scala3 = scala3
          )

          val companionObjectTree = new CompanionObjectGenerator(serviceDefn, params).generateTree
          val companionObjectLines = companionObjectTree.syntax.split('\n').toList

          (traitLines, companionObjectLines)
      }

      packageLines ++ importLines ++ messageLines ++ serviceTraitLines ++ companionObjectLines
    }

    outputCode.map(Generator.Output(getPath(protocol), _))
  }

  private def getPath(p: Protocol): Path = {
    val pathParts: NonEmptyList[String] =
      NonEmptyList // Non empty list for a later safe `head` call
        .one( // Start with reverse path of file name, the only part we know is for sure a thing
          s"${p.getName}$ScalaFileExtension"
        )
        .concat(
          p.getNamespace // and maybe a path prefix from the namespace for the rest
            .split('.')
            .toList
            .reverse
        )
        .reverse // reverse again to put things in correct order
    Paths.get(pathParts.head, pathParts.tail: _*)
  }

  private def validateRequest(request: Schema): ErrorsOr[String] = {
    val requestArgs = request.getFields.asScala
    requestArgs.toList match {
      case Nil =>
        s"$DefaultRequestParamName: $EmptyType".validNel
      case x :: Nil if x.schema.getType == Schema.Type.RECORD =>
        s"${requestArgs.head.name}: ${requestArgs.head.schema.getFullName}".validNel
      case _ :: Nil =>
        s"RPC method request parameter '${requestArgs.head.name}' has non-record request type '${requestArgs.head.schema.getType}'".invalidNel
      case _ =>
        s"RPC method ${request.getName} has more than 1 request parameter".invalidNel
    }
  }

  private def fullyQualifiedRequestType(request: Schema): FullyQualified = {
    val requestArgs = request.getFields.asScala
    requestArgs.toList match {
      case Nil =>
        FullyQualified(EmptyType)
      case _ =>
        FullyQualified(requestArgs.head.schema.getFullName)
    }
  }

  private def fullyQualifiedResponseType(response: Schema): FullyQualified =
    response.getType match {
      case Schema.Type.NULL =>
        FullyQualified(EmptyType)
      case _ =>
        FullyQualified(response.getFullName)
    }


  private def validateResponse(response: Schema): ErrorsOr[String] = {
    response.getType match {
      case Schema.Type.NULL =>
        EmptyType.validNel
      case Schema.Type.RECORD =>
        s"${response.getNamespace}.${response.getName}".validNel
      case _ =>
        s"RPC method response parameter has non-record response type '${response.getType}'".invalidNel
    }
  }

  def buildMethodSignature(
      name: String,
      request: Schema,
      response: Schema
  ): ErrorsOr[String] = {
    (validateRequest(request), validateResponse(response)).mapN {
      case (requestParam, responseParam) =>
        s"  def $name($requestParam): F[$responseParam]"
    }
  }
}
