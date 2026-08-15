package client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnsafeBookingDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("     Conceptual Demonstration: Race Condition Hazard       ");
        System.out.println("==========================================================");
        System.out.println("Scenario: 2 Concurrent Threads attempting seat allocation ");
        System.out.println("          WITHOUT PostgreSQL 'FOR UPDATE' row-level locks.");
        System.out.println("==========================================================");

        // Shared seat pool representing 1 available seat
        SimulatedUnsafeSeatPool seatPool = new SimulatedUnsafeSeatPool();

        Runnable bookingTask = () -> {
            String threadName = Thread.currentThread().getName();
            System.out.println("[" + threadName + "] Reading available seat count...");
            
            // Step 1: Read without lock
            int available = seatPool.getAvailableCount();
            System.out.println("[" + threadName + "] Found " + available + " available seat(s).");
            
            if (available > 0) {
                // Simulate context switch / network latency delay between READ and WRITE
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                // Step 2: Unsafe Write / Allocation
                String allocatedSeat = seatPool.allocateSeatUnsafely(threadName);
                System.out.println("[" + threadName + "] Allocated seat: " + allocatedSeat);
            } else {
                System.out.println("[" + threadName + "] Booking failed: No seats available.");
            }
        };

        Thread t1 = new Thread(bookingTask, "UserThread-1");
        Thread t2 = new Thread(bookingTask, "UserThread-2");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n----------------------------------------------------------");
        System.out.println("UNSAFE DEMO RESULT:");
        System.out.println("Total allocations made for 1 available seat: " + seatPool.getAllocatedLog().size());
        if (seatPool.getAllocatedLog().size() > 1) {
            System.err.println("HAZARD DETECTED: DOUBLE BOOKING OCCURRED! Both users received seat " + seatPool.getAllocatedLog().get(0));
            System.out.println("EXPLANATION: Without 'SELECT ... FOR UPDATE SKIP LOCKED', two concurrent");
            System.out.println("transactions read the same seat as AVAILABLE before either updates it.");
        }
        System.out.println("==========================================================");
    }

    private static class SimulatedUnsafeSeatPool {
        private int availableCount = 1;
        private final List<String> allocatedLog = Collections.synchronizedList(new ArrayList<>());

        public int getAvailableCount() {
            return availableCount;
        }

        public String allocateSeatUnsafely(String threadName) {
            // Decrement available count without transaction isolation
            availableCount--;
            String seatName = "Coach SL1, Seat 1 (LOWER)";
            allocatedLog.add(seatName);
            return seatName;
        }

        public List<String> getAllocatedLog() {
            return allocatedLog;
        }
    }
}
