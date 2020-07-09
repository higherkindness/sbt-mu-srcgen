version := sys.props("version")

enablePlugins(SrcGenPlugin)

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
)
