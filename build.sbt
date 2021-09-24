ThisBuild / scalaVersion       := "2.12.15"
ThisBuild / organization       := "io.higherkindness"
ThisBuild / githubOrganization := "47deg"

publish / skip := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val muV = "0.26.0"

lazy val core = project
  .settings(moduleName := "mu-srcgen-core")
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness"          %% "mu-rpc-service"      % muV,
      "io.higherkindness"          %% "skeuomorph"          % "0.0.29",
      "com.github.julien-truffaut" %% "monocle-core"        % "2.1.0",
      "com.julianpeeters"          %% "avrohugger-core"     % "1.0.0-RC24",
      "io.circe"                   %% "circe-generic"       % "0.14.1",
      "org.http4s"                 %% "http4s-blaze-client" % "0.23.4",
      "org.http4s"                 %% "http4s-circe"        % "0.23.4",
      "org.scalatest"              %% "scalatest"           % "3.2.10"  % Test,
      "org.scalacheck"             %% "scalacheck"          % "1.15.4"  % Test,
      "org.scalatestplus"          %% "scalacheck-1-14"     % "3.2.2.0" % Test,
      "org.slf4j"                   % "slf4j-nop"           % "1.7.32"  % Test
    )
  )

lazy val plugin = project
  .dependsOn(core)
  .settings(moduleName := "sbt-mu-srcgen")
  .enablePlugins(SbtPlugin)
  .settings(
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      "-XX:ReservedCodeCacheSize=256m",
      "-Dmu=" + muV,
      "-Dversion=" + version.value
    )
  )

lazy val documentation = project
  .settings(mdocOut := file("."))
  .settings(publish / skip := true)
  .enablePlugins(MdocPlugin)
