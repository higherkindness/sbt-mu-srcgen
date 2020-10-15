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
      useIdiomaticEndpoints: Boolean,
      streamingImplementation: StreamingImplementation
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
          .collect { case Right(protocol) => protocol } match {
          case first :: _ =>
            first.validNel //multiple protocols are returned when imports are present. We assume the first one is the one defined in our file
          case Nil => s"No protocol definition found in ${inputFile}".invalidNel
        }

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
    val fst :: rest =
      p.namespace.map(_.split('.').toList).toList.flatten :+ s"${p.name}${ScalaFileExtension}"
    Paths.get(fst, rest: _*)
  }

}
