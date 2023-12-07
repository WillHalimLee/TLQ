#!/bin/bash

# Correctly formatted JSON object to pass to Lambda Function
json='{"aggregations": ["SUM(\"Total Profit\") AS total_profit"], "filters": {"\"Order Priority\"": "High"}, "groupBy": ["Region"]}'

# Lambda function name
lambda_function_name="Service_Three"

# AWS Region where your Lambda function is deployed
aws_region="us-east-2" # replace with your region

# Temporary file to store the response
temp_file="lambda_output.txt"

# Invoking the Lambda function
echo "Invoking Lambda function using AWS CLI"
aws lambda invoke --invocation-type RequestResponse --function-name "$lambda_function_name" --region "$aws_region" --payload "$json" "$temp_file"

# Extract the JSON result from the AWS Lambda response and format it
echo "JSON RESULT:"
cat "$temp_file" | jq '.queryResults[]'
echo ""

# Optionally, delete the temporary file
rm "$temp_file"

