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

package higherkindness.mu.rpc.srcgen.compendium

import cats.effect.Sync
import org.http4s._
import org.http4s.circe._
import cats.implicits._
import org.http4s.client.{Client, UnexpectedStatus}

trait CompendiumClient[F[_]] {

  /**
   * Retrieve a Protocol by its id
   *
   * @param identifier the protocol identifier
   * @param version    optional protocol version number
   * @return a protocol
   */
  def retrieveProtocol(identifier: String, version: Option[Int]): F[Option[RawProtocol]]

}

object CompendiumClient {

  def apply[F[_]: Sync](
      clientF: Client[F],
      clientConfig: HttpConfig
  ): CompendiumClient[F] =
    new CompendiumClient[F] {

      override def retrieveProtocol(
          identifier: String,
          version: Option[Int]
      ): F[Option[RawProtocol]] = {
        val versionParam  = version.fold("")(v => s"?version=${v.show}")
        val connectionUrl = s"${clientConfig.serverUrl}/v0/protocol/$identifier$versionParam"

        implicit val rawEntityDecoder = jsonOf[F, RawProtocol]

        clientF.get(connectionUrl)(res =>
          res.status match {
            case Status.Ok       => res.as[RawProtocol].map(Option(_))
            case Status.NotFound => Sync[F].pure(None)
            case s               => Sync[F].raiseError(UnexpectedStatus(s))
          }
        )
      }
    }

}
