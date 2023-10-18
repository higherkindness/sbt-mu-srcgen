version            := sys.props("version")
scalaVersion       := "2.13.12"
crossScalaVersions := List(scalaVersion.value, "3.3.1")

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
)

muSrcGenIdlType := higherkindness.mu.rpc.srcgen.Model.IdlType.Avro
