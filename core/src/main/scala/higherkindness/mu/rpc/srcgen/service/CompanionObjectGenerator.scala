package higherkindness.mu.rpc.srcgen.service

import scala.meta._
import higherkindness.mu.rpc.srcgen.Model.GzipGen
import higherkindness.mu.rpc.srcgen.Model.NoCompressionGen
import higherkindness.mu.rpc.srcgen.Model.SerializationType.Protobuf
import higherkindness.mu.rpc.srcgen.Model.SerializationType.Avro
import higherkindness.mu.rpc.srcgen.Model.SerializationType.AvroWithSchema

class CompanionObjectGenerator(
    service: ServiceDefn,
    params: MuServiceParams
) {

  private val fullServiceName: String =
    if (params.idiomaticEndpoints)
      service.fullName
    else
      service.name

  private val serviceTypeName = Type.Name(service.name)

  private def implicitOrUsing(param: Term.Param): Term.Param =
    if (params.scala3)
      param.copy(mods = List(Mod.Using()))
    else
      param.copy(mods = List(Mod.Implicit()))

  private val implicitOrUsingCE: Term.Param =
    implicitOrUsing(param"CE: _root_.cats.effect.Async[F]")

  private val kleisliTypeLambda: Type =
    if (params.scala3) {
      import scala.meta.dialects.Scala3
      t"[A] =>> _root_.cats.data.Kleisli[F, Context, A]"
    } else {
      t"({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T"
    }

  private def kleisli(a: Type): Type =
    t"_root_.cats.data.Kleisli[F, Context, $a]"

  def generateTree: Defn.Object =
    q"""
    object ${Term.Name(service.name)} {
      $marshallerImport

      ..${service.methods.map(methodDescriptorValDef)}

      $bindService

      $bindContextService

      $clientClass

      $clientMethod
      $clientFromChannel
      $unsafeClient
      $unsafeClientFromChannel

      $contextClientClass
      $contextClientMethod
      $contextClientFromChannel
      $unsafeContextClient
      $unsafeContextClientFromChannel

    }
    """

  private def importGiven(pkg: Term.Select): Import =
    if (params.scala3) {
      Import(List(Importer(pkg, List(Importee.GivenAll()))))
    } else {
      Import(List(Importer(pkg, List(Importee.Wildcard()))))
    }

  def marshallerImport: Import = params.serializationType match {
    case Protobuf =>
      importGiven(q"_root_.higherkindness.mu.rpc.internal.encoders.spb")
    case Avro =>
      importGiven(q"_root_.higherkindness.mu.rpc.internal.encoders.avro")
    case AvroWithSchema =>
      importGiven(q"_root_.higherkindness.mu.rpc.internal.encoders.avrowithschema")
  }

  private def summonOrImplicitlyMarshaller(tpe: Type): Term.ApplyType =
    if (params.scala3) {
      q"summon[_root_.io.grpc.MethodDescriptor.Marshaller[$tpe]]"
    } else {
      q"implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[$tpe]]"
    }

  def methodDescriptorValName(md: MethodDefn): Term.Name =
    Term.Name(s"${md.name}MethodDescriptor")

  def inputType(md: MethodDefn): Type =
    md.in.tpe.tpe.parse[Type].get

  def outputType(md: MethodDefn): Type =
    md.out.tpe.parse[Type].get

  def methodDescriptorValDef(md: MethodDefn): Defn.Val = {
    val in: Type  = inputType(md)
    val out: Type = outputType(md)

    val methodType = (md.clientStreaming, md.serverStreaming) match {
      case (false, false) => "UNARY"
      case (true, false)  => "CLIENT_STREAMING"
      case (false, true)  => "SERVER_STREAMING"
      case (true, true)   => "BIDI_STREAMING"
    }

    val valName = Pat.Var(methodDescriptorValName(md))
    q"""
    val $valName: _root_.io.grpc.MethodDescriptor[$in, $out] =
      _root_.io.grpc.MethodDescriptor.newBuilder(
        ${summonOrImplicitlyMarshaller(in)},
        ${summonOrImplicitlyMarshaller(out)}
      )
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.${Term.Name(methodType)})
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName($fullServiceName, ${md.name}))
      .build()
    """
  }

  def bindService: Defn.Def = {
    def methodCall(md: MethodDefn): Term.Tuple = {
      val in: Type  = inputType(md)
      val out: Type = outputType(md)
      val compression: Term.Select = params.compressionType match {
        case GzipGen          => q"_root_.higherkindness.mu.rpc.protocol.Gzip"
        case NoCompressionGen => q"_root_.higherkindness.mu.rpc.protocol.Identity"
      }
      val algebraMethod = Term.Name(md.name)

      // a curse on @Daenyth for making the argument order inconsistent between
      // server.handlers and server.fs2.handlers :D
      val serverCallHandler = (md.clientStreaming, md.serverStreaming) match {
        case (false, false) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.handlers.unary[F, $in, $out](
            algebra.$algebraMethod,
            $compression,
            disp
          )
          """
        case (true, false) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[F, $in, $out](
            ((req: _root_.fs2.Stream[F, $in], _) => algebra.$algebraMethod(req)),
            disp,
            $compression
          )
          """
        case (false, true) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[F, $in, $out](
            ((req: $in, _) => algebra.$algebraMethod(req)),
            disp,
            $compression
          )
          """
        case (true, true) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[F, $in, $out](
            ((req: _root_.fs2.Stream[F, $in], _) => algebra.$algebraMethod(req)),
            disp,
            $compression
          )
          """
      }

      q"(${methodDescriptorValName(md)}, $serverCallHandler)"
    }

    q"""
    def bindService[F[_]]($implicitOrUsingCE, ${implicitOrUsing(
        param"algebra: $serviceTypeName[F]"
      )}): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
      _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
          ${service.fullName},
          ..${service.methods.map(methodCall)}
        )
      }
    """
  }

  def bindContextService: Defn.Def = {
    def methodCall(md: MethodDefn): Term.Tuple = {
      val in: Type  = inputType(md)
      val out: Type = outputType(md)
      val compression: Term.Select = params.compressionType match {
        case GzipGen          => q"_root_.higherkindness.mu.rpc.protocol.Gzip"
        case NoCompressionGen => q"_root_.higherkindness.mu.rpc.protocol.Identity"
      }
      val algebraMethod = Term.Name(md.name)

      val serverCallHandler = (md.clientStreaming, md.serverStreaming) match {
        case (false, false) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[F, Context, $in, $out](
            algebra.$algebraMethod,
            ${methodDescriptorValName(md)},
            $compression,
            disp
          )
          """
        case (true, false) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[F, Context, $in, $out](
            algebra.$algebraMethod,
            ${methodDescriptorValName(md)},
            disp,
            $compression
          )
          """
        case (false, true) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[F, Context, $in, $out](
            algebra.$algebraMethod,
            ${methodDescriptorValName(md)},
            disp,
            $compression
          )
          """
        case (true, true) =>
          q"""
          _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextBidiStreaming[F, Context, $in, $out](
            algebra.$algebraMethod,
            ${methodDescriptorValName(md)},
            disp,
            $compression
          )
          """
      }

      q"(${methodDescriptorValName(md)}, $serverCallHandler)"
    }

    q"""
    def bindContextService[F[_], Context](
        $implicitOrUsingCE,
        ${implicitOrUsing(
        param"serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context]"
      )},
        ${implicitOrUsing(param"algebra: $serviceTypeName[$kleisliTypeLambda]")}
    ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
      _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
          ${service.fullName},
          ..${service.methods.map(methodCall)}
        )
      }
    """
  }

  def clientClass: Defn.Class = {
    def method(md: MethodDefn): Defn.Def = {
      val in  = inputType(md)
      val out = outputType(md)

      (md.clientStreaming, md.serverStreaming) match {
        case (false, false) =>
          q"""
          def ${Term.Name(md.name)}(input: $in): F[$out] =
            _root_.higherkindness.mu.rpc.internal.client.calls.unary[F, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
        case (true, false) =>
          q"""
          def ${Term.Name(md.name)}(input: _root_.fs2.Stream[F, $in]): F[$out] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.clientStreaming[F, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
        case (false, true) =>
          q"""
          def ${Term.Name(md.name)}(input: $in): F[_root_.fs2.Stream[F, $out]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.serverStreaming[F, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
        case (true, true) =>
          q"""
          def ${Term.Name(
              md.name
            )}(input: _root_.fs2.Stream[F, $in]): F[_root_.fs2.Stream[F, $out]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.bidiStreaming[F, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
      }
    }

    q"""
    class Client[F[_]](
      channel: _root_.io.grpc.Channel,
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE
    ) extends _root_.io.grpc.stub.AbstractStub[Client[F]](channel, options) with (${serviceTypeName}[F]) {

      override def build(
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions
      ): Client[F] =
        new Client[F](channel, options)

      ..${service.methods.map(method)}

    }
    """
  }

  def clientMethod: Defn.Def =
    q"""
    def client[F[_]](
      channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
      channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE
    ): _root_.cats.effect.Resource[F, $serviceTypeName[F]] =
      _root_.cats.effect.Resource.make(
        new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
      )(
        (channel) => CE.void(CE.delay(channel.shutdown()))
      ).evalMap((ch) =>
        CE.delay(new Client[F](ch, options))
      )
    """

  def clientFromChannel: Defn.Def =
    q"""
    def clientFromChannel[F[_]](
      channel: F[_root_.io.grpc.ManagedChannel],
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE
    ): _root_.cats.effect.Resource[F, $serviceTypeName[F]] =
      _root_.cats.effect.Resource.make(channel)(
        (channel) => CE.void(CE.delay(channel.shutdown()))
      ).evalMap((ch) =>
        CE.delay(new Client[F](ch, options))
      )
    """

  def unsafeClient: Defn.Def =
    q"""
    def unsafeClient[F[_]](
      channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
      channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
      disp: _root_.cats.effect.std.Dispatcher[F],
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE
    ): $serviceTypeName[F] = {
      val managedChannelInterpreter = new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).unsafeBuild(disp)
      new Client[F](managedChannelInterpreter, options)
    }
    """

  def unsafeClientFromChannel: Defn.Def =
    q"""
    def unsafeClientFromChannel[F[_]](
      channel: _root_.io.grpc.Channel,
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE
    ): $serviceTypeName[F] =
      new Client[F](channel, options)
    """

  def contextClientClass: Defn.Class = {
    def method(md: MethodDefn): Defn.Def = {
      val in  = inputType(md)
      val out = outputType(md)

      (md.clientStreaming, md.serverStreaming) match {
        case (false, false) =>
          q"""
          def ${Term.Name(md.name)}(input: $in): ${kleisli(out)} =
            _root_.higherkindness.mu.rpc.internal.client.calls.contextUnary[F, Context, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
        case (true, false) =>
          q"""
          def ${Term.Name(md.name)}(input: _root_.fs2.Stream[$kleisliTypeLambda, $in]): ${kleisli(
              out
            )} =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextClientStreaming[F, Context, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
        case (false, true) =>
          q"""
          def ${Term.Name(md.name)}(input: $in): ${kleisli(
              t"_root_.fs2.Stream[$kleisliTypeLambda, $out]"
            )} =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextServerStreaming[F, Context, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
        case (true, true) =>
          q"""
          def ${Term.Name(md.name)}(input: _root_.fs2.Stream[$kleisliTypeLambda, $in]): ${kleisli(
              t"_root_.fs2.Stream[$kleisliTypeLambda, $out]"
            )} =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextBidiStreaming[F, Context, $in, $out](
              input,
              ${methodDescriptorValName(md)},
              channel,
              options
            )
          """
      }
    }

    q"""
    class ContextClient[F[_], Context](
      channel: _root_.io.grpc.Channel,
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE,
      ${implicitOrUsing(
        param"clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]"
      )}
    ) extends _root_.io.grpc.stub.AbstractStub[ContextClient[F, Context]](channel, options)
      with ($serviceTypeName[$kleisliTypeLambda]) {

      override def build(
        channel: _root_.io.grpc.Channel,
        options: _root_.io.grpc.CallOptions
      ): ContextClient[F, Context] =
        new ContextClient[F, Context](channel, options)

      ..${service.methods.map(method)}

    }
    """
  }

  def contextClientMethod: Defn.Def =
    q"""
    def contextClient[F[_], Context](
      channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
      channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE,
      ${implicitOrUsing(
        param"clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]"
      )}
    ): _root_.cats.effect.Resource[F, $serviceTypeName[$kleisliTypeLambda]] =
      _root_.cats.effect.Resource.make(
        new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
      )((channel) =>
        CE.void(CE.delay(channel.shutdown()))
      ).evalMap((ch) =>
        CE.delay(new ContextClient[F, Context](ch, options))
      )
    """

  def contextClientFromChannel: Defn.Def =
    q"""
    def contextClientFromChannel[F[_], Context](
      channel: F[_root_.io.grpc.ManagedChannel],
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE,
      ${implicitOrUsing(
        param"clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]"
      )}
    ): _root_.cats.effect.Resource[F, $serviceTypeName[$kleisliTypeLambda]] =
      _root_.cats.effect.Resource.make(channel)(
        (channel) => CE.void(CE.delay(channel.shutdown()))
      ).evalMap((ch) =>
        CE.delay(new ContextClient[F, Context](ch, options))
      )
    """

  def unsafeContextClient: Defn.Def =
    q"""
    def unsafeContextClient[F[_], Context](
      channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
      channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
      disp: _root_.cats.effect.std.Dispatcher[F],
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE,
      ${implicitOrUsing(
        param"clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]"
      )}
    ): $serviceTypeName[$kleisliTypeLambda] = {
      val managedChannelInterpreter = new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).unsafeBuild(disp)
      new ContextClient[F, Context](managedChannelInterpreter, options)
    }
    """

  def unsafeContextClientFromChannel: Defn.Def =
    q"""
    def unsafeContextClientFromChannel[F[_], Context](
      channel: _root_.io.grpc.Channel,
      options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
    )(
      $implicitOrUsingCE,
      ${implicitOrUsing(
        param"clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]"
      )}
    ): $serviceTypeName[$kleisliTypeLambda] =
      new ContextClient[F, Context](channel, options)
    """

}
