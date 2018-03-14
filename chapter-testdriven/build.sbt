name := "testdriven"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val akkaVersion = "2.5.11"

  Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"   % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "org.scalatest"     %% "scalatest"    % "3.0.5" % Test
  )
}
