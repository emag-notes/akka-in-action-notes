name := "all"

version := "1.0"

organization := "com.manning"

lazy val remoting = project.in(file("chapter-remoting"))

lazy val futures = project.in(file("chapter-futures"))

lazy val fault = project.in(file("chapter-fault-tolerance"))

lazy val test = project.in(file("chapter-testdriven"))

lazy val up = project.in(file("chapter-up-and-running"))

Test / parallelExecution := false
