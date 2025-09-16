# Podman Desktop Testcontainers Networking Bug Reproduction

> **Privacy Note**: This repository has been cleaned of personal information. All IP addresses, usernames, and system-specific details have been anonymized.

This project reproduces a networking issue where Testcontainers cannot communicate with the host via LAN IP when using Podman configurations.

## Quick Start

1. **Build the project**:
   ```bash
   mvn clean compile test-compile
   ```

2. **Test with Podman Desktop closed**:
   ```bash
   ./run-test-podman-closed.sh
   ```

3. **Test with Podman Desktop open**:
   ```bash
   # Start Podman Desktop first
   flatpak run io.podman_desktop.PodmanDesktop &
   
   # Then run the test
   ./run-test-podman-open.sh
   ```

4. **Compare results** - analyze the networking behavior in both scenarios.

## Files

- `REPRODUCTION_STEPS.md` - Detailed reproduction instructions
- `COMPREHENSIVE_ANALYSIS.md` - Complete technical analysis of findings
- `NETWORK_ISSUE_FINDINGS.md` - Documented network issue details
- `run-*.sh` - Helper scripts for easy testing
- `src/main/java/com/example/HostApplication.java` - Host application
- `src/test/java/com/example/PodmanDesktopNetworkingTest.java` - Test cases

## Issue Details

**Problem**: Containers cannot reach host applications via LAN IP due to network configuration issues.

**Environment**: Fedora 42, Podman Desktop 1.19.2 (Flatpak), Podman (Fedora repos), rootful/rootless socket configurations.

**Container Setup**: Chrome with SYS_ADMIN capabilities for comprehensive testing.

## Key Findings

This project discovered that the networking issue is related to **IP address detection problems** rather than Podman Desktop specifically. The host application incorrectly detects the Podman bridge gateway IP instead of the actual LAN IP.

See `REPRODUCTION_STEPS.md` and `COMPREHENSIVE_ANALYSIS.md` for complete details.

## Contributing

This reproduction environment can be used to test various Podman + Testcontainers networking scenarios. Feel free to extend it for additional test cases.
