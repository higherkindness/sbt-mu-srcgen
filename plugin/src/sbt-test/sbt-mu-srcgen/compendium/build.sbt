version := sys.props("version")

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(Seq(
    libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("mu"),
  )
  ))