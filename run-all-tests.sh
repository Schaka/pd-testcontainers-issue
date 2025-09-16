#!/bin/bash

# Script to run all tests for comprehensive testing

echo "=== Running All Testcontainers Tests ==="
echo "This will run all test methods to provide comprehensive diagnostics"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

echo "Configuring Testcontainers to use Podman (rootful socket)..."
export DOCKER_HOST="unix:///run/podman/podman.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/run/podman/podman.sock"
export TESTCONTAINERS_RYUK_DISABLED=true

echo "Using DOCKER_HOST: $DOCKER_HOST (rootful socket)"
echo

echo "Building the project..."
mvn compile test-compile -q

if [ $? -ne 0 ]; then
    echo "Error: Failed to compile the project"
    exit 1
fi

echo "Running all tests..."
echo

# Run all tests in the test class
DOCKER_HOST="$DOCKER_HOST" TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE" TESTCONTAINERS_RYUK_DISABLED="$TESTCONTAINERS_RYUK_DISABLED" mvn test -Dtest=PodmanDesktopNetworkingTest

echo
echo "=== All tests completed ==="
echo "Review the output above to understand the current networking behavior."
