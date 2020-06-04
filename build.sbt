ThisBuild / scalaVersion := "2.12.11"
ThisBuild / organization := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"

publish / skip := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val muVersion: String = "0.22.2"

lazy val core = project
  .settings(moduleName := "mu-srcgen-core")
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness"          %% "mu-rpc-service"      % muVersion,
      "com.github.julien-truffaut" %% "monocle-core"        % "2.0.4",
      "io.higherkindness"          %% "skeuomorph"          % "0.0.23",
      "com.julianpeeters"          %% "avrohugger-core"     % "1.0.0-RC22",
      "io.circe"                   %% "circe-generic"       % "0.13.0",
      "org.http4s"                 %% "http4s-blaze-client" % "0.21.4",
      "org.http4s"                 %% "http4s-circe"        % "0.21.4",
      "org.scalatest"              %% "scalatest"           % "3.1.2"   % Test,
      "org.scalacheck"             %% "scalacheck"          % "1.14.3"  % Test,
      "org.scalatestplus"          %% "scalacheck-1-14"     % "3.1.2.0" % Test,
      "org.slf4j"                   % "slf4j-nop"           % "1.7.30"  % Test
    )
  )

lazy val plugin = project
  .dependsOn(core)
  .settings(moduleName := "sbt-mu-srcgen")
  .settings(
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      "-XX:ReservedCodeCacheSize=256m",
      "-Dmu=" + muVersion,
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
