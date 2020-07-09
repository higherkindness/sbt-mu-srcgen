import higherkindness.mu.rpc.srcgen.Model.IdlType

lazy val root = project
  .in(file("."))
    .enablePlugins(SrcGenPlugin)
  .settings(
    muSrcGenIdlType := IdlType.Proto,
    muSrcGenTargetDir := (Compile / sourceManaged).value / "compiled_proto",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
        "io.higherkindness"    %% "mu-rpc-service" % sys.props("mu"),
        "io.higherkindness"    %% "mu-rpc-fs2" % sys.props("mu")
    )
  )
