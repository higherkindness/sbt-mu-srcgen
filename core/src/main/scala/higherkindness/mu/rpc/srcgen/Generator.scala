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

package higherkindness.mu.rpc.srcgen

import java.io.File
import java.nio.file.Path

object Generator {
  case class Output(path: Path, contents: List[String])
  case class Result(inputFile: File, output: ErrorsOr[Output])
}
trait Generator {

  def idlType: Model.IdlType

  def generateFromFiles(files: Set[File]): List[Generator.Result] =
    inputFiles(files).map(inputFile => Generator.Result(inputFile, generateFromFile(inputFile)))

  protected def inputFiles(files: Set[File]): List[File]

  protected def generateFromFile(inputFile: File): ErrorsOr[Generator.Output]

}
