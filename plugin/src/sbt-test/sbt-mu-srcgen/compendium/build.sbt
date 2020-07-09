version := sys.props("version")

enablePlugins(SrcGenPlugin)

libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("mu")
)