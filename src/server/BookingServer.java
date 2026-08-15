package server;

import rmi.BookingService;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BookingServer {
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("    Distributed Tatkal Booking Server         ");
        System.out.println("==============================================");
        
        try {
            // Start RMI Registry programmatically on port 1099
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("RMI: Started local registry on port 1099.");
            } catch (Exception e) {
                // If already running, fetch the existing registry
                registry = LocateRegistry.getRegistry(1099);
                System.out.println("RMI: Found existing registry on port 1099.");
            }

            // Bind the remote object (bind under both BookingService and TatkalService for compatibility)
            BookingService bookingService = new BookingServiceImpl();
            registry.rebind("BookingService", bookingService);
            registry.rebind("TatkalService", bookingService);
            
            System.out.println("RMI: BookingService & TatkalService successfully bound to registry.");
            System.out.println("Server is running. Press Ctrl+C to terminate...");
            System.out.println("==============================================");
            
        } catch (Exception e) {
            System.err.println("CRITICAL: Booking Server startup failed!");
            e.printStackTrace();
        }
    }
}
