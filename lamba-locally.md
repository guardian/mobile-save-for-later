### Running a lambda locally (WIP)



1. Get and modify the cloudformation definition
- Get a definition of the lambda from by clicking `export function` and choosing `Download AWS SAM file` on the lambda aws console
- Copy this and rename to `template.yaml` and put in the root of the project
- Delete the lines that say:
  `RuntimePolicy:
      UpdateRuntimeOn: Auto`
- Update the param `CodeUri` with the path to the jar that is outputted as a result of running riffRaffPackageType

_(Repeat steps 2 & 3 everytime you make local code changes)_

2. Build a jar of the project by running:
- `sbt "project mobile-save-for-later" riffRaffPackageType`

3. Run the lambda service (--debug not needed for printlns)
- sam local start-lambda 


4. Execute a function in a new terminal window:
- Create a payload.json file with your Request data
- 
  aws lambda invoke --function-name "mobilesaveforlaterFETCHcdkCODE" \
  --endpoint-url "http://127.0.0.1:3001" \
  --cli-binary-format raw-in-base64-out \
  --payload file://payload.json \
  output.txt \
  --no-verify-ssl --profile mobile --region eu-west-1

docs: https://awscli.amazonaws.com/v2/documentation/api/latest/reference