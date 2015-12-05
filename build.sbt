name := "scrock"

version := "0.1.0"

organization := "com.busyjay.scrock"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-target:jvm-1.7")

javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.7", "-target", "1.7")

javacOptions in (Compile, doc) := Seq("-source", "1.7", "-encoding", "UTF-8")

scalaVersion := "2.11.7"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.8.1",
  "org.slf4j" % "slf4j-api" % "1.7.13",
  "ch.qos.logback" % "logback-classic" % "1.0.13" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
    