#!/bin/bash

# Lambda function name
lambda_function_name="TLQ"

# AWS Region where your Lambda function is deployed
aws_region="us-east-2" # replace with your region, e.g., us-east-1

# Temporary file to store the response
temp_file="lambda_output.txt"

# JSON input for invoking the Lambda function
json_input='{"action":"transform"}'

# Create a temporary file for the JSON input
json_input_file="input.json"
echo $json_input > $json_input_file

# Invoking the Lambda function
echo "Invoking Lambda function: $lambda_function_name"
aws lambda invoke --function-name "$lambda_function_name" --region "$aws_region" --payload file://$json_input_file "$temp_file"

# Output the response
echo "Lambda Response:"
cat "$temp_file"
echo ""

# Clean up: Optionally delete the temporary files
rm "$temp_file"
rm "$json_input_file"

