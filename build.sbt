name := "all"

version := "1.0"

organization := "com.manning"

lazy val fault  = project.in(file("chapter-fault-tolerance"))
lazy val stream = project.in(file("chapter-stream"))
lazy val test   = project.in(file("chapter-testdriven"))
lazy val up     = project.in(file("chapter-up-and-running"))

parallelExecution in Test := false
