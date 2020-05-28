package higherkindness.mu.rpc.srcgen.compendium

import java.io.{File, PrintWriter}

import cats.effect.{ConcurrentEffect, Resource}

import scala.util.Try
import cats.implicits._
import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext.global

final case class ProtocolAndVersion(name: String, version: Option[String])
final case class FilePrintWriter(file: File, pw: PrintWriter)

final case class CompendiumMode[F[_]: ConcurrentEffect](
    protocols: List[ProtocolAndVersion],
    fileType: String,
    httpConfig: HttpConfig,
    path: String
) {

  val httpClient = BlazeClientBuilder[F](global).resource

  def run(): F[List[File]] =
    protocols.traverse(protocolAndVersion =>
      httpClient.use(client => {
        for {
          protocol <- CompendiumClient(client, httpConfig)
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
              ConcurrentEffect[F].raiseError[File](
                ProtocolNotFound(s"Protocol ${protocolAndVersion.name} not found in Compendium. ")
              )
          }
        } yield file
      })
    )

  private def safeInt(s: Option[String]): Option[Int] = s.flatMap(str => Try(str.toInt).toOption)

  private def writeTempFile(
      msg: String,
      extension: String,
      identifier: String,
      path: String
  ): F[File] =
    Resource
      .make(ConcurrentEffect[F].delay {
        if (!new File(path).exists()) new File(path).mkdirs()
        val file = new File(path + s"/$identifier.$extension")
        file.deleteOnExit()
        FilePrintWriter(file, new PrintWriter(file))
      }) { fpw: FilePrintWriter => ConcurrentEffect[F].delay(fpw.pw.close()) }
      .use((fpw: FilePrintWriter) => ConcurrentEffect[F].delay(fpw.pw.write(msg)).as(fpw))
      .map(_.file)

}
