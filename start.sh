#!/bin/bash

# Verify if AWS region is set
if [ -z "$AWS_DEFAULT_REGION" ]; then
    echo "Error: AWS_DEFAULT_REGION environment variable is not set"
    exit 1
fi

# Function to get parameter from Parameter Store
get_parameter() {
    local param_name=$1
    aws ssm get-parameter \
        --name "$param_name" \
        --with-decryption \
        --query 'Parameter.Value' \
        --output text
}

# Lista de parâmetros específicos da sua aplicação
PARAMS=(
    "ASSETS_URL_SCRAPING"
    "JWT_SECRET"
    "MAIL_HOST"
    "MAIL_PASS"
    "MAIL_PORT"
    "MAIL_USERNAME"
    "MONGO_ROOT_PASSWORD"
    "MONGO_ROOT_USERNAME"
    "YIELD_URL_SCRAPING"
)

# Create .env file
echo "# Environment variables from AWS Parameter Store" > /app/.env

# Fetch parameters and add to .env file
for param_name in "${PARAMS[@]}"; do
    # Get value from Parameter Store
    param_value=$(get_parameter "$param_name")
    # Add to .env file
    echo "$param_name=$param_value" >> /app/.env
    echo "Loaded parameter: $param_name"
done

# Start the Java application
exec java -jar app.jar