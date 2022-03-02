name := "fsm-todo1"

version := "0.1"

scalaVersion := "2.13.8"

val AkkaVersion = "2.6.18"
//libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
//  "org.slf4j" % "slf4j-simple" % "1.7.32",
//  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
//  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test)

libraryDependencies += "ai.x" %% "play-json-extensions" % "0.42.0"


enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("Main")

lazy val Versions = new {
  val akkaVersion = "2.6.18"
}

lazy val akkaDependencies = Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.4",
  "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
  "com.typesafe.akka" %% "akka-actor" % Versions.akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % Versions.akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % Versions.akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % Versions.akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akkaVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % Versions.akkaVersion % Test
)

lazy val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "org.mockito" % "mockito-core" % "2.19.0" % Test,
  "org.scalatest" %% "scalatest" % "3.3.0-SNAP3" % Test
)

lazy val loggingDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "org.slf4j" % "slf4j-api" % "1.7.25"
)

lazy val otherDependencies = Seq(
  "io.spray" %% "spray-json" % "1.3.5"
)

libraryDependencies ++= (
  akkaDependencies++
    loggingDependencies++
    testDependencies++
    otherDependencies
  )