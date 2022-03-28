package higherkindness.mu.rpc.srcgen.proto

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.Descriptors._
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter}
import scala.collection.JavaConverters._
import scala.meta.prettyprinters.Syntax
import higherkindness.mu.rpc.srcgen.service._

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

  def content: String = {
    val fp = new FunctionalPrinter()
      .add(s"package ${service.getFile.scalaPackage.fullName}")
      .add("")
      .call(printTrait)
      .add("")
      .call(printObject)

    fp.result()
  }

  private def printTrait(fp: FunctionalPrinter): FunctionalPrinter = {
    fp
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

  private def printObject(fp: FunctionalPrinter): FunctionalPrinter = {
    val serviceDefn = ServiceDefn(
      service.getName,
      service.getFullName,
      service.methods.toList.map(md =>
        MethodDefn(
          md.getName,
          FullyQualified(md.getInputType.scalaType.fullNameWithMaybeRoot),
          FullyQualified(md.getOutputType.scalaType.fullNameWithMaybeRoot),
          md.isClientStreaming,
          md.isServerStreaming
        )
      )
    )
    val generator = new CompanionObjectGenerator(serviceDefn, params)
    val tree      = generator.generateTree
    fp.add(tree.show[Syntax])
  }

}
