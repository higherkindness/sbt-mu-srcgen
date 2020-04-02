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

lazy val `docs` = project
  .in(file("sbt-mu-srcgen-docs"))
  .enablePlugins(MdocPlugin)
  .settings(mdocVariables += "NAME" -> "sbt-mu-srcgen")
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
  .dependsOn(allProjects.map(ClasspathDependency(_, None)): _*)

lazy val root = project
  .in(file("."))
  .settings(moduleName := "sbt-mu-srcgen-root")
  .settings(noPublishSettings)
  .aggregate(allProjects: _*)
  .dependsOn(allProjects.map(ClasspathDependency(_, None)): _*)

addCommandAlias("ci-test", "scalafmtCheck; scalafmtSbtCheck; test; scripted")
addCommandAlias("ci-docs", "docs/mdoc; headerCreateAll")

lazy val allProjects: Seq[ProjectReference] = Seq(core, plugin)
