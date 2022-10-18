import higherkindness.mu.rpc.srcgen.Model.IdlType

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    crossScalaVersions := Seq("2.13.8", "3.1.1"),
    muSrcGenIdlType    := IdlType.Proto,
    muSrcGenTargetDir  := (Compile / sourceManaged).value / "compiled_proto",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-service" % sys.props("mu"),
      "io.higherkindness" %% "mu-rpc-fs2"     % sys.props("mu")
    )
  )
