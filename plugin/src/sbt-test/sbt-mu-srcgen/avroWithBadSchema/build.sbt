version      := sys.props("version")
scalaVersion := "2.13.8"

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
)

muSrcGenIdlType := higherkindness.mu.rpc.srcgen.Model.IdlType.Avro
muSrcGenAvroGeneratorType := higherkindness.mu.rpc.srcgen.Model.AvrohuggerGen
