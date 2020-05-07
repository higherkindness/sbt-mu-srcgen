package higherkindness.mu.rpc.srcgen.compendium

import cats.effect.Sync
import cats.free.Free
import hammock._
import hammock.circe.implicits._
import cats.implicits._

trait CompendiumClient[F[_]] {

  /** Stores a protocol
   *
   * @param identifier the protocol identifier
   * @param protocol a protocol
   * @return the identifier of the protocol
   */
  def storeProtocol(identifier: String, protocol: RawProtocol): F[Int]

  /** Retrieve a Protocol by its id
   *
   * @param identifier the protocol identifier
   * @param version    optional protocol version number
   * @return a protocol
   */
  def retrieveProtocol(identifier: String, version: Option[Int]): F[Option[RawProtocol]]

}

object CompendiumClient {

  def apply[F[_]]()(
      implicit interp: InterpTrans[F],
      clientConfig: HttpConfig,
      F: Sync[F]
  ): CompendiumClient[F] =
    new CompendiumClient[F] {

      val baseUrl: String          = s"http://${clientConfig.host}:${clientConfig.port}"
      val versionParamName: String = "version"

      override def storeProtocol(identifier: String, protocol: RawProtocol): F[Int] = {
        val request: Free[HttpF, HttpResponse] =
          Hammock.request(Method.POST, uri"$baseUrl/v0/protocol/$identifier", Map(), Some(protocol))

        for {
          status <- request.map(_.status).exec[F]
          _ <- status match {
            case Status.Created => Sync[F].unit
            case Status.OK      => Sync[F].unit
            case Status.BadRequest =>
              asError(request, SchemaError)
            case Status.InternalServerError =>
              Sync[F].raiseError(UnknownError(s"Error in compendium server"))
            case _ =>
              Sync[F].raiseError(UnknownError(s"Unknown error with status code $status"))
          }
        } yield status.code
      }

      override def retrieveProtocol(
          identifier: String,
          version: Option[Int]
      ): F[Option[RawProtocol]] = {
        val versionParam = version.fold("")(v => s"?$versionParamName=${v.show}")
        val uri          = uri"$baseUrl/v0/protocol/$identifier$versionParam"

        val request: Free[HttpF, HttpResponse] = Hammock.request(Method.GET, uri, Map())

        for {
          status <- request.map(_.status).exec[F]
          out <- status match {
            case Status.OK       => request.as[RawProtocol].map(Option(_)).exec[F]
            case Status.NotFound => Sync[F].pure(None)
            case Status.InternalServerError =>
              Sync[F].raiseError(UnknownError(s"Error in compendium server"))
            case _ =>
              Sync[F].raiseError(UnknownError(s"Unknown error with status code $status"))
          }
        } yield out
      }
      private def asError(request: Free[HttpF, HttpResponse], error: String => Exception): F[Unit] =
        request
          .as[ErrorResponse]
          .exec[F]
          .flatMap(rsp => Sync[F].raiseError(error(rsp.message)))

    }

}
