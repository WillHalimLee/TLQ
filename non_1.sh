#!/bin/bash

# Lambda function name
lambda_function_name="Service_One"

# AWS Region where your Lambda function is deployed
aws_region="us-east-2" # replace with your region, e.g., us-east-1

# Temporary file to store the response
temp_file="lambda_output.txt"

# Invoking the Lambda function
echo "Invoking Lambda function: $lambda_function_name"
aws lambda invoke --function-name "$lambda_function_name" --region "$aws_region" "$temp_file"

# Output the response
echo "Lambda Response:"
cat "$temp_file"
echo ""

# Optionally, delete the temporary file
rm "$temp_file"
