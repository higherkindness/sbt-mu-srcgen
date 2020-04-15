addCommandAlias("ci-test", "scalafmtCheck; scalafmtSbtCheck; test; scripted")
addCommandAlias("ci-docs", "project-docs/mdoc; headerCreateAll")

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

lazy val `project-docs` = (project in file(".docs"))
  .settings(moduleName := "sbt-mu-srcgen-project-docs")
  .settings(mdocIn := file(".docs"))
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
  .enablePlugins(MdocPlugin)
  .dependsOn(allProjects.map(ClasspathDependency(_, None)): _*)

lazy val root = project
  .in(file("."))
  .settings(moduleName := "sbt-mu-srcgen-root")
  .settings(skip in publish := true)
  .aggregate(allProjects: _*)
  .dependsOn(allProjects.map(ClasspathDependency(_, None)): _*)

lazy val allProjects: Seq[ProjectReference] = Seq(core, plugin)
