# Podman Desktop Testcontainers Network Issue Reproduction

This project reproduces the networking issue reported in Podman Desktop where containers using Testcontainers cannot communicate with the host via LAN IP when Podman Desktop is open.

## Issue Summary

**Reported Problem**: When Podman Desktop is open, Testcontainers-managed containers cannot communicate with applications running on the host using the host's LAN IPv4 address (e.g., 192.168.1.XXX). This works perfectly when Podman Desktop is closed.

**User's Environment**:
- OS: Fedora 42
- Podman Desktop: 1.19.2 (via Flatpak)
- Podman: Via Fedora repositories
- Setup: Rootful socket with Docker compatibility
- Container: Chrome with SYS_ADMIN capabilities and `withAccessToHost(true)`

## Prerequisites

Ensure you have the following installed:
- Java 11 or higher
- Maven 3.6+
- Podman (via Fedora repos)
- Podman Desktop (via Flatpak)
- podman-docker package enabled

### Verify Prerequisites

```bash
# Check Java version
java -version

# Check Maven
mvn -version

# Check Podman
podman --version

# Check Podman Desktop installation
flatpak list | grep podman

# Verify Docker compatibility is enabled
podman system info | grep -A5 "Docker compatibility"
```

### Configure Podman for Testcontainers (Rootful Socket)

The user's bug report mentions using the **rootful socket** with Docker compatibility. Configure this setup:

```bash
# Start and enable the system-wide (rootful) Podman socket service
sudo systemctl start podman.socket
sudo systemctl enable podman.socket

# Verify the rootful socket is running
sudo systemctl status podman.socket

# Check that the rootful socket file exists
sudo ls -la /run/podman/podman.sock

# Ensure your user is in the podman group for socket access
groups $USER | grep podman

# Test connection to rootful socket
DOCKER_HOST="unix:///run/podman/podman.sock" docker version
```

**Note**: The test scripts automatically configure the required environment variables to use the rootful socket (`DOCKER_HOST=unix:///run/podman/podman.sock`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`, and `TESTCONTAINERS_RYUK_DISABLED`).

## Project Structure

```
Dummy Testcontainers V2/
├── pom.xml                           # Maven configuration with Testcontainers dependencies
├── src/
│   ├── main/java/com/example/
│   │   └── HostApplication.java      # Host application that listens on LAN IP
│   └── test/java/com/example/
│       └── PodmanDesktopNetworkingTest.java  # Test that reproduces the issue
├── run-host-app.sh                   # Script to start host application
├── run-test-podman-closed.sh         # Test with Podman Desktop closed
├── run-test-podman-open.sh           # Test with Podman Desktop open
├── run-all-tests.sh                  # Run all diagnostic tests
└── REPRODUCTION_STEPS.md             # This file
```

## Reproduction Steps

### Step 1: Build the Project

```bash
cd "/path/to/project/Dummy Testcontainers V2"
mvn clean compile test-compile
```

### Step 2: Test with Podman Desktop CLOSED

1. **Ensure Podman Desktop is completely closed**:
   ```bash
   # Check if Podman Desktop is running
   pgrep -f "podman-desktop"
   
   # If running, close it completely
   pkill -f "podman-desktop"
   ```

2. **Run the test**:
   ```bash
   ./run-test-podman-closed.sh
   ```
   
   **Expected Result**: Test should PASS, indicating that container-to-host communication works when Podman Desktop is closed.

### Step 3: Test with Podman Desktop OPEN

1. **Start Podman Desktop**:
   ```bash
   flatpak run io.podman_desktop.PodmanDesktop &
   ```
   
   Wait for Podman Desktop to fully load (you should see the GUI).

2. **Run the test**:
   ```bash
   ./run-test-podman-open.sh
   ```
   
   **Expected Result**: According to the bug report, this test should FAIL when Podman Desktop is open, demonstrating the networking issue.

### Step 4: Compare Results

Compare the outputs from Step 2 and Step 3. If the issue is reproduced:
- Test with Podman Desktop closed: ✅ PASS
- Test with Podman Desktop open: ❌ FAIL

## Alternative Testing Methods

### Manual Testing with Host Application

1. **Start the host application** (in one terminal):
   ```bash
   ./run-host-app.sh
   ```
   
   This will show you the detected LAN IP and start an HTTP server.

2. **Run comprehensive tests** (in another terminal):
   ```bash
   ./run-all-tests.sh
   ```

### Individual Test Methods

You can run specific test methods:

```bash
# Test basic connectivity and HTTP communication
mvn test -Dtest=PodmanDesktopNetworkingTest#testContainerToHostCommunicationViaLanIp

# Run network diagnostics
mvn test -Dtest=PodmanDesktopNetworkingTest#runNetworkDiagnostics

# Test specifically for Podman Desktop interference
mvn test -Dtest=PodmanDesktopNetworkingTest#testPodmanDesktopInterference
```

## What the Test Does

1. **Host Application**: Starts an HTTP server on the detected LAN IP (e.g., 192.168.1.XXX)
2. **Chrome Container**: Launches with:
   - Privileged mode (equivalent to SYS_ADMIN capability)
   - `withAccessToHost(true)` (key setting from user's setup)
   - Network connectivity to host
3. **Network Tests**:
   - Ping test to LAN IP
   - HTTP requests to host application
   - Multiple endpoint testing
   - Network diagnostics

## Expected Behavior

### When Working (Podman Desktop Closed)
```
✅ Ping successful - network connectivity is working
✅ HTTP communication successful
Container can reach: http://192.168.1.XXX:8080
```

### When Broken (Podman Desktop Open)
```
❌ Ping failed - this might indicate the reported issue
❌ HTTP communication failed - this reproduces the reported issue
Connection timeouts or "no route to host" errors
```

## Troubleshooting

### If Tests Don't Reproduce the Issue

1. **Verify Podman Rootful Socket Configuration**:
   ```bash
   # Check rootful socket status
   sudo systemctl status podman.socket
   
   # Verify rootful socket file exists and has correct permissions
   sudo ls -la /run/podman/podman.sock
   
   # Check user group membership
   groups $USER | grep podman
   
   # Test connection to rootful Podman socket
   DOCKER_HOST="unix:///run/podman/podman.sock" docker version
   ```

2. **Check Network Configuration**:
   ```bash
   ip route show
   podman network ls
   ```

3. **Verify Testcontainers Configuration**:
   ```bash
   # Check if DOCKER_HOST is set correctly
   echo $DOCKER_HOST
   
   # Should point to rootful Podman socket:
   # unix:///run/podman/podman.sock
   ```

### Common Issues

1. **No LAN IP Detected**: The host application will fall back to localhost (127.0.0.1)
2. **Container Startup Timeout**: Increase timeout or check Podman service
3. **Permission Issues**: Ensure user has access to Podman socket

## Sharing Results

When sharing results with the Podman Desktop team:

1. **Include the full test output** from both scenarios (closed/open)
2. **Provide system information**:
   ```bash
   # System info
   cat /etc/os-release
   podman --version
   flatpak info io.podman_desktop.PodmanDesktop
   
   # Network configuration
   ip addr show
   ip route show
   ```
3. **Include any error messages** from the test output

## Notes

- This reproduction setup closely matches the user's reported configuration
- The test is designed to be non-destructive and can be run multiple times
- All scripts include safety checks and clear instructions
- The host application automatically detects your LAN IP address
