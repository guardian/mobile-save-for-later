import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { codeProps, prodProps } from "../bin/cdk";
import { MobileSaveForLater } from "./mobile-save-for-later";

describe("The MobileSaveForLater stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const codeStack = new MobileSaveForLater(
      app,
      "MobileSaveForLater-CODE",
      codeProps
    );
    const prodStack = new MobileSaveForLater(
      app,
      "MobileSaveForLater-PROD",
      prodProps
    );
    expect(Template.fromStack(codeStack).toJSON()).toMatchSnapshot();
    expect(Template.fromStack(prodStack).toJSON()).toMatchSnapshot();
  });
});
