version := sys.props("version")

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(Seq(
    libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu"),
  "io.higherkindness" %% "mu-rpc-marshallers-jodatime" % sys.props("mu")
  )
  ))

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)

val checkCustomImport = taskKey[Unit]("Check the custom import is added to the generated file")
checkCustomImport := {
  val generatedFile = file("target/scala-2.12/src_managed/main/io/higherkindness/MyService.scala")
  val lines = sbt.io.IO.readLines(generatedFile)
  assert(lines.contains("import com.sample.marshallers._"))
}


lazy val util = (project in file("plugin/src/sbt-test"))
  .enablePlugins(SrcGenPlugin)
  .settings(
    name := "hello-util"
  )
