/*
 * Copyright 2020-2021 47 Degrees <https://www.47deg.com>
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
              ProtocolNotFound(s"Protocol ${protocolAndVersion.name} not found in Compendium. ")
                .raiseError[F, File]
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
