import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._

import com.alejandrohdezma.sbt.github.SbtGithubPlugin

import scala.language.reflectiveCalls

object ProjectPlugin extends AutoPlugin {

  override def requires = SbtGithubPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avrohugger: String          = "1.0.0-RC22"
      val circe: String               = "0.13.0"
      val monocle: String             = "2.0.4"
      val mu                          = "0.22.0"
      val scalacheck: String          = "1.14.3"
      val scalatest: String           = "3.1.1"
      val scalatestplusScheck: String = "3.1.0.0-RC2"
      val skeuomorph: String          = "0.0.23"
      val slf4j: String               = "1.7.30"
    }

    lazy val srcGenSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        "io.higherkindness"          %% "mu-rpc-service"           % V.mu,
        "com.github.julien-truffaut" %% "monocle-core"             % V.monocle,
        "io.higherkindness"          %% "skeuomorph"               % V.skeuomorph,
        "com.julianpeeters"          %% "avrohugger-core"          % V.avrohugger,
        "io.circe"                   %% "circe-generic"            % V.circe,
        "org.scalatest"              %% "scalatest"                % V.scalatest           % Test,
        "org.scalacheck"             %% "scalacheck"               % V.scalacheck          % Test,
        "org.scalatestplus"          %% "scalatestplus-scalacheck" % V.scalatestplusScheck % Test,
        "org.slf4j"                   % "slf4j-nop"                % V.slf4j               % Test
      )
    )

    lazy val sbtPluginSettings: Seq[Def.Setting[_]] = Seq(
      sbtPlugin := true,
      scriptedLaunchOpts ++= Seq(
        "-Xmx2048M",
        "-XX:ReservedCodeCacheSize=256m",
        "-Dmu=" + V.mu,
        "-Dversion=" + version.value,
        // See https://github.com/sbt/sbt/issues/3469#issuecomment-521326813
        s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}"
      )
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(name := "sbt-mu-srcgen")

}
