import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._

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
      resolvers ++= Seq(
        Classpaths.typesafeReleases,
        Resolver.mavenLocal
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
        "c3p0" % "c3p0" % "0.9.1.2",
        "org.scalatra" %% "scalatra-auth" % "2.4.0.M2",
        "org.virtualbox_4_3" % "vboxjws" % "4.3.0"
      )
    )
  )
}
