name := "all"

version := "1.0"

organization := "com.manning"

lazy val test = project.in(file("chapter-testdriven"))

lazy val up = project.in(file("chapter-up-and-running"))

Test / parallelExecution := false
