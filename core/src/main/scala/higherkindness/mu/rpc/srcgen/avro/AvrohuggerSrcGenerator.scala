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

final case class AvrohuggerSrcGenerator(
    marshallersImports: List[MarshallersImport],
    compressionType: CompressionTypeGen = NoCompressionGen,
    serializationType: SerializationType,
    useIdiomaticEndpoints: Boolean = true,
    scala3: Boolean
) extends Generator {

  private val AvprExtension = ".avpr"
  private val AvdlExtension = ".avdl"

  private val avroScalaCustomTypes = Standard.defaultTypes.copy(
    enum = if (scala3) ScalaCaseObjectEnum else ScalaEnumeration,
    decimal = ScalaBigDecimalWithPrecision(None)
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
  override def generateFromFiles(files: Set[File]): List[Generator.Result] =
    super
      .generateFromFiles(files)
      .filter(output => files.contains(output.inputFile))

  def generateFromFile(inputFile: File): ErrorsOr[Generator.Output] =
    generateFromSchemaProtocols(
      mainGenerator.fileParser
        .getSchemaOrProtocols(
          inputFile,
          mainGenerator.format,
          mainGenerator.classStore,
          mainGenerator.classLoader
        )
    )

  private def generateFromSchemaProtocols(
      schemasOrProtocols: List[Either[Schema, Protocol]]
  ): ErrorsOr[Generator.Output] =
    Some(schemasOrProtocols)
      .filter(_.nonEmpty)
      .flatMap(_.last.toOption)
      .toValidNel(s"No protocol definition found")
      .andThen(generateFromProtocol(_))

  private case class PackageLineAndMessageLines(packageLine: String, messageLines: List[String])

  def generateFromProtocol(protocol: Protocol): ErrorsOr[Generator.Output] = {
    val fileContent =
      if (protocol.getMessages.isEmpty) {
        val PackageLineAndMessageLines(packageLine, messageLines) =
          generateMessageClasses(protocol, adtGenerator)
        (packageLine :: "" :: messageLines).validNel
      } else {
        generateMessageClassesAndService(protocol, mainGenerator)
      }
    fileContent.map(Generator.Output(getPath(protocol), _))
  }

  private def generateMessageClasses(
      protocol: Protocol,
      schemaGenerator: avrohugger.Generator
  ): PackageLineAndMessageLines = {
    val (packageLine :: messageLines) = schemaGenerator
      .protocolToStrings(protocol)
      .mkString
      .split('\n')
      .toList
      .tail                 // remove top comment and get package declaration on first line
      .filterNot(_ == "()") // https://github.com/julianpeeters/sbt-avrohugger/issues/33

    PackageLineAndMessageLines(packageLine, messageLines)
  }

  private def generateMessageClassesAndService(
      protocol: Protocol,
      schemaGenerator: avrohugger.Generator
  ): ErrorsOr[List[String]] = {
    val imports: String =
      marshallersImports
        .map(mi => s"import ${mi.marshallersImport}")
        .sorted
        .mkString("\n")

    val PackageLineAndMessageLines(pkg, messageLines) =
      generateMessageClasses(protocol, schemaGenerator)
    val messages = messageLines.mkString("\n")

    val methodDefns: ErrorsOr[List[MethodDefn]] = protocol.getMessages.asScala.toList.traverse {
      case (name, message) =>
        buildMethodDefn(name, message)
    }

    methodDefns.map { methods =>
      val serviceMethods: String = methods.map(methodSignature).flatten.indent.mkString("\n")

      val serviceTrait: String =
        s"""|trait ${protocol.getName}[F[_]] {
            |$serviceMethods
            |}""".stripMargin

      val serviceDefn = ServiceDefn(
        name = protocol.getName,
        fullName = s"${protocol.getNamespace}.${protocol.getName}",
        methods = methods
      )
      val params = MuServiceParams(
        idiomaticEndpoints = useIdiomaticEndpoints,
        compressionType = compressionType,
        serializationType = serializationType,
        scala3 = scala3
      )
      val companionObject: String =
        new CompanionObjectGenerator(serviceDefn, params).generateTree.syntax

      s"""|$pkg
          |
          |$imports
          |
          |$messages
          |
          |$serviceTrait
          |
          |$companionObject
          |""".stripMargin
        .split("\n")
        .toList
    }

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

  private def validateRequest(request: Schema): ErrorsOr[RequestParam] = {
    val requestArgs = request.getFields.asScala
    requestArgs.toList match {
      case Nil =>
        RequestParam.Anon(FullyQualified(EmptyType)).validNel
      case x :: Nil if x.schema.getType == Schema.Type.RECORD =>
        RequestParam.Named(x.name, FullyQualified(x.schema.getFullName)).validNel
      case _ :: Nil =>
        s"RPC method request parameter '${requestArgs.head.name}' has non-record request type '${requestArgs.head.schema.getType}'".invalidNel
      case _ =>
        s"RPC method ${request.getName} has more than 1 request parameter".invalidNel
    }
  }

  private def validateResponse(response: Schema): ErrorsOr[FullyQualified] = {
    response.getType match {
      case Schema.Type.NULL =>
        FullyQualified(EmptyType).validNel
      case Schema.Type.RECORD =>
        FullyQualified(response.getFullName).validNel
      case _ =>
        s"RPC method response parameter has non-record response type '${response.getType}'".invalidNel
    }
  }

  def methodSignature(method: MethodDefn): List[String] = {
    val comment = method.comment.map(doc => s"/** $doc */")
    val requestParam = method.in match {
      case RequestParam.Anon(tpe)        => s"$DefaultRequestParamName: ${tpe.tpe}"
      case RequestParam.Named(name, tpe) => s"$name: ${tpe.tpe}"
    }
    val signature = s"def ${method.name}($requestParam): F[${method.out.tpe}]"
    List(comment, Some(signature)).flatten
  }

  def buildMethodDefn(methodName: String, message: Protocol#Message): ErrorsOr[MethodDefn] =
    (validateRequest(message.getRequest), validateResponse(message.getResponse)).mapN {
      case (requestParam, responseType) =>
        MethodDefn(
          name = methodName,
          in = requestParam,
          out = responseType,
          clientStreaming = false,
          serverStreaming = false,
          comment = Option(message.getDoc)
        )
    }

  private implicit class StringListOps(val xs: List[String]) {
    def indent: List[String] = xs.map(x => s"  $x")
  }

}
