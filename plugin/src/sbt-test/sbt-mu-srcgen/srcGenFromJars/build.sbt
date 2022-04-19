import higherkindness.mu.rpc.srcgen.Model.IdlType

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

lazy val domain = project
  .in(file("domain"))
  .enablePlugins(SrcGenPlugin)
  .settings(
    organization := "foo.bar.srcgenfromjars",
    name         := "domain",
    scalaVersion := "2.13.8",
    version := "1.0.0-SNAPSHOT",
    Compile / packageBin / mappings ~= { _.filter(!_._1.getName.endsWith(".class")) },
    muSrcGenIdlType    := IdlType.Avro,
    muSrcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    muSrcGenTargetDir  := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
    )
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    name         := "root",
    scalaVersion := "2.13.8",
    version              := sys.props("version"),
    muSrcGenIdlType      := IdlType.Avro,
    muSrcGenJarNames     := Seq("domain"),
    muSrcGenIdlTargetDir := (Compile / resourceManaged).value / "avro",
    muSrcGenTargetDir    := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "foo.bar.srcgenfromjars" %% "domain" % "1.0.0-SNAPSHOT"
    )
  )
