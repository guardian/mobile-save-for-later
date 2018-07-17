import sbtassembly.MergeStrategy
import Dependencies._
 import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName

import scala.collection.immutable

val testAndCompileDependencies: String = "test->test;compile->compile"

def projectMaker(projectName: String) = Project(projectName, file(projectName))
    .enablePlugins(RiffRaffArtifact)
    .settings( List (
      name := projectName,
      riffRaffManifestProjectName := s"Mobile::${name.value}"
    ) ++ commonAssemblySettings(projectName)
  )

def commonAssemblySettings(module: String): immutable.Seq[Def.Setting[_]]  = commonSettings(module) ++ List (
  assemblyMergeStrategy in assembly := {
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" => new MergeFilesStrategy
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  assemblyJarName := s"${name.value}.jar",
  riffRaffPackageType := assembly.value,
  riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
  riffRaffUploadManifestBucket := Option("riffraff-builds"),
  riffRaffArtifactResources += (file(s"${module}/conf/cfn.yaml"), s"${module}-cfn/cfn.yaml"),
)

def commonSettings(module: String): immutable.Seq[Def.Setting[_]] = {
  List(
    fork := true, // was hitting deadlock, fxxund similar complaints online, disabling concurrency helps: https://github.com/sbt/sbt/issues/3022, https://github.com/mockito/mockito/issues/1067
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms"
    ),
    libraryDependencies ++= Seq(
      awsLambda,
      awsDynamo,
      awsLambdaLog,
      awsJavaSdk,
      jackson,
      jacksonDataFormat,
      jacksonJdk8DataType,
      jacksonJsrDataType,
      log4j,
      commonsIo,
      scanamo,
      guSimpleConfigurationSsm,
      okHttp,
      specsCore,
      specsScalaCheck,
      specsMock
    ),
    dependencyOverrides ++= Seq(
      commonsLogging,
      slf4jApi,
      apacheLog4JCore,
      apacheLog$jApi
    ),
    organization := "com.gu",
    version := "1.0",
    scalaVersion := "2.12.5",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-target:jvm-1.8",
      "-Ypartial-unification",
      "-Ywarn-dead-code"
   )
  )
}

lazy val saveforlaterapp = projectMaker("mobile-save-for-later")

lazy val root = project.in(file(".")).aggregate(saveforlaterapp)

