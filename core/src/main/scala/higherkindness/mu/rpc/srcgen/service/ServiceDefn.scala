package higherkindness.mu.rpc.srcgen.service

final case class ServiceDefn(
    name: String,
    fullName: String,
    methods: List[MethodDefn]
)
