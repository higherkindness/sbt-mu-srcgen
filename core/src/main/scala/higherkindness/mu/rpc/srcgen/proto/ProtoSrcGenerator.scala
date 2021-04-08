/*
 * Copyright 2020-2021 47 Degrees <https://www.47deg.com>
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

import java.io.File
import java.nio.file.Paths

import scala.meta._
import scala.util.control.NoStackTrace
import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import cats.data.Validated._
import higherkindness.droste.data.Mu
import higherkindness.droste.data.Mu._
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen._
import higherkindness.skeuomorph.mu.{CompressionType, MuF}
import higherkindness.skeuomorph.{mu => skeuomorph}
import higherkindness.skeuomorph.protobuf.ParseProto.{parseProto, ProtoSource}
import higherkindness.skeuomorph.protobuf.{ProtobufF, Protocol}

object ProtoSrcGenerator {

  final case class ProtobufSrcGenException(message: String) extends NoStackTrace {
    override def toString: String = s"ProtoBufSrcGenException: $message"
  }

  def apply(
      streamingImplementation: StreamingImplementation,
      idlTargetDir: File = new File("."),
      compressionType: CompressionType = CompressionType.Identity,
      useIdiomaticEndpoints: Boolean = true
  ): SrcGenerator =
    new SrcGenerator {

      val idlType: IdlType = IdlType.Proto

      def inputFiles(files: Set[File]): List[File] =
        files.filter(_.getName.endsWith(ProtoExtension)).toList

      def generateFrom(
          inputFile: File,
          serializationType: SerializationType
      ): ErrorsOr[Generator.Output] =
        getCode[IO](inputFile)
          .map { case (p, c) =>
            c.map(Generator.Output(Paths.get(p), _))
          }
          .unsafeRunSync()

      val streamCtor: (Type, Type) => Type.Apply = streamingImplementation match {
        case Fs2Stream       => { case (f, a) => t"_root_.fs2.Stream[$f, $a]" }
        case MonixObservable => { case (_, a) => t"_root_.monix.reactive.Observable[$a]" }
      }

      val transformToMuProtocol: Protocol[Mu[ProtobufF]] => skeuomorph.Protocol[Mu[MuF]] =
        skeuomorph.Protocol.fromProtobufProto(compressionType, useIdiomaticEndpoints)

      val generateScalaSource: skeuomorph.Protocol[Mu[MuF]] => Either[String, String] =
        skeuomorph.codegen.protocol(_, streamCtor).map(_.syntax)

      val splitLines: String => List[String] = _.split("\n").toList

      private def getCode[F[_]](
          file: File
      )(implicit F: Sync[F]): F[(String, ErrorsOr[List[String]])] =
        parseProto[F, Mu[ProtobufF]]
          .parse(ProtoSource(file.getName, file.getParent, Some(idlTargetDir.getCanonicalPath)))
          .flatMap { protocol =>
            val path = getPath(protocol)
            (transformToMuProtocol andThen generateScalaSource)(protocol) match {
              case Left(error) =>
                F.raiseError(
                  ProtobufSrcGenException(
                    s"Failed to generate Scala source from Protobuf file ${file.getAbsolutePath}. Error details: $error"
                  )
                )
              case Right(fileContent) =>
                F.pure(path -> Valid(splitLines(fileContent)))
            }
          }

      private def getPath(p: Protocol[Mu[ProtobufF]]): String =
        s"${p.pkg.replace('.', '/')}/${p.name}$ScalaFileExtension"

    }
}
