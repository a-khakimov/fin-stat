ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(name := "FinStatus", assembly / assemblyJarName := "App.jar")

//scalapbCodeGeneratorOptions += CodeGeneratorOption.JavaConversions

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
  "io.grpc" % "grpc-protobuf" % "1.51.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "co.fs2" %% "fs2-io" % "3.4.0"
)

// (optional) If you need scalapb/scalapb.proto or anything from
// google/protobuf/*.proto
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
)


libraryDependencies += "co.fs2" %% "fs2-core" % "3.4.0"
libraryDependencies += "co.fs2" %% "fs2-io" % "3.4.0"
libraryDependencies += "co.fs2" %% "fs2-reactive-streams" % "3.4.0"
libraryDependencies += "co.fs2" %% "fs2-scodec" % "3.4.0"
libraryDependencies += "com.github.fd4s" %% "fs2-kafka" % "3.0.0-M8"

enablePlugins(Fs2Grpc)
