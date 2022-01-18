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

package higherkindness.mu.rpc.srcgen

import java.io.File
import java.nio.file.Path
import higherkindness.mu.rpc.srcgen.avro.{AvroSrcGenerator, LegacyAvroSrcGenerator}
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator
import higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator.HttpImpl
import higherkindness.mu.rpc.srcgen.proto.ProtoSrcGenerator
import higherkindness.skeuomorph.mu.CompressionType

object SrcGenApplication {

  def apply(
      avroGeneratorTypeGen: AvroGeneratorTypeGen,
      marshallersImports: List[MarshallersImport],
      bigDecimalTypeGen: BigDecimalTypeGen,
      compressionTypeGen: CompressionTypeGen,
      useIdiomaticEndpoints: Boolean,
      idlTargetDir: File,
      resourcesBasePath: Path,
      httpImpl: HttpImpl,
      protocVersion: String
  ): GeneratorApplication[SrcGenerator] = {
    val compressionType: CompressionType = compressionTypeGen match {
      case GzipGen          => CompressionType.Gzip
      case NoCompressionGen => CompressionType.Identity
    }
    new GeneratorApplication(
      ProtoSrcGenerator(
        idlTargetDir,
        compressionType,
        useIdiomaticEndpoints,
        Some(protocVersion)
      ),
      avroGeneratorTypeGen match {
        case Model.AvrohuggerGen =>
          LegacyAvroSrcGenerator(
            marshallersImports,
            bigDecimalTypeGen,
            compressionType,
            useIdiomaticEndpoints
          )
        case Model.SkeumorphGen =>
          AvroSrcGenerator(compressionType, useIdiomaticEndpoints)
      },
      OpenApiSrcGenerator(
        httpImpl,
        resourcesBasePath
      )
    )
  }
}
