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

import FileUtil._
import cats.data.{NonEmptyList, ValidatedNel}
import higherkindness.mu.rpc.srcgen.Model.{IdlType, SerializationType}
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.implicits._

class GeneratorApplication[T <: Generator](generators: T*) {
  // Code covered by plugin tests
  // $COVERAGE-OFF$

  private val generatorsByType = generators.map(gen => gen.idlType -> gen).toMap

  def generateFrom(
      idlType: IdlType,
      serializationType: SerializationType,
      inputFiles: Set[File],
      outputDir: File
  ): Seq[File] = generatorsByType.get(idlType) match {
    case Some(generator) =>
      val result: ValidatedNel[(File, NonEmptyList[Error]), List[(File, List[String])]] =
        generator
          .generateFrom(inputFiles, serializationType)
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
          outputFiles.map { case (outputFile, content) =>
            Option(outputFile.getParentFile).foreach(_.mkdirs())
            outputFile.write(content)
            outputFile
          }
      }
    case None =>
      System.out.println(
        s"Unsupported IDL type '$idlType', skipping code generation in this module. " +
          s"Valid values: ${generatorsByType.keys.mkString(", ")}"
      )
      Seq.empty[File]
  }

  // $COVERAGE-ON$
}
