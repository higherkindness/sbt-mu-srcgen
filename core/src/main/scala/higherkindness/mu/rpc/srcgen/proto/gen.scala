package higherkindness.mu.rpc.srcgen.proto

import protocbridge.Artifact
import protocbridge.SandboxedJvmGenerator
import higherkindness.mu.rpc.srcgen.{BuildInfo, Model}

object gen {

  /**
   * The entrypoint to our protoc plugin. This is what is called from the sbt plugin.
   */
  def apply(
      idiomaticEndpoints: Boolean,
      compressionType: Model.CompressionTypeGen,
      scala3: Boolean
  ): (SandboxedJvmGenerator, Seq[String]) =
    (
      SandboxedJvmGenerator.forModule(
        "scala",
        Artifact(
          BuildInfo.organization,
          s"${BuildInfo.moduleName}_${BuildInfo.scalaBinaryVersion}",
          BuildInfo.version
        ),
        MuServiceGenerator.getClass.getName,
        Nil
      ),
      List(
        idiomaticEndpoints.toString,
        compressionType.annotationParameterValue,
        scala3.toString
      )
    )

}
