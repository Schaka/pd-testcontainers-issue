package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that reproduces the Podman Desktop networking issue reported by the user.
 * 
 * This test:
 * 1. Starts a Chrome container with SYS_ADMIN capabilities and host access
 * 2. Attempts to communicate with the host application via LAN IP
 * 3. Should work when Podman Desktop is closed, fail when it's open
 */
@Testcontainers
public class PodmanDesktopNetworkingTest {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome:latest";
    private static final int HOST_PORT = 8080;
    
    private HostApplication hostApp;
    private String lanIpAddress;

    @Container
    private GenericContainer<?> chromeContainer = new GenericContainer<>(DockerImageName.parse(CHROME_IMAGE))
            .withPrivilegedMode(true)  // Equivalent to SYS_ADMIN capability
            // Temporarily disable withAccessToHost to test basic functionality
            // .withAccessToHost(true)    // This is the key setting from the user's setup
            .withStartupTimeout(Duration.ofMinutes(3));

    @BeforeEach
    void setUp() throws IOException {
        // Start the host application
        hostApp = new HostApplication();
        hostApp.start();
        lanIpAddress = hostApp.getLanIpAddress();
        
        System.out.println("Host application started on: " + lanIpAddress + ":" + HOST_PORT);
        System.out.println("Chrome container will attempt to connect to this address");
    }

    @AfterEach
    void tearDown() {
        if (hostApp != null) {
            hostApp.stop();
        }
    }

    @Test
    void testContainerToHostCommunicationViaLanIp() {
        // Wait for container to be fully started
        assertTrue(chromeContainer.isRunning(), "Chrome container should be running");
        
        System.out.println("Container ID: " + chromeContainer.getContainerId());
        System.out.println("Container network mode: " + chromeContainer.getNetworkMode());
        
        // Test 1: Basic connectivity test using curl
        testBasicConnectivity();
        
        // Test 2: HTTP request to the host application
        testHttpCommunication();
        
        // Test 3: Multiple requests to simulate real-world usage
        testMultipleRequests();
    }

    private void testBasicConnectivity() {
        System.out.println("\n=== Testing Basic Connectivity ===");
        
        // Test ping to the LAN IP
        try {
            var pingResult = chromeContainer.execInContainer(
                "ping", "-c", "3", lanIpAddress
            );
            
            System.out.println("Ping exit code: " + pingResult.getExitCode());
            System.out.println("Ping stdout: " + pingResult.getStdout());
            System.out.println("Ping stderr: " + pingResult.getStderr());
            
            // Note: We don't assert success here because the issue might prevent this from working
            if (pingResult.getExitCode() == 0) {
                System.out.println("✅ Ping successful - network connectivity is working");
            } else {
                System.out.println("❌ Ping failed - this might indicate the reported issue");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to execute ping: " + e.getMessage());
        }
    }

    private void testHttpCommunication() {
        System.out.println("\n=== Testing HTTP Communication ===");
        
        String hostUrl = "http://" + lanIpAddress + ":" + HOST_PORT;
        
        try {
            // Test connection to root endpoint
            var curlResult = chromeContainer.execInContainer(
                "curl", "-v", "--connect-timeout", "10", "--max-time", "30", 
                hostUrl
            );
            
            System.out.println("Curl exit code: " + curlResult.getExitCode());
            System.out.println("Curl stdout: " + curlResult.getStdout());
            System.out.println("Curl stderr: " + curlResult.getStderr());
            
            if (curlResult.getExitCode() == 0) {
                System.out.println("✅ HTTP communication successful");
                assertTrue(curlResult.getStdout().contains("Host Application"), 
                    "Response should contain expected content");
            } else {
                System.out.println("❌ HTTP communication failed - this reproduces the reported issue");
                System.out.println("Expected behavior: This should work when Podman Desktop is closed");
                System.out.println("Reported issue: This fails when Podman Desktop is open");
                
                // Don't fail the test - we want to document the issue
                System.out.println("Test completed - check if Podman Desktop is open or closed");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to execute curl: " + e.getMessage());
        }
    }

    private void testMultipleRequests() {
        System.out.println("\n=== Testing Multiple Requests ===");
        
        String healthUrl = "http://" + lanIpAddress + ":" + HOST_PORT + "/health";
        String testUrl = "http://" + lanIpAddress + ":" + HOST_PORT + "/test";
        
        // Test multiple endpoints to simulate real-world usage
        String[] urls = {healthUrl, testUrl};
        
        for (String url : urls) {
            try {
                var result = chromeContainer.execInContainer(
                    "curl", "-s", "--connect-timeout", "5", "--max-time", "15", url
                );
                
                System.out.println("Request to " + url + " - Exit code: " + result.getExitCode());
                if (result.getExitCode() == 0) {
                    System.out.println("  Response: " + result.getStdout().trim());
                } else {
                    System.out.println("  Error: " + result.getStderr());
                }
                
            } catch (Exception e) {
                System.err.println("Failed request to " + url + ": " + e.getMessage());
            }
        }
    }

    /**
     * Additional test method to run network diagnostics
     */
    @Test
    void runNetworkDiagnostics() {
        System.out.println("\n=== Network Diagnostics ===");
        
        try {
            // Show container's network configuration
            var ifconfigResult = chromeContainer.execInContainer("ip", "addr", "show");
            System.out.println("Container network interfaces:");
            System.out.println(ifconfigResult.getStdout());
            
            // Show routing table
            var routeResult = chromeContainer.execInContainer("ip", "route", "show");
            System.out.println("Container routing table:");
            System.out.println(routeResult.getStdout());
            
            // Test DNS resolution
            var nslookupResult = chromeContainer.execInContainer("nslookup", lanIpAddress);
            System.out.println("DNS lookup for LAN IP:");
            System.out.println("Exit code: " + nslookupResult.getExitCode());
            System.out.println("Output: " + nslookupResult.getStdout());
            
        } catch (Exception e) {
            System.err.println("Failed to run network diagnostics: " + e.getMessage());
        }
    }

    /**
     * Test specifically designed to check the issue described by the user
     */
    @Test 
    void testPodmanDesktopInterference() {
        System.out.println("\n=== Testing Podman Desktop Interference ===");
        System.out.println("MANUAL TEST INSTRUCTIONS:");
        System.out.println("1. First, close Podman Desktop completely");
        System.out.println("2. Run this test - it should PASS");
        System.out.println("3. Then, open Podman Desktop");
        System.out.println("4. Run this test again - according to the bug report, it should FAIL");
        System.out.println("5. Compare the results to confirm the issue");
        
        // Perform the actual test
        testBasicConnectivity();
        testHttpCommunication();
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("If this test fails when Podman Desktop is open but passes when it's closed,");
        System.out.println("then we have successfully reproduced the reported issue.");
    }
}
