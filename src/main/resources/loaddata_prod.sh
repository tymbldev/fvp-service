#!/bin/bash

# Database credentials
DB_USER="root"
DB_PASS="Info@feb16"
DB_NAME="fvp_prod"
DB_HOST="localhost"
DB_PORT="3306"

# Path to schema file
SCHEMA_FILE="schema.sql"

echo "Executing database schema for PRODUCTION environment..."

# Execute the schema file
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < $SCHEMA_FILE

# Check if the command was successful
if [ $? -eq 0 ]; then
    echo "Schema executed successfully in PRODUCTION!"
else
    echo "Error executing schema in PRODUCTION. Please check the error message above."
    exit 1
fi 