name := "futures"

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

libraryDependencies ++= {
  val akkaVersion = "2.6.15"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.github.nscala-time" %% "nscala-time" % "2.28.0",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "3.2.9" % "test"
  )
}
