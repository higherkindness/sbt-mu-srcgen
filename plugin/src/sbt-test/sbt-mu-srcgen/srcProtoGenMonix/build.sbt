import higherkindness.mu.rpc.srcgen.Model.MonixObservable
import higherkindness.mu.rpc.srcgen.Model.IdlType

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    muSrcGenIdlType := IdlType.Proto,
    muSrcGenTargetDir := (Compile / sourceManaged).value / "generated_from_proto",
    muSrcGenStreamingImplementation := MonixObservable,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch),
    libraryDependencies ++= Seq(
        "io.higherkindness"    %% "mu-rpc-service" % sys.props("mu"),
        "io.higherkindness"    %% "mu-rpc-monix" % sys.props("mu")
    )
  )
