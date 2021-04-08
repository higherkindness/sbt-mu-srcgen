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

package higherkindness.mu.rpc.srcgen.avro

import java.io.File
import java.nio.file.{Path, Paths}

import avrohugger.format.Standard
import avrohugger.input.parsers.FileInputParser
import avrohugger.stores.ClassStore
import cats.data.NonEmptyList
import higherkindness.mu.rpc.srcgen.Model._
import higherkindness.mu.rpc.srcgen.{ErrorsOr, Generator, Model, ScalaFileExtension, SrcGenerator}
import org.apache.avro.Protocol
import cats.implicits._
import higherkindness.droste.data.Mu
import higherkindness.skeuomorph.avro.{AvroF, Protocol => AvroProtocol}
import higherkindness.skeuomorph.mu.{CompressionType, MuF, codegen, Protocol => MuProtocol}

import scala.meta._
import scala.util.Try

object AvroSrcGenerator {
  def apply(
      compressionType: CompressionType,
      streamingImplementation: StreamingImplementation,
      useIdiomaticEndpoints: Boolean = true
  ): SrcGenerator = new SrcGenerator {
    private val classStore              = new ClassStore
    private val classLoader             = getClass.getClassLoader
    override def idlType: Model.IdlType = Model.IdlType.Avro

    override protected def inputFiles(files: Set[File]): List[File] =
      files.filter { file =>
        file.getName.endsWith(AvdlExtension) || file.getName.endsWith(AvprExtension)
      }.toList

    override protected def generateFrom(
        inputFile: File,
        serializationType: Model.SerializationType
    ): ErrorsOr[Generator.Output] = {
      val nativeAvroProtocol: ErrorsOr[Protocol] =
        (new FileInputParser)
          .getSchemaOrProtocols(inputFile, Standard, classStore, classLoader)
          // multiple protocols are returned when imports are present.
          // AvroHugger put the one defined in our file in the last position
          // to generate dependent classes first
          .lastOption
          .collect { case Right(protocol) =>
            protocol
          }
          .toValidNel(s"No protocol definition found in $inputFile")

      val skeuomorphAvroProtocol: ErrorsOr[AvroProtocol[Mu[AvroF]]] =
        nativeAvroProtocol.andThen(p =>
          Try(AvroProtocol.fromProto[Mu[AvroF]](p)).toEither.toValidatedNel
            .leftMap { ts: NonEmptyList[Throwable] =>
              ts.map(_.getMessage)
            }
        )

      val source = skeuomorphAvroProtocol.andThen { sap =>
        val muProtocol: MuProtocol[Mu[MuF]] =
          MuProtocol.fromAvroProtocol(compressionType, useIdiomaticEndpoints)(sap)

        val outputFilePath = getPath(sap)

        val streamCtor: (Type, Type) => Type.Apply = streamingImplementation match {
          case Fs2Stream => { case (f, a) =>
            t"_root_.fs2.Stream[$f, $a]"
          }
          case MonixObservable => { case (_, a) =>
            t"_root_.monix.reactive.Observable[$a]"
          }
        }

        val stringified: ErrorsOr[List[String]] =
          codegen
            .protocol(muProtocol, streamCtor)
            .toValidatedNel
            .map(_.syntax.split("\n").toList)

        stringified.map(Generator.Output(outputFilePath, _))
      }
      source
    }
  }
  private def getPath(p: AvroProtocol[Mu[AvroF]]): Path = {
    val pathParts: NonEmptyList[String] =
      NonEmptyList // Non empty list for a later safe `head` call
        .one(      // Start with reverse path of file name, the only part we know is for sure a thing
          s"${p.name}${ScalaFileExtension}"
        )
        .concat(
          p.namespace // and maybe a path prefix from the namespace for the rest
            .map(_.split('.').toList)
            .toList
            .flatten
            .reverse
        )
        .reverse // reverse again to put things in correct order
    Paths.get(pathParts.head, pathParts.tail: _*)
  }

}
