package higherkindness.mu.rpc.srcgen.proto

import higherkindness.mu.rpc.srcgen.Model.CompressionTypeGen

final case class MuServiceParams(
    idiomaticEndpoints: Boolean,
    compressionType: CompressionTypeGen
)
