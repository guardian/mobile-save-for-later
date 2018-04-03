import sbtassembly.MergeStrategy

//enablePlugins(RiffRaffArtifact)

val awsVersion = "1.2.0"

def commonSettings(module: String) = List(
  name := s"Mobile::${module}",
  organization := "com.gu",
  version := "1.0",
  scalaVersion := "2.12.4",
  description:= "lambda to replace the content-notifications-service",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Ywarn-dead-code"
  ),
  assemblyMergeStrategy in assembly := {
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms"
  ),
  assemblyJarName := s"${name.value}.jar",
  riffRaffPackageType := assembly.value,
  riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
  riffRaffUploadManifestBucket := Option("riffraff-builds"),
  riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")
)

lazy val saveForLater = project.settings(commonSettings("save-for-later")).settings(
  libraryDependencies ++= )



