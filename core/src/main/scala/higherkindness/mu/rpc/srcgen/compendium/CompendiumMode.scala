package higherkindness.mu.rpc.srcgen.compendium

import java.io.{File, PrintWriter}
import cats.effect.{Async, Resource, Sync}
import scala.util.Try
import cats.implicits._
import hammock.asynchttpclient.AsyncHttpClientInterpreter

final case class ProtocolAndVersion(name: String, version: Option[String])
final case class FilePrintWriter(file: File, pw: PrintWriter)

case class CompendiumMode[F[_]: Async](
    protocols: List[ProtocolAndVersion],
    fileType: String,
    httpConfig: HttpConfig,
    path: String
) {

  implicit val interpreter  = AsyncHttpClientInterpreter.instance[F]
  implicit val clientConfig = httpConfig

  def run(): F[List[File]] =
    protocols.traverse(protocolAndVersion =>
      for {
        protocol <- CompendiumClient()
          .retrieveProtocol(
            protocolAndVersion.name,
            safeInt(protocolAndVersion.version)
          )
        file <- protocol match {
          case Some(raw) =>
            writeTempFile(
              raw.raw,
              extension = fileType,
              identifier = protocolAndVersion.name,
              path = path
            )
          case None =>
            Async[F].raiseError[File](
              ProtocolNotFound(s"Protocol ${protocolAndVersion.name} not found in Compendium. ")
            )
        }
      } yield file
    )

  private def safeInt(s: Option[String]): Option[Int] = s.flatMap(str => Try(str.toInt).toOption)

  private def writeTempFile(
      msg: String,
      extension: String,
      identifier: String,
      path: String
  ): F[File] =
    Resource
      .make(Sync[F].delay {
        if (!new File(path).exists()) new File(path).mkdirs()
        val file = new File(path + s"/$identifier.$extension")
        file.deleteOnExit()
        FilePrintWriter(file, new PrintWriter(file))
      }) { fpw: FilePrintWriter => Sync[F].delay(fpw.pw.close()) }
      .use((fpw: FilePrintWriter) => Sync[F].delay(fpw.pw.write(msg)).as(fpw))
      .map(_.file)

}
