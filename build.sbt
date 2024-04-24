import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName
import sbtassembly.MergeStrategy
import com.gu.riffraff.artifact.BuildInfo

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
    awsLambdaLog,
    awsJavaSdk,
    jackson,
    jacksonDataFormat,
    jacksonJsrDataType,
    log4j,
    commonsIo,
    scanamo,
    okHttp,
    identityAuthCore,
    specsCore,
    specsScalaCheck,
    specsMock,
    "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
    "org.slf4j" % "log4j-over-slf4j" % "2.0.13", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
    "ch.qos.logback" % "logback-classic" % "1.5.6",
    "com.lihaoyi" %% "upickle" % "3.3.0",
  ),
  ThisBuild / assemblyMergeStrategy := {
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last equalsIgnoreCase "Log4j2Plugins.dat" => sbtassembly.Log4j2MergeStrategy.plugincache
    case _ => MergeStrategy.first
  },
  organization := "com.gu",
  version := "1.0",
  scalaVersion := "2.12.19",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-Ypartial-unification",
    "-Ywarn-dead-code"
  )
)

lazy val common = project
  .in(file("common"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    buildInfoPackage := "com.gu.s4l",
      buildInfoKeys := {
      lazy val buildInfo = BuildInfo(baseDirectory.value)
      Seq[BuildInfoKey](
        "buildNumber" -> buildInfo.buildIdentifier,
        "gitCommitId" -> buildInfo.revision,
        "buildTime" -> System.currentTimeMillis
      )
    }
  )


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
