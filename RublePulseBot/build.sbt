import sbtbuildinfo.BuildInfoKeys

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GitVersioning)
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
  "co.fs2" %% "fs2-io" % "3.6.1",
  "co.fs2" %% "fs2-core" % "3.6.1",
  "co.fs2" %% "fs2-reactive-streams" % "3.6.1",
  "co.fs2" %% "fs2-scodec" % "3.6.1",
  "com.github.fd4s" %% "fs2-kafka" % "3.0.0-M8",
  "is.cir" %% "ciris" % "3.1.0",
  "lt.dvim.ciris-hocon" %% "ciris-hocon" % "1.1.0",
  "org.typelevel" %% "log4cats-core" % "2.4.0",
  "org.typelevel" %% "log4cats-slf4j" % "2.4.0",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "com.lihaoyi" %% "sourcecode" % "0.3.0",
  "io.circe" %% "circe-core" % "0.14.4",
  "io.circe" %% "circe-parser" % "0.14.4",
  "io.circe" %% "circe-generic" % "0.14.4",
  "io.github.pityka" %% "nspl-awt" % "0.6.0",
  "io.github.apimorphism" %% "telegramium-core" % "7.65.0",
  "io.github.apimorphism" %% "telegramium-high" % "7.65.0",
  "dev.profunktor" %% "redis4cats-effects" % "1.4.0",
  "dev.profunktor" %% "redis4cats-streams" % "1.4.0"
)

ThisBuild / assemblyMergeStrategy := {
  case PathList("org", "slf4j", xs @ _*) => MergeStrategy.first
  case x                                 => (ThisBuild / assemblyMergeStrategy).value(x)
}