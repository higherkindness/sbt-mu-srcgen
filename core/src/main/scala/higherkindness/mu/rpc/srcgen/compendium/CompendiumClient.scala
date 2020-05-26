package higherkindness.mu.rpc.srcgen.compendium

import cats.effect.Sync
import org.http4s._
import org.http4s.circe._
import cats.implicits._
import org.http4s.client.Client

trait CompendiumClient[F[_]] {

  /** Retrieve a Protocol by its id
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

      val baseUrl: String          = s"http://${clientConfig.host}:${clientConfig.port}"
      val versionParamName: String = "version"

      override def retrieveProtocol(
          identifier: String,
          version: Option[Int]
      ): F[Option[RawProtocol]] = {
        val versionParam = version.fold("")(v => s"?$versionParamName=${v.show}")
        val uri          = s"$baseUrl/v0/protocol/$identifier$versionParam"

        implicit val rawEntityDecoder = jsonOf[F, RawProtocol]

        clientF.get(uri)(res =>
          res.status match {
            case Status.Ok       => res.as[RawProtocol].map(Option(_))
            case Status.NotFound => Sync[F].pure(None)
            case Status.InternalServerError =>
              Sync[F].raiseError(UnknownError(s"Error in compendium server"))
            case _ =>
              Sync[F].raiseError(UnknownError(s"Unknown error with status code ${res.status.code}"))

          }
        )
      }
    }

}
