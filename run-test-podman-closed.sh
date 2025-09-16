#!/bin/bash

# Script to run the test when Podman Desktop is CLOSED
# This should demonstrate the working scenario

echo "=== Testing with Podman Desktop CLOSED ==="
echo
echo "IMPORTANT: Make sure Podman Desktop is completely closed before running this test!"
echo "Press Enter to continue, or Ctrl+C to cancel..."
read

# Check if Podman Desktop is running
if pgrep -f "podman-desktop" > /dev/null; then
    echo "WARNING: Podman Desktop appears to be running!"
    echo "Please close Podman Desktop and try again."
    echo "You can check running processes with: ps aux | grep podman-desktop"
    exit 1
fi

echo "✅ Podman Desktop appears to be closed"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

echo "Configuring Testcontainers to use Podman (rootless socket)..."
export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="$XDG_RUNTIME_DIR/podman/podman.sock"
export TESTCONTAINERS_RYUK_DISABLED=true

echo "Using DOCKER_HOST: $DOCKER_HOST (rootless socket)"
echo

echo "Running the Testcontainers test..."
echo "This test should PASS when Podman Desktop is closed"
echo

# Run the specific test that checks for the issue
DOCKER_HOST="$DOCKER_HOST" TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE" TESTCONTAINERS_RYUK_DISABLED="$TESTCONTAINERS_RYUK_DISABLED" mvn test -Dtest=PodmanDesktopNetworkingTest#testPodmanDesktopInterference

echo
echo "=== Test completed ==="
echo "If the test passed, the networking is working correctly with Podman Desktop closed."
