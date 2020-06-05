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

package higherkindness.mu.rpc.srcgen

import java.io._

object FileUtil {

  implicit class FileOps(val file: File) extends AnyVal {

    def write(lines: Seq[String]): Unit = {
      val writer = new PrintWriter(file)
      try lines.foreach(writer.println)
      finally writer.close()
    }

    def write(line: String): Unit = write(Seq(line))

  }

}
