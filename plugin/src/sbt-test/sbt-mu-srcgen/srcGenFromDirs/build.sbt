import higherkindness.mu.rpc.srcgen.Model.IdlType

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    name            := "root",
    version         := "1.0.0",
    scalaVersion    := "2.13.12",
    muSrcGenIdlType := IdlType.Avro,
    muSrcGenSourceDirs := Seq(
      (Compile / resourceDirectory).value / "domain",
      (Compile / resourceDirectory).value / "protocol"
    ),
    muSrcGenTargetDir := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
    )
  )
