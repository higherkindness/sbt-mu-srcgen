version := sys.props("version")

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)
