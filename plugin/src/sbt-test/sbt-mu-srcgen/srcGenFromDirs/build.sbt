import higherkindness.mu.rpc.srcgen.Model.IdlType

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    name := "root",
    version := "1.0.0",
    scalaVersion := "2.13.8",
    scalacOptions += "-Ymacro-annotations",
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
