import codacy.libs._
import sbt._

Universal / javaOptions ++= Seq("-Xms1g", "-Xmx2g", "-Xss512m", "-XX:+UseG1GC", "-XX:+UseStringDeduplication")

val assemblyCommon = Seq(
  test in assembly := {},
  // Without this assembly merge strategy, gives the following error:
  // (codacyAnalysisCli / assembly) deduplicate: different file contents found in the following:
  // [error] org/bouncycastle/bcpg-jdk15on/1.64/bcpg-jdk15on-1.64.jar:META-INF/versions/9/module-info.class
  // Workaround:
  // https://stackoverflow.com/questions/54834125/sbt-assembly-deduplicate-module-info-class
  assemblyMergeStrategy in assembly := {
    case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  })

val sonatypeInformation = Seq(
  startYear := Some(2018),
  homepage := Some(url("https://github.com/codacy/codacy-analysis-cli")),
  // HACK: This setting is not picked up properly from the plugin
  pgpPassphrase := Option(System.getenv("SONATYPE_GPG_PASSPHRASE")).map(_.toCharArray),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/codacy/codacy-analysis-cli"),
      "scm:git:git@github.com:codacy/codacy-analysis-cli.git"))) ++ publicMvnPublish

lazy val codacyAnalysisCore = project
  .in(file("core"))
  .settings(name := "codacy-analysis-core")
  .settings(coverageExcludedPackages := "<empty>;com\\.codacy\\..*Error.*")
  .settings(Common.genericSettings)
  .settings(
    // App Dependencies
    libraryDependencies ++= Seq(
      Dependencies.caseApp,
      betterFiles,
      Dependencies.jodaTime,
      Dependencies.scalajHttp,
      Dependencies.jGit,
      Dependencies.cats,
      Dependencies.typesafeConfig) ++
      Dependencies.circe ++
      Dependencies.log4s ++
      Dependencies.codacyPlugins,
    // Test Dependencies
    libraryDependencies ++= Dependencies.specs2,
    sonatypeInformation,
    description := "Library to analyze projects")
  .settings(assemblyCommon: _*)
  // Disable legacy Scalafmt plugin imported by codacy-sbt-plugin
  .disablePlugins(com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin)
  .dependsOn(codacyAnalysisModels)

lazy val codacyAnalysisCli = project
  .in(file("cli"))
  .settings(
    name := "codacy-analysis-cli",
    coverageExcludedPackages := "<empty>;com\\.codacy\\..*CLIError.*",
    Common.dockerSettings,
    Common.genericSettings,
    Universal / javaOptions ++= Seq("-XX:MinRAMPercentage=60.0", "-XX:MaxRAMPercentage=90.0"),
    publish := (Docker / publish).value,
    publishLocal := (Docker / publishLocal).value,
    publishArtifact := false,
    libraryDependencies ++= Dependencies.pprint +: Dependencies.specs2)
  .settings(assemblyCommon: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  // Disable legacy Scalafmt plugin imported by codacy-sbt-plugin
  .disablePlugins(com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin)
  .dependsOn(codacyAnalysisCore % "compile->compile;test->test")
  .aggregate(codacyAnalysisCore)

lazy val codacyAnalysisModels = project
  .in(file("model"))
  .settings(
    crossScalaVersions := Common.supportedScalaVersions,
    name := "codacy-analysis-cli-model",
    Common.genericSettings,
    publishTo := sonatypePublishToBundle.value,
    libraryDependencies ++=
      Dependencies.circe ++ Seq(Dependencies.pluginsApi) ++ Dependencies.specs2,
    description := "Library with analysis models")
  .settings(assemblyCommon: _*)
  .settings(sonatypeInformation)
  // Disable legacy Scalafmt plugin imported by codacy-sbt-plugin
  .disablePlugins(com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin)
  .enablePlugins(JavaAppPackaging)

// Scapegoat
ThisBuild / scalaVersion := Common.scalaVersionNumber
ThisBuild / scalaBinaryVersion := Common.scalaBinaryVersionNumber
ThisBuild / scapegoatDisabledInspections := Seq()
ThisBuild / scapegoatVersion := "1.4.3"
