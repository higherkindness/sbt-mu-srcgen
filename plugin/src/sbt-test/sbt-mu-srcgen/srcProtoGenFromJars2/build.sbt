import higherkindness.mu.rpc.srcgen.Model.IdlType

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val domain = project
  .in(file("domain"))
  .enablePlugins(SrcGenPlugin)
  .settings(
    organization := "foo.bar.srcprotogenfromjars2",
    name         := "domain",
    scalaVersion := "2.13.16",
    version      := "1.0.0-SNAPSHOT",
    Compile / packageBin / mappings ~= { _.filter(!_._1.getName.endsWith(".class")) },
    muSrcGenIdlType    := IdlType.Proto,
    muSrcGenSourceDirs := Seq(baseDirectory.value / "src/main/proto"),
    muSrcGenTargetDir  := (Compile / sourceManaged).value / "generated_from_proto",
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-service" % sys.props("mu"),
      "io.higherkindness" %% "mu-rpc-fs2"     % sys.props("mu")
    ),
    scalacOptions += "-Ymacro-annotations"
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(SrcGenPlugin)
  .settings(
    name                 := "root",
    scalaVersion         := "2.13.16",
    version              := sys.props("version"),
    muSrcGenIdlType      := IdlType.Proto,
    muSrcGenJarNames     := Seq("domain"),
    muSrcGenIdlTargetDir := (Compile / resourceManaged).value / "proto",
    muSrcGenTargetDir    := (Compile / sourceManaged).value / "generated_from_proto",
    libraryDependencies ++= Seq(
      "foo.bar.srcprotogenfromjars2" %% "domain" % "1.0.0-SNAPSHOT"
    ),
    scalacOptions += "-Ymacro-annotations"
  )
