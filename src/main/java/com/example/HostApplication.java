package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Simple HTTP server that runs on the host's LAN IP address.
 * This simulates the user's setup where containers need to communicate with the host via LAN IP.
 */
public class HostApplication {
    private static final int PORT = 8080;
    private HttpServer server;
    private String lanIpAddress;

    public HostApplication() throws IOException {
        this.lanIpAddress = findLanIpAddress();
        System.out.println("Detected LAN IP: " + lanIpAddress);
        
        // Create HTTP server bound to the LAN IP
        InetSocketAddress address = new InetSocketAddress(lanIpAddress, PORT);
        this.server = HttpServer.create(address, 0);
        
        // Add handlers
        server.createContext("/", new RootHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/test", new TestHandler());
        
        server.setExecutor(null); // Use default executor
    }

    public void start() {
        server.start();
        System.out.println("Host application started on http://" + lanIpAddress + ":" + PORT);
        System.out.println("Available endpoints:");
        System.out.println("  - GET /         - Root endpoint");
        System.out.println("  - GET /health   - Health check");
        System.out.println("  - GET /test     - Test endpoint");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Host application stopped");
        }
    }

    public String getLanIpAddress() {
        return lanIpAddress;
    }

    public int getPort() {
        return PORT;
    }

    /**
     * Find the LAN IP address (192.168.x.x or 10.x.x.x or 172.16-31.x.x)
     */
    private String findLanIpAddress() throws IOException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        
        for (NetworkInterface ni : Collections.list(interfaces)) {
            if (!ni.isUp() || ni.isLoopback()) {
                continue;
            }
            
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            for (InetAddress addr : Collections.list(addresses)) {
                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                    continue;
                }
                
                String ip = addr.getHostAddress();
                // Check for common private network ranges
                if (ip.startsWith("192.168.") || 
                    ip.startsWith("10.") || 
                    (ip.startsWith("172.") && isInRange172(ip))) {
                    return ip;
                }
            }
        }
        
        // Fallback to localhost if no LAN IP found
        System.out.println("Warning: No LAN IP found, using localhost");
        return "127.0.0.1";
    }

    private boolean isInRange172(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return false;
    }

    // HTTP Handlers
    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Host Application - Running on LAN IP\n" +
                            "Timestamp: " + System.currentTimeMillis() + "\n" +
                            "Remote Address: " + exchange.getRemoteAddress() + "\n";
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "OK";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Test endpoint reached successfully!\n" +
                            "This confirms container-to-host communication via LAN IP is working.\n" +
                            "Request from: " + exchange.getRemoteAddress() + "\n";
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public static void main(String[] args) {
        try {
            HostApplication app = new HostApplication();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
            
            app.start();
            
            // Keep the application running
            System.out.println("Press Ctrl+C to stop the server");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Failed to start host application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
