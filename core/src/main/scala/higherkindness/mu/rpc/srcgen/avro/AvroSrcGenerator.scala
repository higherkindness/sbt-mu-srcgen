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

package higherkindness.mu.rpc.srcgen.avro

import java.io.File

import scala.collection.JavaConverters._
import scala.util.Right

import avrohugger.Generator
import avrohugger.format.Standard
import avrohugger.types._
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen._
import org.apache.avro._
import org.log4s._

final case class AvroSrcGenerator(
    marshallersImports: List[MarshallersImport],
    bigDecimalTypeGen: BigDecimalTypeGen,
    compressionTypeGen: CompressionTypeGen,
    useIdiomaticEndpoints: UseIdiomaticEndpoints
) extends SrcGenerator {

  private[this] val logger = getLogger

  private val avroBigDecimal: AvroScalaDecimalType = bigDecimalTypeGen match {
    case ScalaBigDecimalGen       => ScalaBigDecimal(None)
    case ScalaBigDecimalTaggedGen => ScalaBigDecimalWithPrecision(None)
  }
  private val avroScalaCustomTypes = Standard.defaultTypes.copy(decimal = avroBigDecimal)
  private val mainGenerator        = Generator(Standard, avroScalaCustomTypes = Some(avroScalaCustomTypes))

  private val adtGenerator = mainGenerator.copy(avroScalaCustomTypes =
    Some(mainGenerator.avroScalaTypes.copy(protocol = ScalaADT))
  ) // ScalaADT: sealed trait hierarchies

  val idlType: IdlType = IdlType.Avro

  def inputFiles(files: Set[File]): Seq[File] = {
    val avprFiles = files.filter(_.getName.endsWith(AvprExtension))
    val avdlFiles = files.filter(_.getName.endsWith(AvdlExtension))
    // Using custom FileSorter that can process imports outside the initial fileset
    // Note: this will add all imported files to our fileset, even those from other modules
    avprFiles.toSeq ++ AvdlFileSorter.sortSchemaFiles(avdlFiles)
  }

  // We must process all inputs including imported files from outside our initial fileset,
  // so we then reduce our output to that based on this fileset
  override def generateFrom(
      files: Set[File],
      serializationType: SerializationType
  ): Seq[(File, String, Seq[String])] =
    super
      .generateFrom(files, serializationType)
      .filter(output => files.contains(output._1))

  def generateFrom(
      inputFile: File,
      serializationType: SerializationType
  ): Option[(String, Seq[String])] =
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
  ): Option[(String, Seq[String])] =
    generateFromSchemaProtocols(
      mainGenerator.stringParser
        .getSchemaOrProtocols(input, mainGenerator.schemaStore),
      serializationType
    )

  private def generateFromSchemaProtocols(
      schemasOrProtocols: List[Either[Schema, Protocol]],
      serializationType: SerializationType
  ): Option[(String, Seq[String])] =
    Some(schemasOrProtocols)
      .filter(_.nonEmpty)
      .flatMap(_.last match {
        case Right(p) => Some(p)
        case _        => None
      })
      .map(generateFrom(_, serializationType))

  def generateFrom(
      protocol: Protocol,
      serializationType: SerializationType
  ): (String, Seq[String]) = {

    val outputPath =
      s"${protocol.getNamespace.replace('.', '/')}/${protocol.getName}$ScalaFileExtension"

    val schemaGenerator = if (protocol.getMessages.isEmpty) adtGenerator else mainGenerator
    val schemaLines = schemaGenerator
      .protocolToStrings(protocol)
      .mkString
      .split('\n')
      .toSeq
      .tail                 // remove top comment and get package declaration on first line
      .filterNot(_ == "()") // https://github.com/julianpeeters/sbt-avrohugger/issues/33

    val packageLines = Seq(schemaLines.head, "")

    val importLines =
      ("import higherkindness.mu.rpc.protocol._" :: marshallersImports
        .map(_.marshallersImport)
        .map("import " + _)).sorted

    val messageLines = schemaLines.tail :+ ""

    val requestLines = protocol.getMessages.asScala.toSeq.flatMap {
      case (name, message) =>
        val comment = Seq(Option(message.getDoc).map(doc => s"  /** $doc */")).flatten
        try comment ++ Seq(parseMessage(name, message.getRequest, message.getResponse), "")
        catch {
          case ParseException(msg) =>
            logger.warn(s"$msg, cannot be converted to mu: $message")
            Seq.empty
        }
    }

    val extraParams: List[String] =
      s"compressionType = ${compressionTypeGen.value}" +:
        (if (useIdiomaticEndpoints) {
           List(
             s"""namespace = Some("${protocol.getNamespace}")""",
             "methodNameStyle = Capitalize"
           )
         } else Nil)

    val serviceParams: String = (serializationType.toString +: extraParams).mkString(",")

    val serviceLines =
      if (requestLines.isEmpty) Seq.empty
      else
        Seq(
          s"@service($serviceParams) trait ${protocol.getName}[F[_]] {",
          ""
        ) ++ requestLines :+ "}"

    outputPath -> (packageLines ++ importLines ++ messageLines ++ serviceLines)
  }

  private def parseMessage(name: String, request: Schema, response: Schema): String = {
    val args = request.getFields.asScala
    if (args.size > 1)
      throw ParseException("RPC method has more than 1 request parameter")
    val requestParam = {
      if (args.isEmpty)
        s"$DefaultRequestParamName: $EmptyType"
      else {
        val arg = args.head
        if (arg.schema.getType != Schema.Type.RECORD)
          throw ParseException("RPC method request parameter is not a record type")
        s"${arg.name}: ${arg.schema.getFullName}"
      }
    }
    val responseParam = {
      if (response.getType == Schema.Type.NULL) EmptyType
      else s"${response.getNamespace}.${response.getName}"
    }
    s"  def $name($requestParam): F[$responseParam]"
  }

  private case class ParseException(msg: String) extends Exception

}
