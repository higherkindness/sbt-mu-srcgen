package higherkindness.mu.rpc.srcgen.compendium

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ErrorResponse(message: String)

object ErrorResponse {
  implicit val decoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  implicit val encoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
}
