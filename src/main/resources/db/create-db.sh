#!/bin/bash

# Check if DEMO_DB environment variable is set
if [ -z "$DEMO_DB" ]; then
    echo "Error: DEMO_DB environment variable is not set"
    echo "Usage: DEMO_DB=your_database_name ./create-db.sh"
    exit 1
fi

# Create the role
echo "Creating role ${DEMO_DB}..."
psql -c "CREATE ROLE ${DEMO_DB} LOGIN NOCREATEDB NOCREATEROLE NOREPLICATION NOSUPERUSER ENCRYPTED PASSWORD '${DEMO_DB}';"

# Create the database
echo "Creating database ${DEMO_DB}..."
psql -c "CREATE DATABASE ${DEMO_DB} WITH OWNER ${DEMO_DB} ENCODING='UTF8';"

echo "Database ${DEMO_DB} created successfully!" 