package higherkindness.mu.rpc.srcgen.service

/** A fully qualified Scala type, including _root_ prefix */
final case class FullyQualified(tpe: String)

sealed abstract class RequestParam(val tpe: FullyQualified)

object RequestParam {

  /** A request parameter with a name, a la Avro */
  case class Named(name: String, override val tpe: FullyQualified) extends RequestParam(tpe)

  /** A request parameter with no name specified, a la Protobuf */
  case class Anon(override val tpe: FullyQualified) extends RequestParam(tpe)
}

final case class MethodDefn(
    name: String,
    in: RequestParam,
    out: FullyQualified,
    clientStreaming: Boolean,
    serverStreaming: Boolean,
    comment: Option[String] = None
)

final case class ServiceDefn(
    name: String,
    fullName: String,
    methods: List[MethodDefn]
)
