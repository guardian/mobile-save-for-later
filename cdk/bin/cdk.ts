import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { MobileSaveForLater } from "../lib/mobile-save-for-later";

const app = new App();
new MobileSaveForLater(app, "MobileSaveForLater-CODE", {
  stack: "mobile",
  stage: "CODE",
});
new MobileSaveForLater(app, "MobileSaveForLater-PROD", {
  stack: "mobile",
  stage: "PROD",
});
