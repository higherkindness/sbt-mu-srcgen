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

package higherkindness.mu.rpc.srcgen

object Model {

  sealed trait ExecutionMode extends Product with Serializable
  object ExecutionMode {
    case object Compendium extends ExecutionMode
    case object Local      extends ExecutionMode
  }

  sealed trait IdlType extends Product with Serializable
  object IdlType {
    case object Proto   extends IdlType
    case object Avro    extends IdlType
    case object OpenAPI extends IdlType
    case object Unknown extends IdlType
  }

  sealed trait SerializationType extends Product with Serializable
  object SerializationType {
    case object Protobuf       extends SerializationType
    case object Avro           extends SerializationType
    case object AvroWithSchema extends SerializationType
    case object Custom         extends SerializationType
  }

  sealed abstract class MarshallersImport(val marshallersImport: String)
      extends Product
      with Serializable

  final case class CustomMarshallersImport(mi: String) extends MarshallersImport(mi)

  case object BigDecimalAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigdecimal._")

  case object BigDecimalTaggedAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._")

  case object JavaTimeDateAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.avro.javatime._")

  case object JodaDateTimeAvroMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.marshallers.jodaTimeEncoders.avro._")

  case object BigDecimalProtobufMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.pbd.bigdecimal._")

  case object JavaTimeDateProtobufMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.internal.encoders.pbd.javatime._")

  case object JodaDateTimeProtobufMarshallers
      extends MarshallersImport("higherkindness.mu.rpc.marshallers.jodaTimeEncoders.pbd._")

  sealed trait BigDecimalTypeGen       extends Product with Serializable
  case object ScalaBigDecimalGen       extends BigDecimalTypeGen
  case object ScalaBigDecimalTaggedGen extends BigDecimalTypeGen

  sealed abstract class CompressionTypeGen extends Product with Serializable
  case object GzipGen                      extends CompressionTypeGen
  case object NoCompressionGen             extends CompressionTypeGen

  sealed trait StreamingImplementation
  case object Fs2Stream       extends StreamingImplementation
  case object MonixObservable extends StreamingImplementation

  sealed abstract class AvroGeneratorTypeGen extends Product with Serializable
  case object AvrohuggerGen                  extends AvroGeneratorTypeGen
  case object SkeumorphGen                   extends AvroGeneratorTypeGen

}
