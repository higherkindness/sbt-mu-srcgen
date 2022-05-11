/*
 * Copyright 2020-2022 47 Degrees <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        Model.SerializationType.Protobuf.toString,
        scala3.toString
      )
    )

}
