version := sys.props("version")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(Seq(
    libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu"),
  )
  ))
