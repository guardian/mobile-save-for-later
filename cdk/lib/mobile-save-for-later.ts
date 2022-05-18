import { join } from "path";
import { GuApiGatewayWithLambdaByPath } from "@guardian/cdk";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import { GuLambdaFunction } from "@guardian/cdk/lib/constructs/lambda";
import type { App } from "aws-cdk-lib";
import { Duration } from "aws-cdk-lib";
import { PolicyStatement } from "aws-cdk-lib/aws-iam";
import { Runtime } from "aws-cdk-lib/aws-lambda";
import { CfnInclude } from "aws-cdk-lib/cloudformation-include";

export interface MobileSaveForLaterProps extends GuStackProps {
  certificateId: string;
  domainName: string;
  hostedZoneName: string;
  hostedZoneId: string;
  identityApiHost: string;
}

export class MobileSaveForLater extends GuStack {
  constructor(scope: App, id: string, props: MobileSaveForLaterProps) {
    super(scope, id, props);

    const yamlTemplateFilePath = join(
      __dirname,
      "../..",
      "mobile-save-for-later/conf/cfn.yaml"
    );
    const yamlDefinedResources = new CfnInclude(this, "YamlTemplate", {
      templateFile: yamlTemplateFilePath,
    });

    const app = "mobile-save-for-later";

    const commonLambdaProps = {
      runtime: Runtime.JAVA_8, // We should upgrade to Java 11 in a future PR
      app,
      fileName: `${app}.jar`,
      // N.B. these lambdas previously used very specific, low memory sizes (384 & 394); odd choices for a Scala Lambda
    };

    const commonEnvironmentVariables = {
      App: app,
      Stack: this.stack,
      Stage: this.stage,
      IdentityApiHost: props.identityApiHost,
    };

    const saveArticlesLambda = new GuLambdaFunction(
      this,
      "save-articles-lambda",
      {
        handler: "com.gu.sfl.lambda.SaveArticlesLambda::handleRequest",
        functionName: `mobile-save-for-later-SAVE-cdk-${this.stage}`,
        environment: {
          ...commonEnvironmentVariables,
          SavedArticleLimit: "1000",
        },
        ...commonLambdaProps,
      }
    );

    const fetchArticlesLambda = new GuLambdaFunction(
      this,
      "fetch-articles-lambda",
      {
        handler: "com.gu.sfl.lambda.FetchArticlesLambda::handleRequest",
        functionName: `mobile-save-for-later-FETCH-cdk-${this.stage}`,
        timeout: Duration.seconds(20),
        reservedConcurrentExecutions: this.stage === "PROD" ? 50 : 1,
        environment: commonEnvironmentVariables,
        ...commonLambdaProps,
      }
    );

    [saveArticlesLambda, fetchArticlesLambda].map((lambda) => {
      // permissions for the departmental standard are provided by cdk 'for free' but we cannot
      // use them here because our parameter path used differs from the departmental standard
      lambda.addToRolePolicy(
        new PolicyStatement({
          actions: ["ssm:GetParametersByPath"],
          resources: [
            `arn:aws:ssm:${this.region}:${this.account}:parameter/${app}/${this.stage}/${this.stack}`,
          ],
        })
      );
      lambda.addToRolePolicy(
        new PolicyStatement({
          actions: ["cloudwatch:putMetricData"],
          resources: ["*"],
        })
      );
      lambda.addToRolePolicy(
        new PolicyStatement({
          actions: [
            "dynamodb:GetItem",
            "dynamodb:PutItem",
            "dynamodb:UpdateItem",
            "dynamodb:Query",
          ],
          resources: [
            yamlDefinedResources
              .getResource("SaveForLaterDynamoTable")
              // https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-dynamodb-table.html#aws-resource-dynamodb-table-return-values
              .getAtt("Arn")
              .toString(),
          ],
        })
      );
    });

    const saveForLaterApi = new GuApiGatewayWithLambdaByPath(this, {
      app,
      restApiName: `${app}-api-${this.stage}`,
      monitoringConfiguration: { noMonitoring: true }, //TODO: configure monitoring (this is not setup in current CFN)
      targets: [
        {
          path: "/syncedPrefs/me/savedArticles",
          httpMethod: "POST",
          lambda: saveArticlesLambda,
        },
        {
          path: "/syncedPrefs/me",
          httpMethod: "GET",
          lambda: fetchArticlesLambda,
        },
      ],
    });

    // TODO:
    // Move into cdk: DNS configuration
    // Decide whether to port across or leave in CFN: Dynamo Table & Dynamo Throttle CloudWatch Alarms
  }
}
