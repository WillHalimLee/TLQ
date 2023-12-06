#!/bin/bash
# JSON object to pass to Lambda Function
json={"\"bucketname\"":\"test.bucket.462.termproject\"","\"filename\"":\"output.csv\""}
echo "Invoking Lambda function using AWS CLI"
time output=`aws lambda invoke --invocation-type RequestResponse --function-name Service_Two --region us-east-2 --payload $json /dev/stdout | head -n 1 | head -c -2 ; echo`
echo ""
echo "JSON RESULT:"
echo $output | jq
