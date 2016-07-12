
scalaVersion := "2.11.8"

organization := "fr.ramiro"

name := "scala-gntp"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.netty" % "netty-all" % "4.0.27.Final",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "test"
)
