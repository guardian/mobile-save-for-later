import "source-map-support/register";
import { App } from "aws-cdk-lib";
import type { MobileSaveForLaterProps } from "../lib/mobile-save-for-later";
import { MobileSaveForLater } from "../lib/mobile-save-for-later";

const app = new App();

export const codeProps: MobileSaveForLaterProps = {
  stack: "mobile",
  stage: "CODE",
  certificateId: "0ee21f37-ec53-437c-b572-3c9d294ab749",
  domainName: "mobile-save-for-later.mobile-aws.code.dev-guardianapis.com",
  hostedZoneName: "mobile-aws.code.dev-guardianapis.com",
  hostedZoneId: "Z6PRU8YR6TQDK",
  identityApiHost: "https://id.code.dev-guardianapis.com",
  reservedConcurrentExecutions: 1,
};

export const prodProps: MobileSaveForLaterProps = {
  stack: "mobile",
  stage: "PROD",
  certificateId: "b4c2902a-fc80-47a9-88b7-7810b88e7e26",
  domainName: "mobile-save-for-later.mobile-aws.guardianapis.com",
  hostedZoneName: "mobile-aws.guardianapis.com",
  hostedZoneId: "Z1EYB4AREPXE3B",
  identityApiHost: "https://id.guardianapis.com",
  reservedConcurrentExecutions: 300,
};

new MobileSaveForLater(app, "MobileSaveForLater-CODE", codeProps);
new MobileSaveForLater(app, "MobileSaveForLater-PROD", prodProps);
