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
