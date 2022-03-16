version := sys.props("version")

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("mu")
)
