name := "fsm-todo1"

version := "0.1"

scalaVersion := "2.13.8"

val AkkaVersion = "2.6.18"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "org.slf4j" % "slf4j-simple" % "1.7.32",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test)

libraryDependencies += "ai.x" %% "play-json-extensions" % "0.42.0"