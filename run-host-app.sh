#!/bin/bash

# Script to run the host application
# This simulates the user's non-containerized application running on the host

echo "=== Starting Host Application ==="
echo "This application will listen on your LAN IP address"
echo "Containers will attempt to communicate with this application"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

echo "Building the project..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "Error: Failed to compile the project"
    exit 1
fi

echo "Starting the host application..."
echo "Press Ctrl+C to stop"
echo

# Run the host application
mvn exec:java -Dexec.mainClass="com.example.HostApplication" -q
