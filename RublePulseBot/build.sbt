import sbtbuildinfo.BuildInfoKeys

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

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
  "co.fs2" %% "fs2-io" % "3.10.2",
  "co.fs2" %% "fs2-core" % "3.10.2",
  "co.fs2" %% "fs2-reactive-streams" % "3.10.2",
  "co.fs2" %% "fs2-scodec" % "3.10.2",
  "is.cir" %% "ciris" % "3.6.0",
  "lt.dvim.ciris-hocon" %% "ciris-hocon" % "1.2.0",
  "org.typelevel" %% "log4cats-core" % "2.4.0",
  "org.typelevel" %% "log4cats-slf4j" % "2.4.0",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "com.lihaoyi" %% "sourcecode" % "0.4.2",
  "io.circe" %% "circe-core" % "0.14.9",
  "io.circe" %% "circe-parser" % "0.14.9",
  "io.circe" %% "circe-generic" % "0.14.9",
  "io.github.pityka" %% "nspl-awt" % "0.6.0",
  "io.github.apimorphism" %% "telegramium-core" % "9.77.0",
  "io.github.apimorphism" %% "telegramium-high" % "9.77.0"
)

ThisBuild / assemblyMergeStrategy := {
  case PathList("org", "slf4j", xs @ _*) => MergeStrategy.first
  case x                                 => (ThisBuild / assemblyMergeStrategy).value(x)
}