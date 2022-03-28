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
        in = FullyQualified("_root_.com.foo.bar.MethodOneRequest"),
        out = FullyQualified("_root_.com.foo.bar.MethodOneResponse"),
        clientStreaming = false,
        serverStreaming = false
      ),
      MethodDefn(
        name = "methodTwo",
        in = FullyQualified("_root_.com.foo.bar.MethodTwoRequest"),
        out = FullyQualified("_root_.com.foo.bar.MethodTwoResponse"),
        clientStreaming = true,
        serverStreaming = false
      ),
      MethodDefn(
        name = "methodThree",
        in = FullyQualified("_root_.com.foo.bar.MethodThreeRequest"),
        out = FullyQualified("_root_.com.foo.bar.MethodThreeResponse"),
        clientStreaming = false,
        serverStreaming = true
      ),
      MethodDefn(
        name = "methodFour",
        in = FullyQualified("_root_.com.foo.bar.MethodFourRequest"),
        out = FullyQualified("_root_.com.foo.bar.MethodFourResponse"),
        clientStreaming = true,
        serverStreaming = true
      )
    )
  )

  describe("CompanionObjectGenerator") {

    it("generates method descriptors") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          scala3 = false
        )
      )
      val tree = generator.methodDescriptorValDef(serviceDefn.methods.head)

      val expected = q"""
        val methodOneMethodDescriptor: _root_.io.grpc.MethodDescriptor[_root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse] =
          _root_.io.grpc.MethodDescriptor.newBuilder(
            _root_.scalapb.grpc.Marshaller.forMessage[_root_.com.foo.bar.MethodOneRequest],
            _root_.scalapb.grpc.Marshaller.forMessage[_root_.com.foo.bar.MethodOneResponse]
          )
          .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("com.foo.bar.MyService", "methodOne"))
          .build()
        """

      compare(tree, expected)
    }

    it("generates a bindService method with Scala 2 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
          scala3 = false
        )
      )
      val tree = generator.bindService

      val expected = q"""
        def bindService[F[_]](
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
                  _root_.higherkindness.mu.rpc.protocol.Gzip,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[F, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodTwoRequest], _) => algebra.methodTwo(req)),
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[F, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  ((req: _root_.com.foo.bar.MethodThreeRequest, _) => algebra.methodThree(req)),
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[F, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodFourRequest], _) => algebra.methodFour(req)),
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              )
            )
          }
      """

      compare(tree, expected)
    }

    it("generates a bindService method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
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
          _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
            _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
              "com.foo.bar.MyService",
              (
                methodOneMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.handlers.unary[F, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
                  algebra.methodOne,
                  _root_.higherkindness.mu.rpc.protocol.Gzip,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.clientStreaming[F, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodTwoRequest], _) => algebra.methodTwo(req)),
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.serverStreaming[F, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  ((req: _root_.com.foo.bar.MethodThreeRequest, _) => algebra.methodThree(req)),
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.bidiStreaming[F, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  ((req: _root_.fs2.Stream[F, _root_.com.foo.bar.MethodFourRequest], _) => algebra.methodFour(req)),
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
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
          scala3 = false
        )
      )
      val tree = generator.bindContextService

      val expected = {
        import scala.meta.dialects.Scala3
        q"""
        def bindContextService[F[_], Context](
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
                  _root_.higherkindness.mu.rpc.protocol.Gzip,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[F, Context, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  algebra.methodTwo,
                  methodTwoMethodDescriptor,
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[F, Context, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  algebra.methodThree,
                  methodThreeMethodDescriptor,
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextBidiStreaming[F, Context, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  algebra.methodFour,
                  methodFourMethodDescriptor,
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              )
            )
          }
        """
      }

      compare(tree, expected)
    }

    it("generates a bindContextService method with Scala 3 syntax") {
      val generator = new CompanionObjectGenerator(
        serviceDefn,
        MuServiceParams(
          idiomaticEndpoints = true,
          compressionType = GzipGen,
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
          _root_.cats.effect.std.Dispatcher[F].evalMap { disp =>
            _root_.higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder.build[F](
              "com.foo.bar.MyService",
              (
                methodOneMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.handlers.contextUnary[F, Context, _root_.com.foo.bar.MethodOneRequest, _root_.com.foo.bar.MethodOneResponse](
                  algebra.methodOne,
                  methodOneMethodDescriptor,
                  _root_.higherkindness.mu.rpc.protocol.Gzip,
                  disp
                )
              ),
              (
                methodTwoMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextClientStreaming[F, Context, _root_.com.foo.bar.MethodTwoRequest, _root_.com.foo.bar.MethodTwoResponse](
                  algebra.methodTwo,
                  methodTwoMethodDescriptor,
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodThreeMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextServerStreaming[F, Context, _root_.com.foo.bar.MethodThreeRequest, _root_.com.foo.bar.MethodThreeResponse](
                  algebra.methodThree,
                  methodThreeMethodDescriptor,
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              ),
              (
                methodFourMethodDescriptor,
                _root_.higherkindness.mu.rpc.internal.server.fs2.handlers.contextBidiStreaming[F, Context, _root_.com.foo.bar.MethodFourRequest, _root_.com.foo.bar.MethodFourResponse](
                  algebra.methodFour,
                  methodFourMethodDescriptor,
                  disp,
                  _root_.higherkindness.mu.rpc.protocol.Gzip
                )
              )
            )
          }
        """
      }

      compare(tree, expected)
    }

  }

  def compare(actual: Tree, expected: Tree): Assertion = {
    val equal = actual.isEqual(expected)

    if (!equal) {
      println("Actual")
      println(actual.syntax)
      println("----")
      println("Expected:")
      println(expected.syntax)
    }

    assert(equal)
  }

}