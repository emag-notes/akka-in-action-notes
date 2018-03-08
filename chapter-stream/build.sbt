enablePlugins(JavaServerAppPackaging)

name := "stream"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val akkaVersion     = "2.5.11"
  val akkaHttpVersion = "10.0.11"
  Seq(
    "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core"       % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-testkit"         % akkaVersion % Test,
    "org.scalatest"     %% "scalatest"            % "3.0.5" % Test
  )
}
