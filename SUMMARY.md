# Podman Desktop Testcontainers Bug Reproduction - Summary

## What This Project Does

This project reproduces a specific networking bug in Podman Desktop where Testcontainers cannot communicate with host applications via LAN IP when Podman Desktop's GUI is running.

## Quick Test Commands

```bash
# Test with Podman Desktop CLOSED (should work)
./run-test-podman-closed.sh

# Test with Podman Desktop OPEN (should fail according to bug report)  
./run-test-podman-open.sh
```

## Key Components

1. **HostApplication.java** - HTTP server that binds to your LAN IP (e.g., 192.168.1.XXX)
2. **PodmanDesktopNetworkingTest.java** - Testcontainers test with Chrome container
3. **Helper Scripts** - Easy-to-use test scripts with safety checks

## Expected Results

| Scenario | Expected Result |
|----------|----------------|
| Podman Desktop Closed | ✅ Test passes - networking works |
| Podman Desktop Open | ❌ Test fails - reproduces the bug |

## Container Configuration

The test uses the exact setup reported by the user:
- Chrome container with privileged mode (SYS_ADMIN equivalent)
- `withAccessToHost(true)` - key setting for host communication
- Rootful socket with Docker compatibility
- Communication via host's actual LAN IPv4 address

## Files for Sharing

When reporting results to the Podman Desktop team:
- `REPRODUCTION_STEPS.md` - Complete reproduction guide
- Test output logs from both scenarios
- System information (OS, Podman versions, network config)

## Next Steps

1. Run both test scenarios
2. Compare results
3. If issue is reproduced, share findings with Podman Desktop developers
4. Include full logs and system information for debugging
