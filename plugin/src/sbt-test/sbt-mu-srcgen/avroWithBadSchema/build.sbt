version      := sys.props("version")
scalaVersion := "2.13.15"

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
)

muSrcGenIdlType := higherkindness.mu.rpc.srcgen.Model.IdlType.Avro
