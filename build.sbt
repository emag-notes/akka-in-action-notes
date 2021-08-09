name := "all"

version := "1.0"

organization := "com.manning"

lazy val up = project.in(file("chapter-up-and-running"))
lazy val test = project.in(file("chapter-testdriven"))
lazy val fault = project.in(file("chapter-fault-tolerance"))
lazy val futures = project.in(file("chapter-futures"))
lazy val remoting = project.in(file("chapter-remoting"))
lazy val conf = project.in(file("chapter-conf-deploy"))

Test / parallelExecution := false
