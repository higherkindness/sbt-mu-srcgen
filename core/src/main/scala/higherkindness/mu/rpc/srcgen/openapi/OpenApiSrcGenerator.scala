/*
 * Copyright 2020 47 Degrees <https://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen.openapi

import java.io.File
import java.nio.file.{Path, Paths}

import higherkindness.mu.rpc.srcgen._
import higherkindness.mu.rpc.srcgen.Model.IdlType
import higherkindness.skeuomorph.openapi._
import schema.OpenApi
import ParseOpenApi._
import print._
import client.print._
import client.http4s.circe._
import client.http4s.print._
import cats.data.Validated.Valid
import cats.implicits._
import higherkindness.skeuomorph.Parser
import cats.effect._
import higherkindness.skeuomorph.openapi.JsonSchemaF

import scala.collection.JavaConverters._

object OpenApiSrcGenerator {
  sealed trait HttpImpl extends Product with Serializable

  object HttpImpl {
    case object Http4sV20 extends HttpImpl
    case object Http4sV18 extends HttpImpl
  }

  def apply(httpImpl: HttpImpl, resourcesBasePath: Path): SrcGenerator =
    new SrcGenerator {
      def idlType: IdlType = IdlType.OpenAPI

      implicit def http4sSpecifics: Http4sSpecifics =
        httpImpl match {
          case HttpImpl.Http4sV18 => client.http4s.print.v18.v18Http4sSpecifics
          case HttpImpl.Http4sV20 => client.http4s.print.v20.v20Http4sSpecifics
        }

      protected def inputFiles(files: Set[File]): List[File] =
        files.collect {
          case json if json.getName.endsWith(JsonExtension) => json
          case yaml if yaml.getName.endsWith(YamlExtension) => yaml
        }.toList

      protected def generateFrom(
          inputFile: File,
          serializationType: Model.SerializationType
      ): ErrorsOr[Generator.Output] =
        getCode[IO](inputFile)
          .map { case (p, c) =>
            c.map(Generator.Output(Paths.get(p), _))
          }
          .unsafeRunSync()

      private def getCode[F[_]: Sync](
          file: File
      ): F[(String, ErrorsOr[List[String]])] =
        parseFile[F]
          .apply(file)
          .map(OpenApi.extractNestedTypes[JsonSchemaF.Fixed])
          .map { openApi =>
            val (_, paths) =
              file.getParentFile.toPath.asScala
                .splitAt(
                  resourcesBasePath.iterator().asScala.size + 1
                ) //we need to add one because it is changing the resource path, adding open api
            val path: Path = Paths.get(paths.map(_.toString()).mkString("/"))
            val pkg        = packageName(path)
            pathFrom(path, file).toString ->
              Valid(
                List(
                  s"package ${pkg.value}",
                  model[JsonSchemaF.Fixed].print(openApi),
                  interfaceDefinition.print(openApi),
                  impl.print(pkg -> openApi)
                ).filter(_.nonEmpty)
              )
          }

      private def packageName(path: Path): PackageName =
        PackageName(path.iterator.asScala.map(_.toString).mkString("."))

      private def pathFrom(path: Path, file: File): Path =
        path.resolve(s"${file.getName.split('.').head}$ScalaFileExtension")

      private def parseFile[F[_]: Sync]: File => F[OpenApi[JsonSchemaF.Fixed]] =
        x => {
          val y = handleFile(x)(
            Parser[F, JsonSource, OpenApi[JsonSchemaF.Fixed]].parse(_),
            Parser[F, YamlSource, OpenApi[JsonSchemaF.Fixed]].parse(_)
          )
          y
        }

      private def handleFile[T](
          file: File
      )(json: JsonSource => T, yaml: YamlSource => T): T =
        file match {
          case x if (x.getName.endsWith(JsonExtension)) => json(JsonSource(x))
          case x if (x.getName.endsWith(YamlExtension)) => yaml(YamlSource(x))
        }
    }
}
