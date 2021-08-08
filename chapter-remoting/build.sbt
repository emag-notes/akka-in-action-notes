name := "goticks"

version := "1.0"

organization := "com.goticks"

scalaVersion := "2.13.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-feature",
  "-language:_"
)

val akkaVersion = "2.6.15"
val akkaHttpVersion = "10.2.5"

import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++=
      Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-remote" % akkaVersion,
        "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
        "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
        "ch.qos.logback" % "logback-classic" % "1.2.5",
        "org.scalatest" %% "scalatest" % "3.2.9" % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
        "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test
      ),
    Test / parallelExecution := false
  )
  .settings(multiJvmSettings: _*)
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
