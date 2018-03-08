name := "all"

version := "1.0"

organization := "com.manning"

lazy val stream = project.in(file("chapter-stream"))
lazy val up     = project.in(file("chapter-up-and-running"))

parallelExecution in Test := false
