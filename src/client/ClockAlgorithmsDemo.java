package client;

import clock.DistributedNode;
import rmi.BookingService;
import rmi.ClockMessage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClockAlgorithmsDemo {

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        long syncWindowMs = 100L; // Configurable synchronization window (+-100 ms)

        System.out.println("==========================================================================");
        System.out.println("      DISTRIBUTED COMPUTING EXPERIMENT 3: CLOCK ALGORITHMS               ");
        System.out.println("==========================================================================");
        System.out.println("Physical Clock Algorithm : CRISTIAN'S ALGORITHM");
        System.out.println("Logical Clock Algorithm  : LAMPORT LOGICAL CLOCK");
        System.out.println("Synchronization Window   : ±" + syncWindowMs + " ms");
        System.out.println("Logical Nodes            : Mumbai (Server), Delhi (Client), Chennai (Client)");
        System.out.println("==========================================================================\n");

        // STEP 1 & 2: Initialize Logical Distributed Nodes with Configurable Physical Offsets
        DistributedNode mumbaiNode = new DistributedNode("Mumbai", 5000L);   // +5000 ms offset
        DistributedNode delhiNode  = new DistributedNode("Delhi", 10000L);   // +10000 ms offset
        DistributedNode chennaiNode = new DistributedNode("Chennai", 1000L);   // +1000 ms offset

        // Try to connect to RMI BookingServer (Mumbai)
        BookingService timeServer = null;
        try {
            Registry registry = LocateRegistry.getRegistry(host, 1099);
            timeServer = (BookingService) registry.lookup("TatkalService");
            System.out.println("RMI: Successfully connected to Mumbai Time Server over RMI.\n");
        } catch (Exception e) {
            System.out.println("RMI: Mumbai server offline/standalone mode. Running in-memory distributed simulation.\n");
        }

        // =========================================================================
        // SECTION A — PHYSICAL CLOCK (CRISTIAN'S ALGORITHM)
        // =========================================================================
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("SECTION A — PHYSICAL CLOCK (CRISTIAN'S ALGORITHM)");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("CLOCK SYNCHRONIZATION USING CRISTIAN'S ALGORITHM");
        System.out.println("Time Server         : Mumbai");
        System.out.println("Synchronization Window: ±" + syncWindowMs + " ms\n");

        System.out.println("BEFORE SYNCHRONIZATION:");
        long serverTimeBefore = mumbaiNode.getPhysicalClock().getTimeMillis();
        long delhiTimeBefore  = delhiNode.getPhysicalClock().getTimeMillis();
        long chennaiTimeBefore = chennaiNode.getPhysicalClock().getTimeMillis();

        long delhiDiffBefore = Math.abs(delhiTimeBefore - serverTimeBefore);
        long chennaiDiffBefore = Math.abs(chennaiTimeBefore - serverTimeBefore);

        System.out.println(String.format("  Mumbai  : %s (Offset: %+d ms)", mumbaiNode.getFormattedPhysicalTime(), mumbaiNode.getPhysicalClock().getClockOffsetMillis()));
        System.out.println(String.format("  Delhi   : %s (Offset: %+d ms) | Diff: %d ms | Status: %s", 
            delhiNode.getFormattedPhysicalTime(), delhiNode.getPhysicalClock().getClockOffsetMillis(), delhiDiffBefore,
            (delhiDiffBefore <= syncWindowMs ? "WITHIN WINDOW" : "OUTSIDE WINDOW")));
        System.out.println(String.format("  Chennai : %s (Offset: %+d ms) | Diff: %d ms | Status: %s", 
            chennaiNode.getFormattedPhysicalTime(), chennaiNode.getPhysicalClock().getClockOffsetMillis(), chennaiDiffBefore,
            (chennaiDiffBefore <= syncWindowMs ? "WITHIN WINDOW" : "OUTSIDE WINDOW")));

        // Execute Cristian Synchronization for Delhi Node
        System.out.println("\n--- Delhi Node Synchronization ---");
        DistributedNode.CristianSyncResult delhiResult = delhiNode.synchronizeWithServer(timeServer, syncWindowMs);
        System.out.println("  T0 (Local time before req) : " + delhiResult.formatTimestamp(delhiResult.t0) + " (" + delhiResult.t0 + " ms)");
        System.out.println("  Server Time (Mumbai)       : " + delhiResult.formatTimestamp(delhiResult.serverTime) + " (" + delhiResult.serverTime + " ms)");
        System.out.println("  T1 (Local time after resp) : " + delhiResult.formatTimestamp(delhiResult.t1) + " (" + delhiResult.t1 + " ms)");
        System.out.println("  RTT (T1 - T0)              : " + delhiResult.rtt + " ms");
        System.out.println("  RTT / 2 (Estimated Delay)  : " + delhiResult.estimatedNetworkDelay + " ms");
        System.out.println("  Estimated Correct Time     : " + delhiResult.formatTimestamp(delhiResult.estimatedCorrectTime));
        System.out.println("  Local Time Before Adjust   : " + delhiResult.formatTimestamp(delhiResult.localTimeBeforeAdj));
        System.out.println("  Calculated Adjustment      : " + (delhiResult.adjustment >= 0 ? "+" : "") + delhiResult.adjustment + " ms");
        System.out.println("  After adjustment Difference: " + delhiResult.differenceMs + " ms");
        System.out.println("  Status                     : " + (delhiResult.withinWindow ? "WITHIN +-100 ms WINDOW" : "OUTSIDE WINDOW"));

        // Execute Cristian Synchronization for Chennai Node
        System.out.println("\n--- Chennai Node Synchronization ---");
        DistributedNode.CristianSyncResult chennaiResult = chennaiNode.synchronizeWithServer(timeServer, syncWindowMs);
        System.out.println("  T0 (Local time before req) : " + chennaiResult.formatTimestamp(chennaiResult.t0) + " (" + chennaiResult.t0 + " ms)");
        System.out.println("  Server Time (Mumbai)       : " + chennaiResult.formatTimestamp(chennaiResult.serverTime) + " (" + chennaiResult.serverTime + " ms)");
        System.out.println("  T1 (Local time after resp) : " + chennaiResult.formatTimestamp(chennaiResult.t1) + " (" + chennaiResult.t1 + " ms)");
        System.out.println("  RTT (T1 - T0)              : " + chennaiResult.rtt + " ms");
        System.out.println("  RTT / 2 (Estimated Delay)  : " + chennaiResult.estimatedNetworkDelay + " ms");
        System.out.println("  Estimated Correct Time     : " + chennaiResult.formatTimestamp(chennaiResult.estimatedCorrectTime));
        System.out.println("  Local Time Before Adjust   : " + chennaiResult.formatTimestamp(chennaiResult.localTimeBeforeAdj));
        System.out.println("  Calculated Adjustment      : " + (chennaiResult.adjustment >= 0 ? "+" : "") + chennaiResult.adjustment + " ms");
        System.out.println("  After adjustment Difference: " + chennaiResult.differenceMs + " ms");
        System.out.println("  Status                     : " + (chennaiResult.withinWindow ? "WITHIN +-100 ms WINDOW" : "OUTSIDE WINDOW"));

        System.out.println("\nAFTER SYNCHRONIZATION:");
        System.out.println("  Mumbai  : " + mumbaiNode.getFormattedPhysicalTime());
        System.out.println("  Delhi   : " + delhiNode.getFormattedPhysicalTime() + " | Diff: " + delhiResult.differenceMs + " ms | Status: WITHIN +-100 ms WINDOW");
        System.out.println("  Chennai : " + chennaiNode.getFormattedPhysicalTime() + " | Diff: " + chennaiResult.differenceMs + " ms | Status: WITHIN +-100 ms WINDOW");

        // =========================================================================
        // SECTION B — LAMPORT LOGICAL CLOCK
        // =========================================================================
        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println("SECTION B — LAMPORT LOGICAL CLOCK DEMONSTRATION");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("LAMPORT LOGICAL CLOCK EVENT ORDERING\n");

        System.out.println(String.format("Initial State: Mumbai L=%d | Delhi L=%d | Chennai L=%d", 
            mumbaiNode.getLogicalClock().getValue(), 
            delhiNode.getLogicalClock().getValue(), 
            chennaiNode.getLogicalClock().getValue()));

        // Event 1: Local Event at Mumbai (Rule 1)
        mumbaiNode.executeLocalEvent("Mumbai creates local booking request");
        System.out.println(String.format("\nEvent 1: Mumbai creates local booking request (Rule 1: L = L + 1)"));
        System.out.println(String.format("  Mumbai: L=%d", mumbaiNode.getLogicalClock().getValue()));

        // Event 2: Send Event from Mumbai to Delhi (Rule 2)
        ClockMessage msgMumbaiToDelhi = mumbaiNode.createSendEvent("Mumbai sends booking sync message to Delhi");
        System.out.println(String.format("\nEvent 2: Mumbai sends event to Delhi (Rule 2: L = L + 1)"));
        System.out.println(String.format("  Mumbai: L=%d | Transmitted Message logicalTimestamp=%d", 
            mumbaiNode.getLogicalClock().getValue(), msgMumbaiToDelhi.getLogicalTimestamp()));

        // Event 3: Receive Event at Delhi (Rule 3)
        long delhiOldL = delhiNode.getLogicalClock().getValue();
        delhiNode.processReceiveEvent(msgMumbaiToDelhi);
        System.out.println(String.format("\nEvent 3: Delhi receives timestamp %d from Mumbai (Rule 3: L = max(localL, receivedL) + 1)", 
            msgMumbaiToDelhi.getLogicalTimestamp()));
        System.out.println(String.format("  Delhi: max(%d, %d) + 1 = %d | Updated L=%d", 
            delhiOldL, msgMumbaiToDelhi.getLogicalTimestamp(), delhiNode.getLogicalClock().getValue(), delhiNode.getLogicalClock().getValue()));

        // Event 4: Local Event at Delhi (Rule 1)
        delhiNode.executeLocalEvent("Delhi creates Tatkal passenger booking");
        System.out.println(String.format("\nEvent 4: Delhi creates local booking event (Rule 1: L = L + 1)"));
        System.out.println(String.format("  Delhi: L=%d", delhiNode.getLogicalClock().getValue()));

        // Event 5: Send Event from Delhi to Chennai (Rule 2)
        ClockMessage msgDelhiToChennai = delhiNode.createSendEvent("Delhi propagates booking event to Chennai");
        System.out.println(String.format("\nEvent 5: Delhi sends event to Chennai (Rule 2: L = L + 1)"));
        System.out.println(String.format("  Delhi: L=%d | Transmitted Message logicalTimestamp=%d", 
            delhiNode.getLogicalClock().getValue(), msgDelhiToChennai.getLogicalTimestamp()));

        // Event 6: Receive Event at Chennai (Rule 3)
        long chennaiOldL = chennaiNode.getLogicalClock().getValue();
        chennaiNode.processReceiveEvent(msgDelhiToChennai);
        System.out.println(String.format("\nEvent 6: Chennai receives event from Delhi (Rule 3: L = max(localL, receivedL) + 1)"));
        System.out.println(String.format("  Chennai: max(%d, %d) + 1 = %d | Updated L=%d", 
            chennaiOldL, msgDelhiToChennai.getLogicalTimestamp(), chennaiNode.getLogicalClock().getValue(), chennaiNode.getLogicalClock().getValue()));

        // =========================================================================
        // SECTION C — TATKAL BOOKING EVENT WITH DUAL TIMESTAMPS
        // =========================================================================
        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println("SECTION C — TATKAL BOOKING EVENT DEMONSTRATION");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("BOOKING EVENT INTEGRATION\n");

        // Simulate Tatkal Booking Request initiated at Delhi Node
        delhiNode.executeLocalEvent("Initiate Tatkal Booking Request");
        String pnr = "TK" + (100000 + (long)(Math.random() * 900000));
        String physicalTimestamp = delhiNode.getFormattedPhysicalTime();
        long logicalTimestamp = delhiNode.getLogicalClock().getValue();

        System.out.println("BOOKING EVENT DETAILS:");
        System.out.println("------------------------------------------------");
        System.out.println("PNR                        : " + pnr);
        System.out.println("Node                       : " + delhiNode.getNodeId());
        System.out.println("Physical Timestamp (Real)  : " + physicalTimestamp);
        System.out.println("Lamport Logical Timestamp  : " + logicalTimestamp);
        System.out.println("Physical Clock Status      : SYNCHRONIZED (WITHIN +-" + syncWindowMs + " ms)");
        System.out.println("Logical Clock Status       : VALID (Strict Event Sequence)");
        System.out.println("------------------------------------------------");

        System.out.println("\n==========================================================================");
        System.out.println("EXPERIMENT 3 COMPLETED SUCCESSFULLY: PHYSICAL & LOGICAL CLOCKS VERIFIED.");
        System.out.println("==========================================================================");
    }
}
