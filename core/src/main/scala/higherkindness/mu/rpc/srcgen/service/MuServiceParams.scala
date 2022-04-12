package higherkindness.mu.rpc.srcgen.service

import higherkindness.mu.rpc.srcgen.Model.{CompressionTypeGen, SerializationType}

final case class MuServiceParams(
    idiomaticEndpoints: Boolean,
    compressionType: CompressionTypeGen,
    serializationType: SerializationType,
    scala3: Boolean
)
