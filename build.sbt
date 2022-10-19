ThisBuild / scalaVersion       := "2.12.17"
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
      "org.typelevel"        %% "cats-core"                % "2.8.0",
      "com.julianpeeters"    %% "avrohugger-core"          % "1.2.1",
      "com.thesamet.scalapb" %% "compilerplugin"           % "0.11.11",
      "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.3",
      "org.scalameta"        %% "scalameta"                % "4.5.13",
      "ch.epfl.scala"        %% "scalafix-core"            % "0.10.4",
      "ch.epfl.scala"        %% "scalafix-cli"             % "0.10.4" cross CrossVersion.full,
      "ch.epfl.scala"         % "scalafix-interfaces"      % "0.10.4",
      "org.scalatest"        %% "scalatest"                % "3.2.14"  % Test,
      "org.scalacheck"       %% "scalacheck"               % "1.17.0"  % Test,
      "org.scalatestplus"    %% "scalacheck-1-14"          % "3.2.2.0" % Test,
      "org.slf4j"             % "slf4j-nop"                % "2.0.3"   % Test,
      "org.scalameta"        %% "contrib"                  % "4.1.6"   % Test
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

val muV = "0.29.1"
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
