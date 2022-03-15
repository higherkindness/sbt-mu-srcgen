package higherkindness.mu.rpc.srcgen.proto

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.Descriptors._
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter}
import scala.collection.JavaConverters._

/**
 * A printer that generates a Mu service trait based on a protobuf ServiceDescriptor
 */
class MuServicePrinter(
    service: ServiceDescriptor,
    params: MuServiceParams,
    implicits: DescriptorImplicits
) {
  import implicits._

  def result: CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(s"${service.getFile.scalaDirectory}/${service.getName}.scala")
    b.setContent(content)
    b.build()
  }

  /*
   * Ideally we should avoid imports and use fully-qualified names everywhere,
   * but the @service macro requires the serialization type (e.g. `Protobuf`)
   * to be unqualified.
   */
  private val imports = List(
    "import _root_.higherkindness.mu.rpc.protocol._"
  )

  def content: String = {
    val fp = new FunctionalPrinter()
      .add(s"package ${service.getFile.scalaPackage.fullName}")
      .add("")
      .add(imports: _*)
      .add("")
      .call(printTrait)

    fp.result()
  }

  private def printTrait(fp: FunctionalPrinter): FunctionalPrinter = {
    val namespace =
      if (params.idiomaticEndpoints) {
        s"""Some("${service.getFile.getPackage}")"""
      } else {
        "None"
      }

    fp
      .add(
        s"@service(Protobuf, compressionType = ${params.compressionType}, namespace = $namespace)"
      )
      .add(s"trait ${service.name}[F[_]] {")
      .indented(
        _.print(service.getMethods.asScala)(printMethod)
      )
      .add("}")
  }

  private def printMethod(fp: FunctionalPrinter, method: MethodDescriptor): FunctionalPrinter = {
    val reqInnerType = method.getInputType.scalaType.fullNameWithMaybeRoot
    val reqType =
      if (method.isClientStreaming) {
        s"_root_.fs2.Stream[F, $reqInnerType]"
      } else {
        reqInnerType
      }

    val respInnerType = method.getOutputType.scalaType.fullNameWithMaybeRoot
    val respType =
      if (method.isServerStreaming) {
        s"_root_.fs2.Stream[F, $respInnerType]"
      } else {
        respInnerType
      }

    fp.add(s"def ${method.getName}(req: $reqType): F[$respType]")
  }

}
