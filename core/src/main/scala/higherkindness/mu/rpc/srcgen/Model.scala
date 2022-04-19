/*
 * Copyright 2020-2022 47 Degrees <https://www.47deg.com>
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

object Model {

  sealed trait IdlType extends Product with Serializable
  object IdlType {
    case object Proto   extends IdlType
    case object Avro    extends IdlType
    case object Unknown extends IdlType
  }

  sealed trait SerializationType extends Product with Serializable
  object SerializationType {
    case object Protobuf       extends SerializationType
    case object Avro           extends SerializationType
    case object AvroWithSchema extends SerializationType

    def fromString(string: String): Either[IllegalArgumentException, SerializationType] =
      string match {
        case "Protobuf"       => Right(Protobuf)
        case "Avro"           => Right(Avro)
        case "AvroWithSchema" => Right(AvroWithSchema)
        case other => Left(new IllegalArgumentException(s"Unknown serialization type: '$other'"))
      }

  }

  sealed abstract class MarshallersImport(val marshallersImport: String)
      extends Product
      with Serializable

  final case class CustomMarshallersImport(mi: String) extends MarshallersImport(mi)

  case object BigDecimalTaggedAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._")

  sealed abstract class CompressionTypeGen(
      val annotationParameterValue: String
  ) extends Product
      with Serializable {
    override def toString(): String = annotationParameterValue
  }
  case object GzipGen          extends CompressionTypeGen("Gzip")
  case object NoCompressionGen extends CompressionTypeGen("Identity")
  object CompressionTypeGen {
    def fromString(
        annotationParameterValue: String
    ): Either[IllegalArgumentException, CompressionTypeGen] = annotationParameterValue match {
      case "Gzip"     => Right(GzipGen)
      case "Identity" => Right(NoCompressionGen)
      case _ =>
        Left(new IllegalArgumentException(s"Unknown compression type: '$annotationParameterValue'"))
    }
  }

}
