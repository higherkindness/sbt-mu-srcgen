import higherkindness.mu.rpc.srcgen.Model.IdlType

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val domain = project
  .in(file("domain"))
  .enablePlugins(SrcGenPlugin)
  .settings(
    organization := "foo.bar.srcgenfromjars2",
    name         := "domain",
    scalaVersion := "2.13.16",
    version      := "1.0.0-SNAPSHOT",
    Compile / packageBin / mappings ~= { _.filter(!_._1.getName.endsWith(".class")) },
    muSrcGenIdlType    := IdlType.Avro,
    muSrcGenSourceDirs := Seq(baseDirectory.value / "src/main/avro"),
    muSrcGenTargetDir  := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-service" % sys.props("mu")
    )
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    name                 := "root",
    scalaVersion         := "2.13.16",
    version              := sys.props("version"),
    muSrcGenIdlType      := IdlType.Avro,
    muSrcGenJarNames     := Seq("domain"),
    muSrcGenIdlTargetDir := (Compile / resourceManaged).value / "avro",
    muSrcGenTargetDir    := (Compile / sourceManaged).value / "generated_from_avro",
    libraryDependencies ++= Seq(
      "foo.bar.srcgenfromjars2" %% "domain" % "1.0.0-SNAPSHOT"
    )
  )
