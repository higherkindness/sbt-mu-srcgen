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

import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.avro.AvrohuggerSrcGenerator
import cats.data.{NonEmptyList, ValidatedNel}
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.implicits._
import scalafix.interfaces.Scalafix

import java.io.File
import java.nio.file.Files
import scala.jdk.CollectionConverters._

class GeneratorApplication(scala3: Boolean, generators: Generator*) {
  // Code covered by plugin tests
  // $COVERAGE-OFF$

  private val generatorsByType = generators.map(gen => gen.idlType -> gen).toMap

  def generateSources(
      idlType: IdlType,
      inputFiles: Set[File],
      outputDir: File
  ): Seq[File] = generatorsByType.get(idlType) match {
    case Some(generator) =>
      val result: ValidatedNel[(File, NonEmptyList[Error]), List[(File, List[String])]] =
        generator
          .generateFromFiles(inputFiles)
          .traverse {
            case Generator.Result(inputFile, Invalid(readErrors)) =>
              (inputFile, readErrors).invalidNel
            case Generator.Result(_, Valid(Generator.Output(path, content))) =>
              val outputFile = new File(outputDir, path.toString)
              (outputFile, content).validNel
          }
      result match {
        case Invalid(listOfFilesAndReadErrors) =>
          val formattedErrorMessage = listOfFilesAndReadErrors
            .map { case (inputFile, errors) =>
              s"$inputFile has the following errors: ${errors.toList.mkString(", ")}"
            }
            .toList
            .mkString("\n")
          throw new RuntimeException(
            s"One or more IDL files are invalid. Error details:\n $formattedErrorMessage"
          )
        case Valid(outputFiles) =>
          val files = outputFiles.map { case (outputFile, content) =>
            Option(outputFile.getParentFile).foreach(_.mkdirs())
            Files.write(outputFile.toPath, content.asJava)
            outputFile
          }

          if (scala3) {
            applyRewrites(files)
          }

          files
      }
    case None =>
      System.out.println(
        s"Unsupported IDL type '$idlType', skipping code generation in this module. " +
          s"Valid values: ${generatorsByType.keys.mkString(", ")}"
      )
      Seq.empty[File]
  }

  private val rules = List(
    "class:higherkindness.mu.rpc.srcgen.avro.rewrites.RemoveShapelessImports",
    "class:higherkindness.mu.rpc.srcgen.avro.rewrites.ReplaceShapelessCoproduct",
    "class:higherkindness.mu.rpc.srcgen.avro.rewrites.ReplaceShapelessTaggedDecimal",
    "class:higherkindness.mu.rpc.srcgen.avro.rewrites.AddAvroOrderingAnnotations"
  )

  private def applyRewrites(files: Seq[File]): Unit = {
    val scalafix = Scalafix.classloadInstance(getClass.getClassLoader)
    val errors = scalafix.newArguments
      .withWorkingDirectory(files.head.toPath.getParent)
      .withPaths(files.map(_.toPath).asJava)
      .withRules(rules.asJava)
      .withScalaVersion("3")
      .run()

    errors.foreach(e => println(s"Scalafix error: $e"))
  }

  // $COVERAGE-ON$
}

object GeneratorApplication {

  def apply(
      marshallersImports: List[MarshallersImport],
      compressionTypeGen: CompressionTypeGen,
      serializationType: SerializationType,
      useIdiomaticEndpoints: Boolean,
      scala3: Boolean
  ): GeneratorApplication =
    new GeneratorApplication(
      scala3,
      AvrohuggerSrcGenerator(
        marshallersImports,
        compressionTypeGen,
        serializationType,
        useIdiomaticEndpoints,
        scala3
      )
    )

}
