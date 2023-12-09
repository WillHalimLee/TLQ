#!/bin/bash

# JSON input for invoking the Lambda function with SQL query
json_input='{"action": "query", "aggregations": ["SUM(\"Total Profit\") AS total_profit"], "filters": {"\"Order Priority\"": "High"}, "groupBy": ["Region"]}'

# Lambda function name
lambda_function_name="TLQ"  # Replace with the name of your Lambda function

# AWS Region where your Lambda function is deployed
aws_region="us-east-2"  # Replace with your region

# Temporary file to store the response
temp_file="lambda_output.txt"

# Invoking the Lambda function
echo "Invoking Lambda function using AWS CLI"
aws lambda invoke --function-name "$lambda_function_name" --region "$aws_region" --payload "$json_input" "$temp_file"

# Check if jq is installed for parsing JSON
if ! command -v jq &> /dev/null; then
    echo "jq is not installed. Please install it to parse JSON output."
    exit 1
fi

# Extract and display the JSON result from the AWS Lambda response
echo "JSON Result:"
jq '.' < "$temp_file"

echo ""

# Optionally, delete the temporary file
rm "$temp_file"

