#!/bin/bash

# Check if DEMO_DB environment variable is set
if [ -z "$DEMO_DB" ]; then
    echo "Error: DEMO_DB environment variable is not set"
    echo "Usage: DEMO_DB=your_database_name ./drop-db.sh"
    exit 1
fi

# Drop the database
echo "Dropping database ${DEMO_DB}..."
psql -c "DROP DATABASE ${DEMO_DB};"

# Drop the role
echo "Dropping role ${DEMO_DB}..."
psql -c "DROP ROLE ${DEMO_DB};"

echo "Database ${DEMO_DB} dropped successfully!" 