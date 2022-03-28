package higherkindness.mu.rpc.srcgen.service

/** A fully qualified Scala type, including _root_ prefix */
final case class FullyQualified(tpe: String)

final case class MethodDefn(
    name: String,
    in: FullyQualified,
    out: FullyQualified,
    clientStreaming: Boolean,
    serverStreaming: Boolean
)
