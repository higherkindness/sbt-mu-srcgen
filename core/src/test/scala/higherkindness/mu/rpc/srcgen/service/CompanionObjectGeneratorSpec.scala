package higherkindness.mu.rpc.srcgen.service

import higherkindness.mu.rpc.srcgen.Model._

import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import scala.meta._
import scala.meta.contrib._

class CompanionObjectGeneratorSpec extends AnyFunSpec {

  val serviceDefn = ServiceDefn(
    name = "MyService",
    fullName = "com.foo.bar.MyService",
    methods = List(
      MethodDefn(
        name = "methodOne",
        in = RequestParam.Anon(FullyQualified("_root_.com.foo.bar.MethodOneRequest")),
        out = FullyQualified("_root_.com.foo.bar.MethodOneResponse"),
        clientStreaming = false,
        serverStreaming = false
      ),
      MethodDefn(
        name = "methodTwo",
        in = RequestParam.Anon(FullyQualified("_root_.com.foo.bar.MethodTwoRequest")),
        out = FullyQualified("_root_.com.foo.bar.MethodTwoResponse"),
        clientStreaming = true,
        serverStreaming = false
      ),
      MethodDefn(
        name = "methodThree",
        in = RequestParam.Anon(FullyQualified("_root_.com.foo.bar.MethodThreeRequest")),
        out = FullyQualified("_root_.com.foo.bar.MethodThreeResponse"),
        clientStreaming = false,
        serverStreaming = true
      ),
      MethodDefn(
        name = "methodFour",
        in = RequestParam.Anon(FullyQualified("_root_.com.foo.bar.MethodFourRequest")),
        out = FullyQualified("_root_.com.foo.bar.MethodFourResponse"),
        clientStreaming = true,
        serverStreaming = true
      )
    )
  )

  describe("CompanionObjectGenerator") {

    it("generates a marshaller import (Protobuf, Scala 2)") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.marshallerImport

      val expected = q"import _root_.higherkindness.mu.rpc.internal.encoders.spb._"

      compare(tree, expected)
    }

    it("generates a marshaller import (Protobuf, Scala 3)") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.marshallerImport

      val expected = {
        import scala.meta.dialects.Scala3
        q"import _root_.higherkindness.mu.rpc.internal.encoders.spb.given"
      }

      compare(tree, expected)
    }

    it("generates a marshaller import (Avro, Scala 2)") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Avro,
          scala3 = false
        )
      )
      val tree = generator.marshallerImport

      val expected = q"import _root_.higherkindness.mu.rpc.internal.encoders.avro._"

      compare(tree, expected)
    }

    it("generates a marshaller import (Avro with schema, Scala 2)") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.AvroWithSchema,
          scala3 = false
        )
      )
      val tree = generator.marshallerImport

      val expected = q"import _root_.higherkindness.mu.rpc.internal.encoders.avrowithschema._"

      compare(tree, expected)
    }

    it("generates method descriptors (Scala 2)") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.methodDescriptorValDef(serviceDefn.methods.head)

      val expected = q"""
        val methodOneMethodDescriptor: _root_.io.grpc.MethodDescriptor[_root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse] =
          _root_.io.grpc.MethodDescriptor.newBuilder(
            implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[_root_.com.foo.bar.MethodOneRequest]],
            implicitly[_root_.io.grpc.MethodDescriptor.Marshaller[_root_.com.foo.bar.MethodOneResponse]]
          )
          .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("com.foo.bar.MyService", "methodOne"))
          .build()
        """

      compare(tree, expected)
    }

    it("generates method descriptors (Scala 3)") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.methodDescriptorValDef(serviceDefn.methods.head)

      val expected = q"""
        val methodOneMethodDescriptor: _root_.io.grpc.MethodDescriptor[_root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse] =
          _root_.io.grpc.MethodDescriptor.newBuilder(
            summon[_root_.io.grpc.MethodDescriptor.Marshaller[_root_.com.foo.bar.MethodOneRequest]],
            summon[_root_.io.grpc.MethodDescriptor.Marshaller[_root_.com.foo.bar.MethodOneResponse]]
          )
          .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("com.foo.bar.MyService", "methodOne"))
          .build()
        """

      compare(tree, expected)
    }

    it("generates a _bindService method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator._bindService

      val expected = q"""
        def _bindService[F[_]](
          compressionType: _root_.higherkindness.mu.rpc.protocol.CompressionType
        )(
          implicit CE: _root_.cats.effect.Async[F],
          algebra: MyService[F]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
            _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
              "com.foo.bar.MyService",
              (
                methodOneMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.handlers.unary[F, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
                  algebra.methodOne,
                  compressionType,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[F, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodTwoRequest], _) => algebra.methodTwo(req)),
                  disp,
                  compressionType
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[F, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  ((req: _root_.com.foo.bar.MethodThreeRequest, _) => algebra.methodThree(req)),
                  disp,
                  compressionType
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[F, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodFourRequest], _) => algebra.methodFour(req)),
                  disp,
                  compressionType
                )
              )
            )
          }
      """

      compare(tree, expected)
    }

    it("generates a _bindService method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator._bindService

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def _bindService[F[_]](
          compressionType: _root_.higherkindness.mu.rpc.protocol.CompressionType
        )(
          using CE: _root_.cats.effect.Async[F],
          algebra: MyService[F]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
            _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
              "com.foo.bar.MyService",
              (
                methodOneMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.handlers.unary[F, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
                  algebra.methodOne,
                  compressionType,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[F, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodTwoRequest], _) => algebra.methodTwo(req)),
                  disp,
                  compressionType
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[F, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  ((req: _root_.com.foo.bar.MethodThreeRequest, _) => algebra.methodThree(req)),
                  disp,
                  compressionType
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[F, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodFourRequest], _) => algebra.methodFour(req)),
                  disp,
                  compressionType
                )
              )
            )
          }
        """
      }

      compare(tree, expected)
    }

    it("generates a bindService method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.bindService

      val expected = q"""
        def bindService[F[_]](
          implicit CE: _root_.cats.effect.Async[F],
          algebra: MyService[F]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _bindService[F](_root_.higherkindness.mu.rpc.protocol.Gzip)
      """

      compare(tree, expected)
    }

    it("generates a bindService method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.bindService

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def bindService[F[_]](
          using CE: _root_.cats.effect.Async[F],
          algebra: MyService[F]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _bindService[F](_root_.higherkindness.mu.rpc.protocol.Gzip)
        """
      }

      compare(tree, expected)
    }

    it("generates a _bindContextService method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator._bindContextService

      val expected = q"""
        def _bindContextService[F[_], Context](
          compressionType: _root_.higherkindness.mu.rpc.protocol.CompressionType
        )(
          implicit CE: _root_.cats.effect.Async[F],
          serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
          algebra: MyService[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
            _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
              "com.foo.bar.MyService",
              (
                methodOneMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[F, Context, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
                  algebra.methodOne,
                  methodOneMethodDescriptor,
                  compressionType,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[F, Context, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  algebra.methodTwo,
                  methodTwoMethodDescriptor,
                  disp,
                  compressionType
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[F, Context, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  algebra.methodThree,
                  methodThreeMethodDescriptor,
                  disp,
                  compressionType
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextBidiStreaming[F, Context, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  algebra.methodFour,
                  methodFourMethodDescriptor,
                  disp,
                  compressionType
                )
              )
            )
          }
        """

      compare(tree, expected)
    }

    it("generates a _bindContextService method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator._bindContextService

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def _bindContextService[F[_], Context](
          compressionType: _root_.higherkindness.mu.rpc.protocol.CompressionType
        )(
          using CE: _root_.cats.effect.Async[F],
          serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
          algebra: MyService[[A] =>> _root_.cats.data.Kleisli[F, Context, A]]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
            _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
              "com.foo.bar.MyService",
              (
                methodOneMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[F, Context, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
                  algebra.methodOne,
                  methodOneMethodDescriptor,
                  compressionType,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[F, Context, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  algebra.methodTwo,
                  methodTwoMethodDescriptor,
                  disp,
                  compressionType
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[F, Context, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  algebra.methodThree,
                  methodThreeMethodDescriptor,
                  disp,
                  compressionType
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextBidiStreaming[F, Context, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  algebra.methodFour,
                  methodFourMethodDescriptor,
                  disp,
                  compressionType
                )
              )
            )
          }
        """
      }

      compare(tree, expected)
    }

    it("generates a bindContextService method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.bindContextService

      val expected = q"""
        def bindContextService[F[_], Context](
          implicit CE: _root_.cats.effect.Async[F],
          serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
          algebra: MyService[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _bindContextService[F, Context](_root_.higherkindness.mu.rpc.protocol.Gzip)
        """

      compare(tree, expected)
    }

    it("generates a bindContextService method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.bindContextService

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def bindContextService[F[_], Context](
          using CE: _root_.cats.effect.Async[F],
          serverContext: _root_.higherkindness.mu.rpc.internal.context.ServerContext[F, Context],
          algebra: MyService[[A] =>> _root_.cats.data.Kleisli[F, Context, A]]
        ): _root_.cats.effect.Resource[F, _root_.io.grpc.ServerServiceDefinition] =
          _bindContextService[F, Context](_root_.higherkindness.mu.rpc.protocol.Gzip)
        """
      }

      compare(tree, expected)
    }

    it("generates a Client class") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.clientClass

      val expected = q"""
        class Client[F[_]](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          implicit CE: _root_.cats.effect.Async[F]
        ) extends _root_.io.grpc.stub.AbstractStub[Client[F]](channel, options)
          with MyService[F] {

          override def build(
            channel: _root_.io.grpc.Channel,
            options: _root_.io.grpc.CallOptions
          ): Client[F] =
            new Client[F](channel, options)

          def methodOne(input: _root_.com.foo.bar.MethodOneRequest): F[_root_.com.foo.bar.MethodOneResponse] =
            _root_.higherkindness.mu.rpc.internal.client.calls.unary[F, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
              input,
              methodOneMethodDescriptor,
              channel,
              options
            )

          def methodTwo(input: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodTwoRequest]): F[_root_.com.foo.bar.MethodTwoResponse] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.clientStreaming[F, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
              input,
              methodTwoMethodDescriptor,
              channel,
              options
            )

          def methodThree(input: _root_.com.foo.bar.MethodThreeRequest): F[_root_.fs2.Stream[F, _root_.com.foo.bar.MethodThreeResponse]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.serverStreaming[F, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
              input,
              methodThreeMethodDescriptor,
              channel,
              options
            )

          def methodFour(input: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodFourRequest]): F[_root_.fs2.Stream[F, _root_.com.foo.bar.MethodFourResponse]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.bidiStreaming[F, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
              input,
              methodFourMethodDescriptor,
              channel,
              options
            )
        }
        """

      compare(tree, expected)
    }

    it("generates a client method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.clientMethod

      val expected = q"""
        def client[F[_]](
          channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
          channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          implicit CE: _root_.cats.effect.Async[F]
        ): _root_.cats.effect.Resource[F, MyService[F]] =
          _root_.cats.effect.Resource.make(
            new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
          )(
            (channel) => CE.void(CE.delay(channel.shutdown()))
          ).evalMap((ch) =>
            CE.delay(new Client[F](ch, options))
          )
        """

      compare(tree, expected)
    }

    it("generates a client method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.clientMethod

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def client[F[_]](
          channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
          channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          using CE: _root_.cats.effect.Async[F]
        ): _root_.cats.effect.Resource[F, MyService[F]] =
          _root_.cats.effect.Resource.make(
            new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
          )(
            (channel) => CE.void(CE.delay(channel.shutdown()))
          ).evalMap((ch) =>
            CE.delay(new Client[F](ch, options))
          )
        """
      }

      compare(tree, expected)
    }

    it("generates a ContextClient class with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.contextClientClass

      val expected = q"""
        class ContextClient[F[_], Context](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          implicit CE: _root_.cats.effect.Async[F],
          clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]
        ) extends _root_.io.grpc.stub.AbstractStub[ContextClient[F, Context]](channel, options)
          with MyService[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T] {

          override def build(
            channel: _root_.io.grpc.Channel,
            options: _root_.io.grpc.CallOptions
          ): ContextClient[F, Context] =
            new ContextClient[F, Context](channel, options)

          def methodOne(input: _root_.com.foo.bar.MethodOneRequest): _root_.cats.data.Kleisli[F, Context, _root_.com.foo.bar.MethodOneResponse] =
            _root_.higherkindness.mu.rpc.internal.client.calls.contextUnary[F, Context, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
              input,
              methodOneMethodDescriptor,
              channel,
              options
            )

          def methodTwo(input: _root_.fs2.Stream[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T, _root_.com.foo.bar.MethodTwoRequest]): _root_.cats.data.Kleisli[F, Context, _root_.com.foo.bar.MethodTwoResponse] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextClientStreaming[F, Context, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
              input,
              methodTwoMethodDescriptor,
              channel,
              options
            )

          def methodThree(input: _root_.com.foo.bar.MethodThreeRequest): _root_.cats.data.Kleisli[F, Context, _root_.fs2.Stream[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T, _root_.com.foo.bar.MethodThreeResponse]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextServerStreaming[F, Context, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
              input,
              methodThreeMethodDescriptor,
              channel,
              options
            )

          def methodFour(input: _root_.fs2.Stream[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T, _root_.com.foo.bar.MethodFourRequest]): _root_.cats.data.Kleisli[F, Context, _root_.fs2.Stream[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T, _root_.com.foo.bar.MethodFourResponse]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextBidiStreaming[F, Context, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
              input,
              methodFourMethodDescriptor,
              channel,
              options
            )
        }
        """

      compare(tree, expected)
    }

    it("generates a ContextClient class with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.contextClientClass

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        class ContextClient[F[_], Context](
          channel: _root_.io.grpc.Channel,
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          using CE: _root_.cats.effect.Async[F],
          clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]
        ) extends _root_.io.grpc.stub.AbstractStub[ContextClient[F, Context]](channel, options)
          with MyService[[A] =>> _root_.cats.data.Kleisli[F, Context, A]] {

          override def build(
            channel: _root_.io.grpc.Channel,
            options: _root_.io.grpc.CallOptions
          ): ContextClient[F, Context] =
            new ContextClient[F, Context](channel, options)

          def methodOne(input: _root_.com.foo.bar.MethodOneRequest): _root_.cats.data.Kleisli[F, Context, _root_.com.foo.bar.MethodOneResponse] =
            _root_.higherkindness.mu.rpc.internal.client.calls.contextUnary[F, Context, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
              input,
              methodOneMethodDescriptor,
              channel,
              options
            )

          def methodTwo(input: _root_.fs2.Stream[[A] =>> _root_.cats.data.Kleisli[F, Context, A], _root_.com.foo.bar.MethodTwoRequest]): _root_.cats.data.Kleisli[F, Context, _root_.com.foo.bar.MethodTwoResponse] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextClientStreaming[F, Context, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
              input,
              methodTwoMethodDescriptor,
              channel,
              options
            )

          def methodThree(input: _root_.com.foo.bar.MethodThreeRequest): _root_.cats.data.Kleisli[F, Context, _root_.fs2.Stream[[A] =>> _root_.cats.data.Kleisli[F, Context, A], _root_.com.foo.bar.MethodThreeResponse]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextServerStreaming[F, Context, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
              input,
              methodThreeMethodDescriptor,
              channel,
              options
            )

          def methodFour(input: _root_.fs2.Stream[[A] =>> _root_.cats.data.Kleisli[F, Context, A], _root_.com.foo.bar.MethodFourRequest]): _root_.cats.data.Kleisli[F, Context, _root_.fs2.Stream[[A] =>> _root_.cats.data.Kleisli[F, Context, A], _root_.com.foo.bar.MethodFourResponse]] =
            _root_.higherkindness.mu.rpc.internal.client.fs2.calls.contextBidiStreaming[F, Context, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
              input,
              methodFourMethodDescriptor,
              channel,
              options
            )
        }
        """
      }

      compare(tree, expected)
    }

    it("generates a contextClient method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = false
        )
      )
      val tree = generator.contextClientMethod

      val expected = q"""
        def contextClient[F[_], Context](
          channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
          channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          implicit CE: _root_.cats.effect.Async[F],
          clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]
        ): _root_.cats.effect.Resource[F, MyService[({type T[α] = _root_.cats.data.Kleisli[F, Context, α]})#T]] =
          _root_.cats.effect.Resource.make(
            new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
          )(
            (channel) => CE.void(CE.delay(channel.shutdown()))
          ).evalMap((ch) =>
            CE.delay(new ContextClient[F, Context](ch, options))
          )
        """

      compare(tree, expected)
    }

    it("generates a contextClient method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          serializationType = SerializationType.Protobuf,
          scala3 = true
        )
      )
      val tree = generator.contextClientMethod

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def contextClient[F[_], Context](
          channelFor: _root_.higherkindness.mu.rpc.ChannelFor,
          channelConfigList: List[_root_.higherkindness.mu.rpc.channel.ManagedChannelConfig] = List(_root_.higherkindness.mu.rpc.channel.UsePlaintext()),
          options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT
        )(
          using CE: _root_.cats.effect.Async[F],
          clientContext: _root_.higherkindness.mu.rpc.internal.context.ClientContext[F, Context]
        ): _root_.cats.effect.Resource[F, MyService[[A] =>> _root_.cats.data.Kleisli[F, Context, A]]] =
          _root_.cats.effect.Resource.make(
            new _root_.higherkindness.mu.rpc.channel.ManagedChannelInterpreter[F](channelFor, channelConfigList).build
          )(
            (channel) => CE.void(CE.delay(channel.shutdown()))
          ).evalMap((ch) =>
            CE.delay(new ContextClient[F, Context](ch, options))
          )
        """
      }

      compare(tree, expected)
    }

  }

  def compare(actual: Tree, expected: Tree): Assertion = {
    val equal = actual.isEqual(expected)

    if (!equal) {
      println("Actual:")
      println(actual.syntax)
      // println(actual.structure)
      println("----")
      println("Expected:")
      println(expected.syntax)
      // println(expected.structure)
    }

    assert(equal)
  }

}
