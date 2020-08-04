ThisBuild / scalaVersion := "2.12.11"
ThisBuild / organization := "io.higherkindness"
ThisBuild / githubOrganization := "47deg"

publish / skip := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val core = project
  .settings(moduleName := "mu-srcgen-core")
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness"          %% "mu-rpc-service"      % "0.22.3",
      "com.github.julien-truffaut" %% "monocle-core"        % "2.0.5",
      "io.higherkindness"          %% "skeuomorph"          % "0.0.24",
      "com.julianpeeters"          %% "avrohugger-core"     % "1.0.0-RC22",
      "io.circe"                   %% "circe-generic"       % "0.13.0",
      "org.http4s"                 %% "http4s-blaze-client" % "0.21.6",
      "org.http4s"                 %% "http4s-circe"        % "0.21.6",
      "org.scalatest"              %% "scalatest"           % "3.2.0"   % Test,
      "org.scalacheck"             %% "scalacheck"          % "1.14.3"  % Test,
      "org.scalatestplus"          %% "scalacheck-1-14"     % "3.2.0.0" % Test,
      "org.slf4j"                   % "slf4j-nop"           % "1.7.30"  % Test
    )
  )

lazy val plugin = project
  .dependsOn(core)
  .settings(moduleName := "sbt-mu-srcgen")
  .enablePlugins(SbtPlugin)
  .settings(
    scalacOptions += "-Ypartial-unification",
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      "-XX:ReservedCodeCacheSize=256m",
      "-Dmu=0.22.3",
      "-Dversion=" + version.value
    )
  )

lazy val documentation = project
  .settings(mdocOut := file("."))
  .settings(publish / skip := true)
  .enablePlugins(MdocPlugin)
