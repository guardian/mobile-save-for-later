import sbt._

object Dependencies {
  val awsSdkVersion = "1.11.412"
  val log4j2Version = "2.10.0"
  val jacksonVersion = "2.9.4"
  val specsVersion = "4.0.3"


  //Dependecies
  val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"
  val awsDynamo ="com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion
  val awsLambdaLog = "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0"
  val awsJavaSdk ="com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion
  val awsCloudwatch = "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion
  val awsSqs ="com.amazonaws" % "aws-java-sdk-sqs" % awsSdkVersion
  val awsSsm ="com.amazonaws" % "aws-java-sdk-ssm" % awsSdkVersion
  val awsLambdaEvent = "com.amazonaws" % "aws-lambda-java-events" % "2.2.2"

  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
  val jacksonDataFormat = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
  val jacksonJdk8DataType = "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion
  val jacksonJsrDataType = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion
  val log4j = "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version
  val commonsIo = "commons-io" % "commons-io" % "2.6"
  val scanamo = "com.gu" %% "scanamo" % "1.0.0-M6"
  val guSimpleConfigurationSsm = "com.gu" %% "simple-configuration-ssm" % "1.4.3"
  val okHttp = "com.squareup.okhttp3" % "okhttp" % "3.10.0"
  val specsCore = "org.specs2" %% "specs2-core" % specsVersion % "test"
  val specsScalaCheck = "org.specs2" %% "specs2-scalacheck" % specsVersion % "test"
  val specsMock = "org.specs2" %% "specs2-mock" % specsVersion % "test"

  //DependencyOverride
  val commonsLogging = "commons-logging" % "commons-logging" % "1.2"
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
  val apacheLog4JCore = "org.apache.logging.log4j" % "log4j-core" % log4j2Version
  val apacheLog$jApi = "org.apache.logging.log4j" % "log4j-api" % log4j2Version % "provided"

}
