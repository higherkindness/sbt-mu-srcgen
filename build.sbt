ThisBuild / scalaVersion       := "2.12.15"
ThisBuild / organization       := "io.higherkindness"
ThisBuild / githubOrganization := "47deg"

publish / skip := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val muV =
  "0.28.0+27-56debc04+20220314-1701-SNAPSHOT" // "0.28.0" // TODO update when mu-scala PR to add marshaller is merged

lazy val core = project
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "mu-srcgen-core")
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness"    %% "mu-rpc-service"  % muV,
      "io.higherkindness"    %% "skeuomorph"      % "0.1.3",
      "com.julianpeeters"    %% "avrohugger-core" % "1.0.0",
      "com.thesamet.scalapb" %% "compilerplugin"  % "0.11.1",
      "org.scalatest"        %% "scalatest"       % "3.2.11"  % Test,
      "org.scalacheck"       %% "scalacheck"      % "1.15.4"  % Test,
      "org.scalatestplus"    %% "scalacheck-1-14" % "3.2.2.0" % Test,
      "org.slf4j"             % "slf4j-nop"       % "1.7.36"  % Test
    ),
    buildInfoPackage := "higherkindness.mu.rpc.srcgen",
    buildInfoKeys    := Seq[BuildInfoKey](organization, version, scalaVersion, sbtVersion)
  )

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
