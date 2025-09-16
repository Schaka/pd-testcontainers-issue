# Testcontainers + Podman Rootful Socket Network Issue

> **Note**: This document contains anonymized network information. Actual IP addresses and system details have been replaced with generic examples for privacy.

## Issue Summary

While attempting to reproduce a reported Podman Desktop networking bug, we discovered a **separate but related networking issue** with Testcontainers and rootful Podman socket configuration.

## Environment

- **OS**: Fedora 42
- **Podman**: 5.6.1 (via Fedora repos)
- **Podman Desktop**: Running via Flatpak
- **Testcontainers**: 1.19.8
- **Java**: 11
- **Socket Configuration**: Rootful (`unix:///run/podman/podman.sock`)

## Observed Behavior

### ✅ What Works
- Testcontainers successfully connects to rootful Podman socket
- Containers start successfully (Chrome container starts in ~0.45s)
- Basic container operations function correctly

### ❌ What Fails
- **Container-to-host LAN IP communication fails completely**
- HTTP requests from container to host application timeout after 10 seconds
- This occurs **consistently regardless** of Podman Desktop being open or closed

## Test Results

| Scenario | Container Start | HTTP Communication | Result |
|----------|----------------|-------------------|---------|
| Podman Desktop CLOSED | ✅ SUCCESS (0.47s) | ❌ TIMEOUT (10s) | FAILED |
| Podman Desktop OPEN | ✅ SUCCESS (0.45s) | ❌ TIMEOUT (10s) | FAILED |

## Technical Details

### Host Application
- Runs on detected LAN IP: `10.88.0.1:8080` (Podman gateway IP - this is the problem)
- Accessible from host system
- Provides HTTP endpoints: `/`, `/health`, `/test`

### Container Configuration
- Image: `selenium/standalone-chrome:latest`
- Privileged mode enabled
- Startup timeout: 3 minutes
- Network: Default Podman network

### Network Communication Test
```bash
# From container attempting to reach host
curl --connect-timeout 10 --max-time 30 http://10.88.0.1:8080
# Result: Connection timeout after 10002 milliseconds
```

## Root Cause Analysis

The issue appears to be related to **network isolation between rootful Podman containers and host LAN interfaces**. Possible causes:

1. **Network namespace isolation** - Rootful containers may not have access to host LAN IPs
2. **Firewall/SELinux policies** blocking container-to-host communication
3. **Podman network configuration** not allowing access to external host interfaces
4. **Bridge network limitations** in rootful mode

## Original Bug Report Context

This issue was discovered while trying to reproduce a different bug where:
- **Reported**: Containers can't communicate with host via LAN IP when Podman Desktop is open
- **Expected**: Communication works when Podman Desktop is closed
- **Actual**: Communication fails in both scenarios (different issue)

## Impact

This networking issue affects:
- Testcontainers integration tests that need host communication
- Container applications requiring access to host services via LAN IP
- Development workflows using rootful Podman with Testcontainers

## Next Steps

1. **Test with rootless Podman** to compare behavior
2. **Investigate network configuration** and routing tables
3. **Check firewall/SELinux** policies
4. **Test alternative network configurations**
5. **Report to Podman/Testcontainers teams** if confirmed as bug

## Reproduction

The complete reproduction setup is available in this repository:
- Run `./run-test-podman-closed.sh` or `./run-test-podman-open.sh`
- Both will demonstrate the networking issue
- Host application and container logs show timeout errors

## Value to Community

Even though this isn't the originally reported bug, it represents a **real networking issue** that affects developers using Testcontainers with rootful Podman configurations.
