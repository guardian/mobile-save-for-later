import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { MobileSaveForLater } from "./mobile-save-for-later";

describe("The MobileSaveForLater stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new MobileSaveForLater(app, "MobileSaveForLater", {
      stack: "mobile",
      stage: "TEST",
    });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
