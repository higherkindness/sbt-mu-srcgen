package higherkindness.mu.rpc.srcgen.proto

import protocbridge.Artifact
import protocbridge.SandboxedJvmGenerator
import higherkindness.mu.rpc.srcgen.Model

object gen {

  /**
   * The entrypoint to our protoc plugin. This is what is called from the sbt plugin.
   */
  def apply(
      idiomaticEndpoints: Boolean,
      compressionType: Model.CompressionTypeGen
  ): (SandboxedJvmGenerator, Seq[String]) =
    (
      SandboxedJvmGenerator.forModule(
        "scala",
        Artifact(
          higherkindness.mu.rpc.srcgen.BuildInfo.organization,
          "mu-srcgen-core_2.12",
          higherkindness.mu.rpc.srcgen.BuildInfo.version
        ),
        MuServiceGenerator.getClass.getName,
        Nil
      ),
      List(
        idiomaticEndpoints.toString,
        compressionType.annotationParameterValue
      )
    )

}
