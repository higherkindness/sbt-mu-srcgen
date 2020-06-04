ThisBuild / scalaVersion := "2.12.11"
ThisBuild / organization := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"

publish / skip := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "project-docs/mdoc; headerCreateAll")

lazy val V = new {
  val avrohugger: String          = "1.0.0-RC22"
  val circe: String               = "0.13.0"
  val monocle: String             = "2.0.4"
  val mu                          = "0.22.2"
  val scalacheck: String          = "1.14.3"
  val scalatest: String           = "3.1.2"
  val scalatestplusScheck: String = "3.1.0.0-RC2"
  val skeuomorph: String          = "0.0.23"
  val slf4j: String               = "1.7.30"
  val http4s: String              = "0.21.4"
}

lazy val core = project
  .settings(moduleName := "mu-srcgen-core")
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness"          %% "mu-rpc-service"           % V.mu,
      "com.github.julien-truffaut" %% "monocle-core"             % V.monocle,
      "io.higherkindness"          %% "skeuomorph"               % V.skeuomorph,
      "com.julianpeeters"          %% "avrohugger-core"          % V.avrohugger,
      "io.circe"                   %% "circe-generic"            % V.circe,
      "org.http4s"                 %% "http4s-blaze-client"      % V.http4s,
      "org.http4s"                 %% "http4s-circe"             % V.http4s,
      "org.scalatest"              %% "scalatest"                % V.scalatest           % Test,
      "org.scalacheck"             %% "scalacheck"               % V.scalacheck          % Test,
      "org.scalatestplus"          %% "scalatestplus-scalacheck" % V.scalatestplusScheck % Test,
      "org.slf4j"                   % "slf4j-nop"                % V.slf4j               % Test
    )
  )

lazy val plugin = project
  .dependsOn(core)
  .settings(moduleName := "sbt-mu-srcgen")
  .settings(
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      "-XX:ReservedCodeCacheSize=256m",
      "-Dmu=" + V.mu,
      "-Dversion=" + version.value,
      // See https://github.com/sbt/sbt/issues/3469#issuecomment-521326813
      s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}"
    )
  )
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "mu.rpc.srcgen")
  .enablePlugins(SbtPlugin)

lazy val `project-docs` = (project in file(".docs"))
  .settings(moduleName := "sbt-mu-srcgen-project-docs")
  .settings(mdocIn := file(".docs"))
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
  .enablePlugins(MdocPlugin)
  .dependsOn(core, plugin)
