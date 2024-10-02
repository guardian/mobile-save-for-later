import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName
import sbtassembly.MergeStrategy

import scala.collection.immutable

val testAndCompileDependencies: String = "test->test;compile->compile"

ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always // due to Identity using lift-json

def projectMaker(projectName: String) = Project(projectName, file(projectName))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    List(
      name := projectName,
      riffRaffManifestProjectName := s"Mobile::${name.value}"
    ) ++ commonAssemblySettings(projectName)
  )
  .dependsOn(common % "compile->compile")
  .aggregate(common)

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
def commonAssemblySettings(module: String): immutable.Seq[Def.Setting[_]] =
  commonSettings ++ List(
    assemblyJarName := s"${name.value}.jar",
    riffRaffPackageType := assembly.value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds")
  )

val commonSettings: immutable.Seq[Def.Setting[_]] = List(
  fork := true, // was hitting deadlock, fxxund similar complaints online, disabling concurrency helps: https://github.com/sbt/sbt/issues/3022, https://github.com/mockito/mockito/issues/1067
  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  libraryDependencies ++= Seq(
    awsLambda,
    awsDynamo,
    awsLambdaLog,
    awsJavaSdk,
    jackson,
    jacksonDataFormat,
    jacksonJsrDataType,
    commonsIo,
    scanamo,
    okHttp,
    slf4jSimple,
    identityAuthCore,
    specsCore,
    specsScalaCheck,
    specsMock
  ),
  ThisBuild / assemblyMergeStrategy := {
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last equalsIgnoreCase "Log4j2Plugins.dat" => sbtassembly.Log4j2MergeStrategy.plugincache
    case _ => MergeStrategy.first
  },
  organization := "com.gu",
  version := "1.0",
  scalaVersion := "2.13.15",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-release:21",
    "-Ywarn-dead-code"
  )
)

lazy val common = project
  .in(file("common"))
  .settings(commonSettings: _*)

lazy val saveforlaterapp = projectMaker("mobile-save-for-later").settings(
  riffRaffArtifactResources += (file(
    "cdk/cdk.out/MobileSaveForLater-CODE.template.json"
  ), "mobile-save-for-later-cfn/MobileSaveForLater-CODE.template.json"),
  riffRaffArtifactResources += (file(
    "cdk/cdk.out/MobileSaveForLater-PROD.template.json"
  ), "mobile-save-for-later-cfn/MobileSaveForLater-PROD.template.json")
)

lazy val userDeletion =
  projectMaker("mobile-save-for-later-user-deletion").settings(
    libraryDependencies ++= Seq(
      awsLambdaEvent,
      awsSqs
    ),
    riffRaffArtifactResources += (file(
      "mobile-save-for-later-user-deletion/conf/cfn.yaml"
    ), "mobile-save-for-later-user-deletion-cfn/cfn.yaml")
  )

lazy val root = project.in(file(".")).aggregate(saveforlaterapp)
