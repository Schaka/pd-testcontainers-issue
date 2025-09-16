#!/bin/bash

# Script to run the test when Podman Desktop is OPEN
# This should demonstrate the failing scenario reported by the user

echo "=== Testing with Podman Desktop OPEN ==="
echo
echo "IMPORTANT: Make sure Podman Desktop is running before running this test!"
echo "You can start Podman Desktop from your applications menu or with: flatpak run io.podman_desktop.PodmanDesktop"
echo "Press Enter to continue, or Ctrl+C to cancel..."
read

# Check if Podman Desktop is running
if ! pgrep -f "podman-desktop" > /dev/null; then
    echo "WARNING: Podman Desktop does not appear to be running!"
    echo "Please start Podman Desktop and try again."
    echo "Start command: flatpak run io.podman_desktop.PodmanDesktop &"
    exit 1
fi

echo "✅ Podman Desktop appears to be running"
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

echo "Running the Testcontainers test..."
echo "According to the bug report, this test should FAIL when Podman Desktop is open"
echo

# Run the specific test that checks for the issue
DOCKER_HOST="$DOCKER_HOST" TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE" TESTCONTAINERS_RYUK_DISABLED="$TESTCONTAINERS_RYUK_DISABLED" mvn test -Dtest=PodmanDesktopNetworkingTest#testPodmanDesktopInterference

echo
echo "=== Test completed ==="
echo "If the test failed, this confirms the reported networking issue with Podman Desktop."
echo "Compare this result with the test when Podman Desktop is closed."
