version := sys.props("version")
scalaVersion := "2.13.8"
scalacOptions += "-Ymacro-annotations"

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
)
