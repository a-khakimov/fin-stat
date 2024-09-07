import sbtbuildinfo.BuildInfoKeys

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

Compile / run / fork := true

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GitVersioning)
  .enablePlugins(Fs2Grpc)
  .settings(
    name := "RublePulseBot",
    organization := "org.github.ainr",
    assembly / assemblyJarName := "RublePulseApp.jar",
    buildInfoKeys ++= Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      resolvers,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      },
      BuildInfoKey.action("gitHeadCommit") {
        git.gitHeadCommit.value map { sha => s"v$sha" }
      },
      BuildInfoKey.action("github") {
        "https://github.com/a-khakimov/"
      }
    ),
    scalacOptions ++= Seq(
      "-language:postfixOps",
      "-language:implicitConversions",
      "-feature"
    ),
    buildInfoPackage := "org.github.ainr"
  )

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-io" % "3.10.2",
  "co.fs2" %% "fs2-core" % "3.10.2",
  "co.fs2" %% "fs2-reactive-streams" % "3.10.2",
  "co.fs2" %% "fs2-scodec" % "3.10.2",

  "is.cir" %% "ciris" % "3.6.0",
  "lt.dvim.ciris-hocon" %% "ciris-hocon" % "1.2.0",

  "org.typelevel" %% "log4cats-core" % "2.4.0", // Only if you want to Support Any Backend
  "org.typelevel" %% "log4cats-slf4j" % "2.4.0", // Direct Slf4j Support - Recommended

  "com.lihaoyi" %% "sourcecode" % "0.4.2",

  "io.circe" %% "circe-core" % "0.14.9",
  "io.circe" %% "circe-parser" % "0.14.9",
  "io.circe" %% "circe-generic" % "0.14.9",

  "io.github.pityka" %% "nspl-awt" % "0.6.0",

  "tech.tablesaw" % "tablesaw-core" % "0.43.1",
  "tech.tablesaw" % "tablesaw-jsplot" % "0.43.1",

  "io.github.apimorphism" %% "telegramium-core" % "9.77.0",
  "io.github.apimorphism" %% "telegramium-high" % "9.77.0",

  "org.tpolecat" %% "doobie-core"     % "1.0.0-RC5",
  "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC5",
  "org.tpolecat" %% "doobie-specs2"   % "1.0.0-RC5",
  "org.tpolecat" %% "doobie-hikari"   % "1.0.0-RC5",

  // (optional) If you need scalapb/scalapb.proto or anything from
  // google/protobuf/*.proto
  "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.17" % "protobuf",
  "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
).map(_.exclude("org.slf4j", "*"))

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.4.7"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}