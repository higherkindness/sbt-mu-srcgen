version := sys.props("version")
scalaVersion := "3.1.1"

enablePlugins(SrcGenPlugin)

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "io.higherkindness" %% "mu-rpc-server" % sys.props("mu")
)

muSrcGenIdlType           := higherkindness.mu.rpc.srcgen.Model.IdlType.Avro
muSrcGenAvroGeneratorType := higherkindness.mu.rpc.srcgen.Model.AvrohuggerGen
