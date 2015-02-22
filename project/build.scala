import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

object SllappBuild extends Build {
  val Organization = "com.github.kardeiz"
  val Name = "sllapp"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.1"
  val ScalatraVersion = "2.3.0"

  lazy val project = Project (
    "sllapp",
    file("."),
    settings = ScalatraPlugin.scalatraSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      initialCommands in console := """
        |import com.github.kardeiz.sllapp._
        |import Tables._
        |import Models._
        |import LocalDriver.simple._
        |""".stripMargin,
      resolvers ++= Seq(
        Classpaths.typesafeReleases,
        Resolver.mavenLocal
      ),
      TwirlKeys.templateImports in Compile ++= Seq(
        "com.github.kardeiz.sllapp._"
      ),
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "9.1.5.v20140505" % "container",
        "org.eclipse.jetty" % "jetty-plus" % "9.1.5.v20140505" % "container",
        "javax.servlet" % "javax.servlet-api" % "3.1.0",
        "com.scalatags" %% "scalatags" % "0.4.2",
        "com.pkrete" % "jsip2" % "1.1.0",
        "com.typesafe.slick" %% "slick" % "2.1.0",
        "com.h2database" % "h2" % "1.4.185",
        "com.mchange" % "c3p0" % "0.9.5",
        "org.scalatra" %% "scalatra-auth" % "2.4.0.M2",
        "org.virtualbox_4_3" % "vboxjws" % "4.3.0",
        "joda-time" % "joda-time" % "2.7",
        "org.quartz-scheduler" % "quartz" % "2.2.1" exclude("c3p0", "c3p0"),
        "com.typesafe" % "config" % "1.2.1"
      )
    )
  ).enablePlugins(SbtTwirl)
}
