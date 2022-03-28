package higherkindness.mu.rpc.srcgen.service

import higherkindness.mu.rpc.srcgen.Model.CompressionTypeGen

final case class MuServiceParams(
    idiomaticEndpoints: Boolean,
    compressionType: CompressionTypeGen,
    scala3: Boolean
)
