import sbtassembly.MergeStrategy

val awsSdkVersion = "1.11.307"

name := "mobile-save-for-later"

organization := "com.gu"

version := "10"

scalaVersion := "2.12.5"

description:= "lambdas that implement save for later"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.307",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.307",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.4",
  "com.amazonaws" % "aws-java-sdk-ssm" % "1.11.307",
  "com.amazonaws" % "aws-java-sdk-ec2" % "1.11.307",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.4",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "commons-io" % "commons-io" % "2.6",
  "com.gu" %% "scanamo" % "1.0.0-M6",
  "com.gu.identity.api" %% "identity-api-client-lib" % "3.141" excludeAll(ExclusionRule("com.google.collections", "google-collections"), ExclusionRule("org.slf4j" , "slf4j-simple")),
  "com.gu" %% "simple-configuration-ssm" % "1.4.3",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.mockito" % "mockito-all" % "1.9.0" % "test",
  "org.specs2" %% "specs2-core" % "4.0.2" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "4.0.2" % "test"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms"
)

assemblyMergeStrategy in assembly := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

