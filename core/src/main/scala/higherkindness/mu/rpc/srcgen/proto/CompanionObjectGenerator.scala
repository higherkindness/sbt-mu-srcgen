// vim: set foldmethod=marker
package higherkindness.mu.rpc.srcgen.proto

import com.google.protobuf.Descriptors._
import scalapb.compiler.DescriptorImplicits
import scala.meta._
import higherkindness.mu.rpc.srcgen.Model.GzipGen
import higherkindness.mu.rpc.srcgen.Model.NoCompressionGen

class CompanionObjectGenerator(
    service: ServiceDescriptor,
    params: MuServiceParams,
    implicits: DescriptorImplicits,
    scala3: Boolean
) {
  import implicits._

  private val fullServiceName =
    if (params.idiomaticEndpoints)
      service.getFullName
    else
      service.getName

  def generateTree: Defn.Object =
    q"""
    object ${Term.Name(service.getName)} {

      ..${service.methods.toList.map(methodDescriptorValDef)}

      $bindService

      $bindContextService

      // TODO Client class
      // TODO client method
      // TODO clientFromChannel
      // TODO unsafeClient
      // TODO unsafeClientFromChannel
      //
      // TODO ContextClient class
      // TODO contextClient method
      // TODO contextClientFromChannel
      // TODO unsafeContextClient
      // TODO unsafeContextClientFromChannel

    }
    """

  def methodDescriptorValName(md: MethodDescriptor): Term.Name =
    Term.Name(s"${md.getName}MethodDescriptor")

  def inputType(md: MethodDescriptor): Type =
    md.getInputType.scalaType.fullNameWithMaybeRoot.parse[Type].get

  def outputType(md: MethodDescriptor): Type =
    md.getOutputType.scalaType.fullNameWithMaybeRoot.parse[Type].get

  def methodDescriptorValDef(md: MethodDescriptor): Defn.Val = {
    val in: Type  = inputType(md)
    val out: Type = outputType(md)

    val methodType = (md.isClientStreaming, md.isServerStreaming) match {
      case (false, false) => "UNARY"
      case (true, false)  => "CLIENT_STREAMING"
      case (false, true)  => "SERVER_STREAMING"
      case (true, true)   => "BIDI_STREAMING"
    }

    val valName = Pat.Var(methodDescriptorValName(md))
    q"""
    val $valName: _root_.io.grpc.MethodDescriptor[$in, $out] =
      _root_.io.grpc.MethodDescriptor.newBuilder(
        _root_.scalapb.grpc.Marshaller.forMessage[$in],
        _root_.scalapb.grpc.Marshaller.forMessage[$out]
      )
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.${Term.Name(methodType)})
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName($fullServiceName, ${md.getName}))
      .build()
    """
  }

  def bindService: Defn.Def = {
    def methodCall(md: MethodDescriptor): Term.Tuple = {
      val in: Type  = inputType(md)
      val out: Type = outputType(md)
      val compression: Term.Select = params.compressionType match {
        case GzipGen          => q"_root_.higherkindness.mu.rpc.protocol.Gzip"
        case NoCompressionGen => q"_root_.higherkindness.mu.rpc.protocol.Identity"
      }
      val algebraMethod = Term.Name(md.getName)

      // a curse on @Daenyth for making the argument order inconsistent between
      // server.handlers and server.fs2.handlers :D
      val serverCallHandler = (md.isClientStreaming, md.isServerStreaming) match {
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

    val methodBody =
      q"""
      _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
          ${service.getFullName},
          ..${service.methods.toList.map(methodCall)}
        )
      }
      """

    val serviceTypeName = Type.Name(service.getName)

    if (scala3) {
      import scala.meta.dialects.Scala3
      q"""
      def bindService[F[_]](using CE: _root_.cats.effect.Async[F], algebra: $serviceTypeName[F]): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] = $methodBody
      """
    } else {
      q"""
      def bindService[F[_]](implicit CE: _root_.cats.effect.Async[F], algebra: $serviceTypeName[F]): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] = $methodBody
      """
    }
  }

  def bindContextService: Defn.Def = {
    def methodCall(md: MethodDescriptor): Term.Tuple = {
      val in: Type  = inputType(md)
      val out: Type = outputType(md)
      val compression: Term.Select = params.compressionType match {
        case GzipGen          => q"_root_.higherkindness.mu.rpc.protocol.Gzip"
        case NoCompressionGen => q"_root_.higherkindness.mu.rpc.protocol.Identity"
      }
      val algebraMethod = Term.Name(md.getName)

      val serverCallHandler = (md.isClientStreaming, md.isServerStreaming) match {
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

    val methodBody =
      q"""
      _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
          ${service.getFullName},
          ..${service.methods.toList.map(methodCall)}
        )
      }
      """

    val serviceTypeName = Type.Name(service.getName)

    if (scala3) {
      import scala.meta.dialects.Scala3
      q"""
      def bindContextService[F[_], Context](
        using
          CE: _root_.cats.effect.Async[F],
          serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
          algebra: $serviceTypeName[[A] =>> _root_.cats.data.Kleisli[F, Context, A]]
      ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] = $methodBody
      """
    } else {
      q"""
      def bindContextService[F[_], Context](
        implicit
          CE: _root_.cats.effect.Async[F],
          serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
          algebra: $serviceTypeName[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T]
      ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] = $methodBody
      """
    }
  }

}

/*
 * example of the macro-generated code, slightly cleaned up
 *
object WeatherService {
  import _root_.higherkindness.mu.rpc.internal.encoders.pbd._
  import _root_.higherkindness.mu.rpc.internal.encoders.spb._

  {{{ method descriptors
  object pingMethodDescriptor {

    def methodDescriptor(implicit
      ReqM: _root_.io.grpc.MethodDescriptor.Marshaller[Empty.type],
      RespM: _root_.io.grpc.MethodDescriptor.Marshaller[Empty.type]
    ): _root_.io.grpc.MethodDescriptor[Empty.type, Empty.type] =
      _root_.io.grpc.MethodDescriptor.newBuilder(ReqM, RespM)
        .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("integrationtest.WeatherService", "ping"))
        .build()
    val _methodDescriptor: _root_.io.grpc.MethodDescriptor[Empty.type, Empty.type] = methodDescriptor
  }
  object getForecastMethodDescriptor {

    def methodDescriptor(implicit ReqM: _root_.io.grpc.MethodDescriptor.Marshaller[GetForecastRequest], RespM: _root_.io.grpc.MethodDescriptor.Marshaller[GetForecastResponse]): _root_.io.grpc.MethodDescriptor[GetForecastRequest, GetForecastResponse] = _root_.io.grpc.MethodDescriptor.newBuilder(ReqM, RespM).setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("integrationtest.WeatherService", "getForecast")).build()
    val _methodDescriptor: _root_.io.grpc.MethodDescriptor[GetForecastRequest, GetForecastResponse] = methodDescriptor
  }
  object publishRainEventsMethodDescriptor {

    def methodDescriptor(implicit ReqM: _root_.io.grpc.MethodDescriptor.Marshaller[RainEvent], RespM: _root_.io.grpc.MethodDescriptor.Marshaller[RainSummaryResponse]): _root_.io.grpc.MethodDescriptor[RainEvent, RainSummaryResponse] = _root_.io.grpc.MethodDescriptor.newBuilder(ReqM, RespM).setType(_root_.io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING).setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("integrationtest.WeatherService", "publishRainEvents")).build()
    val _methodDescriptor: _root_.io.grpc.MethodDescriptor[RainEvent, RainSummaryResponse] = methodDescriptor
  }
  object subscribeToRainEventsMethodDescriptor {

    def methodDescriptor(implicit ReqM: _root_.io.grpc.MethodDescriptor.Marshaller[SubscribeToRainEventsRequest], RespM: _root_.io.grpc.MethodDescriptor.Marshaller[RainEvent]): _root_.io.grpc.MethodDescriptor[SubscribeToRainEventsRequest, RainEvent] = _root_.io.grpc.MethodDescriptor.newBuilder(ReqM, RespM).setType(_root_.io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING).setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("integrationtest.WeatherService", "subscribeToRainEvents")).build()
    val _methodDescriptor: _root_.io.grpc.MethodDescriptor[SubscribeToRainEventsRequest, RainEvent] = methodDescriptor
  }
  }}}

  {{{ bindService
  def bindService[F[_$$1]](
    implicit CE: _root_.cats.effect.Async[F],
    algebra: WeatherService[F]
  ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
    _root_.cats.effect.std.Dispatcher.apply[F](CE)
      .evalMap((disp) =>
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
          "integrationtest.WeatherService",
          (
            pingMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.handlers.unary[F, Empty.type, Empty.type](
              algebra.ping,
              _root_.higherkindness.mu.rpc.protocol.Gzip,
              disp
            )
          ),
          (
            getForecastMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.handlers.unary[F, GetForecastRequest, GetForecastResponse](
              algebra.getForecast,
              _root_.higherkindness.mu.rpc.protocol.Gzip,
              disp
            )
          ),
          (
            publishRainEventsMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[F, RainEvent, RainSummaryResponse](
              ((req: _root_.fs2.Stream[F, RainEvent], x$37) => algebra.publishRainEvents(req)),
              disp,
              _root_.higherkindness.mu.rpc.protocol.Gzip
            )
          ),
          (
            subscribeToRainEventsMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[F, SubscribeToRainEventsRequest, RainEvent](
              ((req: SubscribeToRainEventsRequest, x$38) => algebra.subscribeToRainEvents(req)),
              disp,
              _root_.higherkindness.mu.rpc.protocol.Gzip
            )
          )
        )
      )
  }}}

  {{{ bindContextService
  def bindContextService[F[_$$1], Context](
    implicit CE: _root_.cats.effect.Async[F],
    serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
    algebra: WeatherService[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T]
  ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
    _root_.cats.effect.std.Dispatcher.apply[F](CE)
      .evalMap((disp) =>
        _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
          "integrationtest.WeatherService",
          (
            pingMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[F, Context, Empty.type, Empty.type](
              algebra.ping,
              pingMethodDescriptor._methodDescriptor,
              _root_.higherkindness.mu.rpc.protocol.Gzip,
              disp
            )
          ),
          (
            getForecastMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[F, Context, GetForecastRequest, GetForecastResponse](
              algebra.getForecast,
              getForecastMethodDescriptor._methodDescriptor,
              _root_.higherkindness.mu.rpc.protocol.Gzip,
              disp
            )
          ),
          (
            publishRainEventsMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[F, Context, RainEvent, RainSummaryResponse](
              (algebra.publishRainEvents _),
              publishRainEventsMethodDescriptor._methodDescriptor,
              disp,
              _root_.higherkindness.mu.rpc.protocol.Gzip
            )
          ),
          (
            subscribeToRainEventsMethodDescriptor._methodDescriptor,
            _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[F, Context, SubscribeToRainEventsRequest, RainEvent](
              (algebra.subscribeToRainEvents _),
              subscribeToRainEventsMethodDescriptor._methodDescriptor,
              disp,
              _root_.higherkindness.mu.rpc.protocol.Gzip
            )
          )
        )
      )
  }}}

  {{{ Client class
  // TODO remove unused Context type param
  class Client[F[_$$1], Context](
    channel: _root_.io.grpc.Channel,
    options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
  )(
    implicit val CE: _root_.cats.effect.Async[F]
  ) extends _root_.io.grpc.stub.AbstractStub[Client[F, Context]](channel, options)
    with WeatherService[F] {

    override def build(
      channel: _root_.io.grpc.Channel,
      options: _root_.io.grpc.CallOptions
    ): Client[F, Context] =
      new Client[F, Context](channel, options)

    def ping(input: Empty.type): F[Empty.type] =
      _root_.higherkindness.mu.rpc.internal.client.calls.unary[F, Empty.type, Empty.type](
        input,
        pingMethodDescriptor._methodDescriptor,
        channel,
        options
      )

    def getForecast(input: GetForecastRequest): F[GetForecastResponse] =
      _root_.higherkindness.mu.rpc.internal.client.calls.unary[F, GetForecastRequest, GetForecastResponse](
        input,
        getForecastMethodDescriptor._methodDescriptor,
        channel,
        options
      )

    def publishRainEvents(input: Stream[F, RainEvent]): F[RainSummaryResponse] =
      _root_.higherkindness.mu.rpc.internal.client.fs2.calls.clientStreaming[F, RainEvent, RainSummaryResponse](
        input,
        publishRainEventsMethodDescriptor._methodDescriptor,
        channel,
        options
      )

    def subscribeToRainEvents(input: SubscribeToRainEventsRequest): F[Stream[F, RainEvent]] =
      _root_.higherkindness.mu.rpc.internal.client.fs2.calls.serverStreaming[F, SubscribeToRainEventsRequest, RainEvent](
        input,
        subscribeToRainEventsMethodDescriptor._methodDescriptor,
        channel,
        options
      )
  }
  }}}

  {{{ client method
  def client[F[_$$1]](
    channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
    channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
    options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
  )(
    implicit CE: _root_.cats.effect.Async[F]
  ): _root_.cats.effect.Resource[F, WeatherService[F]] =
    _root_.cats.effect.Resource.make(
      new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
    )(
      (channel) => CE.void(CE.delay(channel.shutdown()))
    ).flatMap((ch) =>
      _root_.cats.effect.Resource.make[F, WeatherService[F]](
        CE.delay(new Client[F, _root_.natchez.Span[F]](ch, options))
      )(
        (x$39) => CE.unit
      )
    )
  }}}

  {{{ clientFromChannel
  def clientFromChannel[F[_$$1]](
    channel: F[_root_.io.grpc.ManagedChannel],
    options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
  )(
    implicit CE: _root_.cats.effect.Async[F]
  ): _root_.cats.effect.Resource[F, WeatherService[F]] =
    _root_.cats.effect.Resource.make(channel)(
      (channel) => CE.void(CE.delay(channel.shutdown()))
    ).evalMap((ch) =>
      CE.delay(new Client[F, _root_.natchez.Span[F]](ch, options))
    )
  }}}

  {{{ unsafeClient
  def unsafeClient[F[_$$1]](channelFor: _root_.higherkindness.mu.rpc.ChannelFor, channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()), disp: _root_.cats.effect.std.Dispatcher[F], options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit CE: _root_.cats.effect.Async[F]): WeatherService[F] = {
    val managedChannelInterpreter = new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).unsafeBuild(disp)
    new Client[F, _root_.natchez.Span[F]](managedChannelInterpreter, options)
  }
  }}}

  {{{ unsafeClientFromChannel
  def unsafeClientFromChannel[F[_$$1]](channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit CE: _root_.cats.effect.Async[F]): WeatherService[F] = new Client[F, _root_.natchez.Span[F]](channel, options)
  }}}

  {{{ ContextClient class
  class ContextClient[F[_$$1], Context](channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit val CE: _root_.cats.effect.Async[F], val clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]) extends _root_.io.grpc.stub.AbstractStub[ContextClient[F, Context]](channel, options) with WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T] {
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): ContextClient[F, Context] = new ContextClient[F, Context](channel, options)

    def ping(input: Empty.type): _root_.cats.data.Kleisli[F, Context, Empty.type] = _root_.higherkindness.mu.rpc.internal.client.calls.contextUnary[F, Context, Empty.type, Empty.type](input, pingMethodDescriptor._methodDescriptor, channel, options)

    def getForecast(input: GetForecastRequest): _root_.cats.data.Kleisli[F, Context, GetForecastResponse] = _root_.higherkindness.mu.rpc.internal.client.calls.contextUnary[F, Context, GetForecastRequest, GetForecastResponse](input, getForecastMethodDescriptor._methodDescriptor, channel, options)

    def publishRainEvents(input: _root_.fs2.Stream[(scala.AnyRef {
      type T[α] = _root_.cats.data.Kleisli[F, Context, α]
    })#T, RainEvent]): _root_.cats.data.Kleisli[F, Context, RainSummaryResponse] = _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextClientStreaming[F, Context, RainEvent, RainSummaryResponse](input, publishRainEventsMethodDescriptor._methodDescriptor, channel, options)

    def subscribeToRainEvents(input: SubscribeToRainEventsRequest): _root_.cats.data.Kleisli[F, Context, _root_.fs2.Stream[(scala.AnyRef {
      type T[α] = _root_.cats.data.Kleisli[F, Context, α]
    })#T, RainEvent]] = _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextServerStreaming[F, Context, SubscribeToRainEventsRequest, RainEvent](input, subscribeToRainEventsMethodDescriptor._methodDescriptor, channel, options)
  }
  }}}

  {{{ contextClient method
  def contextClient[F[_$$1], Context](channelFor: _root_.higherkindness.mu.rpc.ChannelFor, channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()), options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit CE: _root_.cats.effect.Async[F], clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]): _root_.cats.effect.Resource[F, WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T]] = _root_.cats.effect.Resource.make(new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build)(((channel) => CE.void(CE.delay(channel.shutdown())))).flatMap(((ch) => _root_.cats.effect.Resource.make[F, WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T]](CE.delay(new ContextClient[F, Context](ch, options)))(((x$40) => CE.unit))))
  }}}

  {{{ contextClientFromChannel
  def contextClientFromChannel[F[_$$1], Context](channel: F[_root_.io.grpc.ManagedChannel], options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit CE: _root_.cats.effect.Async[F], clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]): _root_.cats.effect.Resource[F, WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T]] = _root_.cats.effect.Resource.make(channel)(((channel) => CE.void(CE.delay(channel.shutdown())))).flatMap(((ch) => _root_.cats.effect.Resource.make[F, WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T]](CE.delay(new ContextClient[F, Context](ch, options)))(((x$41) => CE.unit))))
  }}}

  {{{ unsafeContextClient
  def unsafeContextClient[F[_$$1], Context](channelFor: _root_.higherkindness.mu.rpc.ChannelFor, channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()), disp: _root_.cats.effect.std.Dispatcher[F], options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit CE: _root_.cats.effect.Async[F], clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]): WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T] = {
    val managedChannelInterpreter = new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).unsafeBuild(disp)
    new ContextClient[F, Context](managedChannelInterpreter, options)
  }
  }}}

  {{{ unsafeContextClientFromChannel
  def unsafeContextClientFromChannel[F[_$$1], Context](channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit CE: _root_.cats.effect.Async[F], clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]): WeatherService[(scala.AnyRef {
    type T[α] = _root_.cats.data.Kleisli[F, Context, α]
  })#T] = new ContextClient[F, Context](channel, options)
  }}}
}
 */
