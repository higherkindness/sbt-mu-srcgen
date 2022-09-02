ThisBuild / scalaVersion       := "2.12.16"
ThisBuild / organization       := "io.higherkindness"
ThisBuild / githubOrganization := "47deg"

publish / skip := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val core = project
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "mu-srcgen-core")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"        %% "cats-core"           % "2.8.0",
      "com.julianpeeters"    %% "avrohugger-core"     % "1.0.0",
      "com.thesamet.scalapb" %% "compilerplugin"      % "0.11.11",
      "org.scalameta"        %% "scalameta"           % "4.5.9",
      "ch.epfl.scala"        %% "scalafix-core"       % "0.10.1",
      "ch.epfl.scala"        %% "scalafix-cli"        % "0.10.1" cross CrossVersion.full,
      "ch.epfl.scala"         % "scalafix-interfaces" % "0.10.1",
      "org.scalatest"        %% "scalatest"           % "3.2.13"  % Test,
      "org.scalacheck"       %% "scalacheck"          % "1.16.0"  % Test,
      "org.scalatestplus"    %% "scalacheck-1-14"     % "3.2.2.0" % Test,
      "org.slf4j"             % "slf4j-nop"           % "1.7.36"  % Test,
      "org.scalameta"        %% "contrib"             % "4.1.6"   % Test
    ),
    buildInfoPackage := "higherkindness.mu.rpc.srcgen",
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      moduleName,
      version,
      scalaVersion,
      scalaBinaryVersion,
      sbtVersion
    )
  )

val muV = "0.28.0+42-bd246ae6-SNAPSHOT" // TODO update when mu-scala is released
lazy val plugin = project
  .dependsOn(core)
  .settings(moduleName := "sbt-mu-srcgen")
  .enablePlugins(SbtPlugin)
  .settings(
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6"),
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
