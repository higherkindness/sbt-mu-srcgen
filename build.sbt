lazy val core = project
  .in(file("core"))
  .settings(moduleName := "mu-srcgen-core")
  .settings(srcGenSettings)

lazy val plugin = project
  .in(file("plugin"))
  .dependsOn(core)
  .settings(moduleName := "sbt-mu-srcgen")
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "mu.rpc.srcgen")
  .enablePlugins(SbtPlugin)

lazy val root = project
  .in(file("."))
  .settings(moduleName := "sbt-mu-srcgen-root")
  .settings(noPublishSettings)
  .aggregate(core, plugin)
  .dependsOn(core, plugin)
