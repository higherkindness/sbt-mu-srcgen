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

import sbt.Keys._
import sbt.{settingKey, Def, _}
import sbt.io.{Path, PathFinder}
import higherkindness.mu.rpc.srcgen.Model._
import sbtprotoc._
import ProtocPlugin.autoImport._

object SrcGenPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins      = ProtocPlugin

  object autoImport {

    lazy val muSrcGenExtract: TaskKey[Unit] =
      taskKey[Unit](
        "Extract IDL files from the jars specified in muSrcGenJarNames in preparation for code generation"
      )

    lazy val muSrcGenCopy: TaskKey[Unit] =
      taskKey[Unit](
        "Copy IDL files from the source directories (muSrcGenSourceDirs) to the IDL target directory (muSrcGenIdlTargetDir) in preparation for code generation"
      )

    lazy val muSrcGen: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generates mu Scala files from IDL definitions")

    lazy val muSrcGenIdlType: SettingKey[IdlType] =
      settingKey[IdlType]("The IDL type to work with, such as avro or proto")

    lazy val muSrcGenIdlExtension: SettingKey[String] =
      settingKey[String](
        "The IDL extension to work with, files with a different extension will be omitted. By default 'avdl' for avro and 'proto' for proto"
      )

    lazy val muSrcGenSerializationType: SettingKey[SerializationType] =
      settingKey[SerializationType](
        "The serialization type when generating Scala sources from the IDL definitions." +
          "Protobuf, Avro or AvroWithSchema are the current supported serialization types. " +
          "By default, the serialization type is Avro."
      )

    lazy val muSrcGenSourceDirs: SettingKey[Seq[File]] =
      settingKey[Seq[File]]("The IDL directories, where your IDL definitions are placed.")

    lazy val muSrcGenJarNames: SettingKey[Seq[String]] =
      settingKey[Seq[String]](
        "The names of those jars containing IDL definitions that will be used at " +
          "compilation time to generate the Scala Sources. By default, this sequence is empty."
      )

    lazy val muSrcGenIdlTargetDir: SettingKey[File] =
      settingKey[File](
        "The target directory where all the IDL files specified in 'srcGenSourceDirs' will be copied."
      )

    lazy val muSrcGenTargetDir: SettingKey[File] =
      settingKey[File](
        "The Scala target directory, where the `srcGen` task will write the generated files " +
          "in subpackages based on the namespaces declared in the IDL files."
      )

    lazy val muSrcGenBigDecimal: SettingKey[BigDecimalTypeGen] =
      settingKey[BigDecimalTypeGen](
        "The Scala generated type for `decimals`. Possible values are `ScalaBigDecimalGen` and `ScalaBigDecimalTaggedGen`" +
          "The difference is that `ScalaBigDecimalTaggedGen` will append the 'precision' and the 'scale' as tagged types, i.e. `scala.math.BigDecimal @@ (Nat._8, Nat._2)`"
      )

    lazy val muSrcGenMarshallerImports: SettingKey[List[MarshallersImport]] =
      settingKey[List[MarshallersImport]](
        "List of imports needed for creating the request/response marshallers. " +
          "By default, this include the instances for serializing `BigDecimal`, `java.time.LocalDate`, and `java.time.LocalDateTime`"
      )

    lazy val muSrcGenCompressionType: SettingKey[CompressionTypeGen] =
      settingKey[CompressionTypeGen](
        "Specifies the compression type. `NoCompressionGen` by default."
      )

    lazy val muSrcGenIdiomaticEndpoints: SettingKey[Boolean] =
      settingKey[Boolean](
        "If `true`, the gRPC endpoints generated in the services generated from idls will contain the " +
          "namespace as prefix. `true` by default."
      )

    lazy val muSrcGenAvroGeneratorType: SettingKey[AvroGeneratorTypeGen] =
      settingKey[AvroGeneratorTypeGen](
        "Specifies the Avro generation type: `SkeumorphGen` or `AvrohuggerGen`. `SkeumorphGen` by default."
      )

    lazy val muSrcGenProtocVersion: SettingKey[Option[String]] =
      settingKey[Option[String]](
        s"Specifies the protoc version. If not set, ScalaPB's default version is used."
      )

  }

  import autoImport._

  lazy val defaultSettings: Seq[Def.Setting[_]] = Seq(
    muSrcGenIdlType := IdlType.Unknown,
    muSrcGenIdlExtension := {
      muSrcGenIdlType.value match {
        case IdlType.Avro  => "avdl"
        case IdlType.Proto => "proto"
        case _             => "unknown"
      }
    },
    muSrcGenSerializationType := SerializationType.Avro,
    muSrcGenJarNames          := Seq.empty,
    muSrcGenSourceDirs        := Seq((Compile / resourceDirectory).value),
    muSrcGenIdlTargetDir := (Compile / resourceManaged).value / muSrcGenIdlType.value.toString.toLowerCase,
    muSrcGenTargetDir  := (Compile / sourceManaged).value,
    muSrcGenBigDecimal := ScalaBigDecimalTaggedGen,
    muSrcGenMarshallerImports := {
      muSrcGenSerializationType.value match {
        case SerializationType.Avro | SerializationType.AvroWithSchema =>
          val bigDecimal = muSrcGenBigDecimal.value match {
            case ScalaBigDecimalGen       => BigDecimalAvroMarshallers
            case ScalaBigDecimalTaggedGen => BigDecimalTaggedAvroMarshallers
          }
          List(bigDecimal, JavaTimeDateAvroMarshallers)
        case _ =>
          Nil
      }
    },
    muSrcGenCompressionType    := NoCompressionGen,
    muSrcGenIdiomaticEndpoints := true,
    muSrcGenAvroGeneratorType  := SkeumorphGen,
    muSrcGenProtocVersion      := None
  )

  lazy val taskSettings: Seq[Def.Setting[_]] =
    Seq(
      Compile / muSrcGenExtract := Def.task {
        val _ = (Compile / dependencyClasspath).value.map(entry =>
          extractIDLDefinitionsFromJar(
            entry,
            muSrcGenJarNames.value,
            muSrcGenIdlTargetDir.value,
            muSrcGenIdlExtension.value
          )
        )
      }.value,
      Compile / muSrcGenCopy := Def.task {
        muSrcGenSourceDirs.value.toSet.foreach { f: File =>
          IO.copyDirectory(
            f,
            muSrcGenIdlTargetDir.value,
            CopyOptions(
              overwrite = true,
              preserveLastModified = true,
              preserveExecutable = true
            )
          )
        }
      }.value,
      Compile / muSrcGen := Def
        .task {
          muSrcGenIdlType.value match {
            case IdlType.Proto =>
              // If we are doing srcgen from protobuf, we don't need to run
              // our source generator because ScalaPB is in charge of the srcgen
              Nil
            case _ =>
              srcGenTask(
                SrcGenApplication(
                  muSrcGenAvroGeneratorType.value,
                  muSrcGenMarshallerImports.value,
                  muSrcGenBigDecimal.value,
                  muSrcGenCompressionType.value,
                  muSrcGenIdiomaticEndpoints.value,
                  scala3 = scalaBinaryVersion.value.startsWith("3")
                ),
                muSrcGenIdlType.value,
                muSrcGenSerializationType.value,
                muSrcGenTargetDir.value,
                target.value / "srcGen"
              )(muSrcGenIdlTargetDir.value.allPaths.get.toSet).toSeq
          }
        }
        .dependsOn(
          Compile / muSrcGenExtract,
          Compile / muSrcGenCopy
        )
        .value,
      Compile / PB.generate := (Compile / PB.generate)
        .dependsOn(
          Compile / muSrcGenExtract,
          Compile / muSrcGenCopy
        )
        .value
    )

  lazy val packagingSettings: Seq[Def.Setting[_]] = Seq(
    /*
     * Iterate through all files in the IDL target directory,
     * which includes both files copied from the IDL source directories
     * and IDL files extracted from jars.
     *
     * - add them to packageSrc/mappings so they will be included in the
     *   sources jar
     * - add them to packageBin/mappings so they will be included in the binary
     *   jar as well
     */

    Compile / packageSrc / mappings ++= {
      val existingMappings = (Compile / packageSrc / mappings).value
      val existingPaths    = existingMappings.map { case (_, path) => path }.toSet

      val idlTargetDir      = (Compile / muSrcGenIdlTargetDir).value
      val allIDLDefinitions = (idlTargetDir ** "*") filter { _.isFile }

      val unfilteredMappings = allIDLDefinitions.pair(Path.relativeTo(idlTargetDir))

      // Filter out any mappings that would conflict with mappings that are
      // already present, e.g. because the file was in src/main/resources
      val filteredMappings =
        unfilteredMappings.filterNot { case (_, path) => existingPaths.contains(path) }

      filteredMappings
    },
    Compile / packageBin / mappings ++= {
      val existingMappings = (Compile / packageBin / mappings).value
      val existingPaths    = existingMappings.map { case (_, path) => path }.toSet

      val idlTargetDir      = (Compile / muSrcGenIdlTargetDir).value
      val allIDLDefinitions = (idlTargetDir ** "*") filter { _.isFile }

      val unfilteredMappings = allIDLDefinitions.pair(Path.relativeTo(idlTargetDir))

      // Filter out any mappings that would conflict with mappings that are
      // already present, e.g. because the file was in src/main/resources
      val filteredMappings =
        unfilteredMappings.filterNot { case (_, path) => existingPaths.contains(path) }

      filteredMappings
    }
  )

  lazy val sourceGeneratorSettings: Seq[Def.Setting[_]] = Seq(
    // Register the muSrcGen task as a source generator.
    // If we don't do this, the compile task will not see the
    // generated files even if the user manually runs the muSrcGen task.
    Compile / sourceGenerators += (Compile / muSrcGen).taskValue
  )

  lazy val scalapbSettings: Seq[Def.Setting[_]] = Seq(
    Compile / PB.protocVersion := {
      muSrcGenProtocVersion.value match {
        case Some(v) => s"-v${v.stripPrefix("-v")}" // scalapb wants e.g. "-v3.19.2"
        case None    => (Compile / PB.protocVersion).value
      }
    },
    Compile / PB.protoSources := List(muSrcGenIdlTargetDir.value),

    /*
     * sbt-protoc adds PB.protoSources to both unmanagedSourceDirectories and
     * unmanagedResourceDirectories:
     *
     * - unmanagedSourceDirectories so the .proto files show up in the user's
     *   IDE
     * - unmanagedResourceDirectories so they get added to the jar when
     *   packaging
     *
     * We don't really care about the former, but the latter causes problems
     * when the original IDL files are under src/main/resources (which is the
     * Mu convention).
     *
     * Example (based on the srcGenFromJars scripted test):
     *
     * - Say we have a source IDL file `src/main/resources/Hello.avdl`
     *
     * - The `muSrcGenCopy` task will copy it to the IDL target dir, say
     *   `target/scala-2.12/resource_managed/main/avro/Hello.avdl`
     *
     * - sbt-protoc will add the IDL target dir to the
     *   `unmanagedResourceDirectories` list
     *
     * - When we try to build a jar, the `packageSrc` task will try to copy two
     *   files to the same location in the jar:
     *    - `src/main/resources/Hello.avdl` -> `Hello.avdl`
     *    - `target/scala-2.12/resource_managed/main/avro/Hello.avdl` -> `Hello.avdl`
     *
     * - This results in an error: "java.util.zip.ZipException: duplicate
     *   entry: Hello.avdl"
     *
     * This problem does not occur when using sbt-protoc directly without
     * sbt-mu-srcgen, because ScalaPB convention is to put the source IDL files
     * under `src/main/protobuf`, not `/src/main/resources`.
     *
     * Long story short, we just need to undo ScalaPB's change and remove the
     * IDL target directory from `unmanagedResourceDirectories`.
     *
     * When we add the IDL files to the source and binary jars in
     * `packagingSettings`, we explicitly filter out any files that have
     * already been added, to avoid this kind of filename conflict.
     */
    Compile / unmanagedResourceDirectories -= muSrcGenIdlTargetDir.value,
    Compile / PB.targets := {
      muSrcGenIdlType.value match {
        case IdlType.Proto =>
          Seq(
            // first do the standard ScalaPB codegen to generate the message classes
            scalapb.gen(
              grpc = false
            ) -> muSrcGenTargetDir.value,

            // then use our protoc plugin to generate the Mu service trait
            higherkindness.mu.rpc.srcgen.proto.gen(
              idiomaticEndpoints = muSrcGenIdiomaticEndpoints.value,
              compressionType = muSrcGenCompressionType.value,
              scala3 = scalaBinaryVersion.value.startsWith("3")
            ) -> muSrcGenTargetDir.value
          )
        case _ =>
          // If we are doing codgen from Avro, we will use our own source generator.
          // So we don't give ScalaPB any targets, effectively disabling ScalaPB.
          Nil
      }
    }
  )

  private def srcGenTask(
      generator: GeneratorApplication,
      idlType: IdlType,
      serializationType: SerializationType,
      targetDir: File,
      cacheDir: File
  ): Set[File] => Set[File] =
    FileFunction.cached(cacheDir, FilesInfo.lastModified, FilesInfo.exists) {
      inputFiles: Set[File] =>
        generator.generateSources(idlType, serializationType, inputFiles, targetDir).toSet
    }

  private def extractIDLDefinitionsFromJar(
      classpathEntry: Attributed[File],
      jarNames: Seq[String],
      target: File,
      idlExtension: String
  ): File = {

    val nameFilter: NameFilter = new NameFilter {
      override def accept(name: String): Boolean =
        name.toLowerCase.endsWith("." + idlExtension)
    }

    classpathEntry.get(artifact.key).fold((): Unit) { entryArtifact =>
      if (jarNames.exists(entryArtifact.name.startsWith)) {
        IO.withTemporaryDirectory { tmpDir =>
          if (classpathEntry.data.isDirectory) {
            val sources = PathFinder(classpathEntry.data).allPaths pair Path
              .rebase(classpathEntry.data, target)
            IO.copy(
              sources.filter(tuple => nameFilter.accept(tuple._2)),
              overwrite = true,
              preserveLastModified = true,
              preserveExecutable = true
            )
            (): Unit
          } else if (classpathEntry.data.exists) {
            IO.unzip(classpathEntry.data, tmpDir, nameFilter)
            IO.copyDirectory(tmpDir, target)
          }
        }
      }
    }
    target
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    defaultSettings ++ taskSettings ++ packagingSettings ++ sourceGeneratorSettings ++ scalapbSettings
}
