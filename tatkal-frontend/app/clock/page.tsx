"use client";

import { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar, { Breadcrumb } from "@/components/Navbar";
import { Card, CardHeader, CardBody, Button, Badge, Alert } from "@/components/ui";
import { Clock, RefreshCw, Send, CheckCircle2, ArrowRight, Zap, Play } from "lucide-react";

interface NodeState {
  id: string;
  name: string;
  role: "server" | "client";
  initialOffsetMs: number;
  currentOffsetMs: number;
  logicalClock: number;
  lastSyncResult?: {
    t0: number;
    t1: number;
    serverTime: number;
    rtt: number;
    estimatedNetworkDelay: number;
    adjustment: number;
    differenceMs: number;
    withinWindow: boolean;
  };
}

interface MessageTransit {
  id: string;
  from: string;
  to: string;
  logicalTimestamp: number;
  description: string;
  timestamp: string;
}

export default function ClockSyncPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [nodes, setNodes] = useState<Record<string, NodeState>>({
    mumbai: {
      id: "mumbai",
      name: "Mumbai (Time Server)",
      role: "server",
      initialOffsetMs: 5000,
      currentOffsetMs: 5000,
      logicalClock: 0,
    },
    delhi: {
      id: "delhi",
      name: "Delhi (Client Node)",
      role: "client",
      initialOffsetMs: 10000,
      currentOffsetMs: 10000,
      logicalClock: 0,
    },
    chennai: {
      id: "chennai",
      name: "Chennai (Client Node)",
      role: "client",
      initialOffsetMs: 1000,
      currentOffsetMs: 1000,
      logicalClock: 0,
    },
  });

  const [messages, setMessages] = useState<MessageTransit[]>([]);
  const [eventLogs, setEventLogs] = useState<string[]>([]);
  const [dualTimestampEvent, setDualTimestampEvent] = useState<{
    pnr: string;
    node: string;
    physicalTime: string;
    logicalTime: number;
    withinWindow: boolean;
  } | null>(null);

  const [syncingNode, setSyncingNode] = useState<string | null>(null);
  const [demoRunning, setDemoRunning] = useState(false);

  const getNodePhysicalTime = (node: NodeState) => {
    const d = new Date(Date.now() + node.currentOffsetMs);
    return d.toLocaleTimeString("en-IN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      fractionalSecondDigits: 3,
    });
  };

  // Cristian's Algorithm
  const syncNodeWithServer = async (nodeId: string) => {
    setSyncingNode(nodeId);
    const node = nodes[nodeId];
    const t0 = Date.now() + node.currentOffsetMs;

    try {
      const res = await fetch("/api/clock-sync", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          clientLogicalTimestamp: node.logicalClock,
          t0,
        }),
      });
      const data = await res.json();

      // Client receives response at local time T1
      // Simulate real round trip delay locally
      const t1 = t0 + (data.rtt || 14);
      const rtt = t1 - t0;
      const estimatedNetworkDelay = Math.round(rtt / 2);
      const estimatedServerTime = data.serverTime + estimatedNetworkDelay;

      // Adjustment formula: (serverTime + networkDelay) - T1
      const adjustment = estimatedServerTime - t1;
      const newOffset = node.currentOffsetMs + adjustment;
      const diffFromMumbai = Math.abs(newOffset - nodes.mumbai.currentOffsetMs);
      const withinWindow = diffFromMumbai <= 100;

      setNodes((prev) => ({
        ...prev,
        [nodeId]: {
          ...prev[nodeId],
          currentOffsetMs: newOffset,
          lastSyncResult: {
            t0,
            t1,
            serverTime: data.serverTime,
            rtt,
            estimatedNetworkDelay,
            adjustment,
            differenceMs: diffFromMumbai,
            withinWindow,
          },
        },
      }));

      const logMsg = `[Cristian's Sync] ${node.name} synced! RTT=${rtt}ms, Adjustment=${adjustment >= 0 ? "+" : ""}${adjustment}ms, Window Diff=${diffFromMumbai}ms (${withinWindow ? "WITHIN ±100ms" : "OUTSIDE"})`;
      setEventLogs((prev) => [logMsg, ...prev]);
    } catch {
      setEventLogs((prev) => [`[Error] Synchronization failed for ${node.name}`, ...prev]);
    } finally {
      setSyncingNode(null);
    }
  };

  // Lamport Rule 1: Local Event
  const triggerLocalEvent = (nodeId: string) => {
    setNodes((prev) => {
      const n = prev[nodeId];
      const newClock = n.logicalClock + 1;
      return {
        ...prev,
        [nodeId]: { ...n, logicalClock: newClock },
      };
    });
    setEventLogs((prev) => [
      `[Lamport Rule 1] Local event executed at ${nodes[nodeId].name}. Logical Clock L: ${nodes[nodeId].logicalClock} → ${nodes[nodeId].logicalClock + 1}`,
      ...prev,
    ]);
  };

  // Lamport Rule 2 & 3: Send message from one node to another
  const sendMessage = (fromId: string, toId: string) => {
    // Rule 2: Increment sender clock before sending
    let sentClock = 0;
    setNodes((prev) => {
      const sender = prev[fromId];
      sentClock = sender.logicalClock + 1;
      return {
        ...prev,
        [fromId]: { ...sender, logicalClock: sentClock },
      };
    });

    const msg: MessageTransit = {
      id: "msg-" + Date.now(),
      from: nodes[fromId].name,
      to: nodes[toId].name,
      logicalTimestamp: sentClock,
      description: `Tatkal sync message: ${nodes[fromId].name} → ${nodes[toId].name}`,
      timestamp: new Date().toLocaleTimeString(),
    };
    setMessages((prev) => [msg, ...prev].slice(0, 8));

    // Rule 3: Receiver updates clock: L_recv = max(L_local, L_msg) + 1
    setTimeout(() => {
      setNodes((prev) => {
        const receiver = prev[toId];
        const newReceiverClock = Math.max(receiver.logicalClock, sentClock) + 1;
        return {
          ...prev,
          [toId]: { ...receiver, logicalClock: newReceiverClock },
        };
      });
      setEventLogs((prev) => [
        `[Lamport Rule 3] ${nodes[toId].name} received message with timestamp ${sentClock}. Updated L: max(${nodes[toId].logicalClock}, ${sentClock}) + 1 = ${Math.max(nodes[toId].logicalClock, sentClock) + 1}`,
        `[Lamport Rule 2] ${nodes[fromId].name} transmitted message with timestamp ${sentClock}`,
        ...prev,
      ]);
    }, 400);
  };

  // Dual Timestamp Tatkal Event (Section C)
  const triggerDualTimestampEvent = () => {
    const targetNode = nodes.delhi;
    const newClock = targetNode.logicalClock + 1;
    setNodes((prev) => ({
      ...prev,
      delhi: { ...targetNode, logicalClock: newClock },
    }));

    const pnr = "TK" + Math.floor(100000 + Math.random() * 900000);
    const physicalTime = getNodePhysicalTime(targetNode);
    const diff = Math.abs(targetNode.currentOffsetMs - nodes.mumbai.currentOffsetMs);
    const within = diff <= 100;

    setDualTimestampEvent({
      pnr,
      node: targetNode.name,
      physicalTime,
      logicalTime: newClock,
      withinWindow: within,
    });

    setEventLogs((prev) => [
      `[Dual Timestamp Booking Event] PNR: ${pnr} | Real-Time: ${physicalTime} | Lamport L: ${newClock} | Status: ${within ? "SYNCHRONIZED" : "DRIFT DETECTED"}`,
      ...prev,
    ]);
  };

  // Run full automated walkthrough
  const runFullExperiment = async () => {
    setDemoRunning(true);
    setEventLogs(["--- STARTING DISTRIBUTED COMPUTING EXPERIMENT 3 CLOCK DEMO ---"]);

    // 1. Local event in Mumbai
    triggerLocalEvent("mumbai");
    await new Promise((r) => setTimeout(r, 600));

    // 2. Cristian Sync Delhi
    await syncNodeWithServer("delhi");
    await new Promise((r) => setTimeout(r, 600));

    // 3. Cristian Sync Chennai
    await syncNodeWithServer("chennai");
    await new Promise((r) => setTimeout(r, 600));

    // 4. Send from Mumbai to Delhi
    sendMessage("mumbai", "delhi");
    await new Promise((r) => setTimeout(r, 800));

    // 5. Send from Delhi to Chennai
    sendMessage("delhi", "chennai");
    await new Promise((r) => setTimeout(r, 800));

    // 6. Dual timestamp booking
    triggerDualTimestampEvent();

    setDemoRunning(false);
  };

  const resetAll = () => {
    setNodes({
      mumbai: {
        id: "mumbai",
        name: "Mumbai (Time Server)",
        role: "server",
        initialOffsetMs: 5000,
        currentOffsetMs: 5000,
        logicalClock: 0,
      },
      delhi: {
        id: "delhi",
        name: "Delhi (Client Node)",
        role: "client",
        initialOffsetMs: 10000,
        currentOffsetMs: 10000,
        logicalClock: 0,
      },
      chennai: {
        id: "chennai",
        name: "Chennai (Client Node)",
        role: "client",
        initialOffsetMs: 1000,
        currentOffsetMs: 1000,
        logicalClock: 0,
      },
    });
    setMessages([]);
    setEventLogs(["Clocks and simulated states reset to defaults."]);
    setDualTimestampEvent(null);
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
        <Breadcrumb items={[{ label: "Dashboard", href: "/dashboard" }, { label: "Clock Algorithms (Exp 3)" }]} />

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-[#3D2B1F] flex items-center gap-2.5">
              <Clock className="w-7 h-7 text-[#D9874C]" />
              Distributed Clock Algorithms
            </h1>
            <p className="text-[#7A6552] text-sm mt-1">
              Cristian’s Physical Clock Synchronization (±100ms Window) & Lamport’s Logical Clock Event Ordering
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="primary"
              size="sm"
              loading={demoRunning}
              onClick={runFullExperiment}
            >
              <Play className="w-3.5 h-3.5" /> Run Full Experiment Demo
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={resetAll}
            >
              <RefreshCw className="w-3.5 h-3.5" /> Reset
            </Button>
          </div>
        </div>

        {/* Section A: Physical Nodes */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-[#3D2B1F]">
              Section A: Physical Nodes & Cristian’s Synchronization
            </h2>
            <Badge variant="warning">Configured Window: ±100 ms</Badge>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {Object.values(nodes).map((node) => {
              const diffFromMumbai = Math.abs(node.currentOffsetMs - nodes.mumbai.currentOffsetMs);
              const isSynchronized = diffFromMumbai <= 100;
              const isServer = node.role === "server";

              return (
                <Card
                  key={node.id}
                  className={`relative overflow-hidden transition-all duration-200 ${
                    isServer ? "border-[#D9874C] ring-2 ring-[#D9874C]/20" : ""
                  }`}
                >
                  <CardHeader className="flex items-center justify-between !py-3">
                    <div className="flex items-center gap-2">
                      <div
                        className={`w-3 h-3 rounded-full ${
                          isServer
                            ? "bg-[#D9874C] animate-pulse"
                            : isSynchronized
                            ? "bg-[#5A8A5A]"
                            : "bg-[#C17A30]"
                        }`}
                      />
                      <span className="font-bold text-sm text-[#3D2B1F]">{node.name}</span>
                    </div>
                    <Badge variant={isServer ? "primary" : isSynchronized ? "success" : "warning"}>
                      {isServer ? "Master Server" : isSynchronized ? "Synced (±100ms)" : "Drifted"}
                    </Badge>
                  </CardHeader>

                  <CardBody className="space-y-4">
                    {/* Time display */}
                    <div className="bg-[#FEFDF0] border border-[#D5C9A8] rounded-xl p-3 text-center">
                      <div className="text-[11px] text-[#7A6552] uppercase tracking-wider font-semibold">
                        Simulated Local Time
                      </div>
                      <div className="text-xl font-mono font-bold text-[#3D2B1F] mt-0.5">
                        {getNodePhysicalTime(node)}
                      </div>
                      <div className="text-xs text-[#7A6552] mt-1">
                        Offset:{" "}
                        <span className="font-semibold text-[#D9874C]">
                          {node.currentOffsetMs >= 0 ? "+" : ""}
                          {node.currentOffsetMs} ms
                        </span>{" "}
                        (Diff: {diffFromMumbai} ms)
                      </div>
                    </div>

                    {/* Logical Clock */}
                    <div className="flex items-center justify-between px-3 py-2 bg-[#F0EDD8] rounded-lg">
                      <span className="text-xs font-medium text-[#7A6552]">Lamport Clock L:</span>
                      <span className="text-sm font-mono font-bold text-[#3D2B1F] bg-white px-2 py-0.5 rounded border border-[#D5C9A8]">
                        {node.logicalClock}
                      </span>
                    </div>

                    {/* Actions */}
                    <div className="pt-2 border-t border-[#D5C9A8] flex flex-wrap gap-2">
                      {!isServer && (
                        <Button
                          size="sm"
                          variant="secondary"
                          className="flex-1 text-xs"
                          loading={syncingNode === node.id}
                          onClick={() => syncNodeWithServer(node.id)}
                        >
                          Cristian Sync
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-xs"
                        onClick={() => triggerLocalEvent(node.id)}
                      >
                        Local Event (+1)
                      </Button>
                    </div>

                    {/* Cristian Result details */}
                    {node.lastSyncResult && (
                      <div className="text-[11px] bg-[#FEFDF0] border border-[#D5C9A8] rounded-lg p-2.5 space-y-1 font-mono text-[#7A6552]">
                        <div className="text-[#3D2B1F] font-semibold mb-1">Last Sync Analysis:</div>
                        <div>RTT: {node.lastSyncResult.rtt} ms (Delay ≈ {node.lastSyncResult.estimatedNetworkDelay} ms)</div>
                        <div>Adjustment: {node.lastSyncResult.adjustment >= 0 ? "+" : ""}{node.lastSyncResult.adjustment} ms</div>
                        <div>Server Time: {new Date(node.lastSyncResult.serverTime).toLocaleTimeString("en-IN")}</div>
                      </div>
                    )}
                  </CardBody>
                </Card>
              );
            })}
          </div>
        </div>

        {/* Section B & C: Lamport Ordering & Dual Timestamps */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Section B: Message Passing (Lamport Rules 2 & 3) */}
          <Card>
            <CardHeader>
              <h3 className="font-bold text-[#3D2B1F] flex items-center gap-2">
                <Send className="w-4 h-4 text-[#D9874C]" />
                Section B: Lamport Message Passing (Rules 2 & 3)
              </h3>
            </CardHeader>
            <CardBody className="space-y-4">
              <p className="text-xs text-[#7A6552]">
                Transmit causally ordered distributed messages between logical nodes:
              </p>

              <div className="grid grid-cols-2 gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  className="text-xs justify-start"
                  onClick={() => sendMessage("mumbai", "delhi")}
                >
                  Mumbai → Delhi
                  <ArrowRight className="w-3.5 h-3.5 ml-auto" />
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="text-xs justify-start"
                  onClick={() => sendMessage("delhi", "chennai")}
                >
                  Delhi → Chennai
                  <ArrowRight className="w-3.5 h-3.5 ml-auto" />
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="text-xs justify-start"
                  onClick={() => sendMessage("chennai", "mumbai")}
                >
                  Chennai → Mumbai
                  <ArrowRight className="w-3.5 h-3.5 ml-auto" />
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="text-xs justify-start"
                  onClick={() => sendMessage("delhi", "mumbai")}
                >
                  Delhi → Mumbai
                  <ArrowRight className="w-3.5 h-3.5 ml-auto" />
                </Button>
              </div>

              {/* Transit messages */}
              <div className="mt-4 pt-4 border-t border-[#D5C9A8]">
                <div className="text-xs font-semibold text-[#3D2B1F] mb-2">Recent Message Exchanged:</div>
                {messages.length === 0 ? (
                  <div className="text-xs text-[#7A6552] italic py-2">No messages sent yet. Click any button above.</div>
                ) : (
                  <div className="space-y-2">
                    {messages.map((m) => (
                      <div
                        key={m.id}
                        className="flex items-center justify-between bg-[#FEFDF0] border border-[#D5C9A8] rounded-lg px-3 py-2 text-xs"
                      >
                        <div className="flex items-center gap-2">
                          <Zap className="w-3.5 h-3.5 text-[#D9874C]" />
                          <span className="font-medium text-[#3D2B1F]">{m.description}</span>
                        </div>
                        <Badge variant="info">L = {m.logicalTimestamp}</Badge>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardBody>
          </Card>

          {/* Section C: Dual Timestamp Booking Event */}
          <Card>
            <CardHeader>
              <h3 className="font-bold text-[#3D2B1F] flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#5A8A5A]" />
                Section C: Tatkal Booking with Dual Timestamps
              </h3>
            </CardHeader>
            <CardBody className="space-y-4">
              <p className="text-xs text-[#7A6552]">
                Every Tatkal booking transaction is stamped with both Physical Real-Time (Cristian synced) and Lamport Logical Clock:
              </p>

              <Button
                variant="secondary"
                size="md"
                className="w-full"
                onClick={triggerDualTimestampEvent}
              >
                Simulate Tatkal Booking at Delhi Node
              </Button>

              {dualTimestampEvent ? (
                <div className="bg-[#FEFDF0] border border-[#5A8A5A]/30 rounded-xl p-4 space-y-2 animate-slide-up">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#7A6552]">Generated PNR</span>
                    <span className="text-sm font-bold text-[#D9874C]">{dualTimestampEvent.pnr}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#7A6552]">Originating Node</span>
                    <span className="text-sm font-medium text-[#3D2B1F]">{dualTimestampEvent.node}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#7A6552]">Physical Timestamp (Real)</span>
                    <span className="text-sm font-mono font-bold text-[#3D2B1F]">{dualTimestampEvent.physicalTime}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#7A6552]">Lamport Logical Timestamp</span>
                    <span className="text-sm font-mono font-bold text-[#D9874C]">L = {dualTimestampEvent.logicalTime}</span>
                  </div>
                  <div className="flex items-center justify-between pt-2 border-t border-[#D5C9A8]">
                    <span className="text-xs text-[#7A6552]">Physical Clock Status</span>
                    <Badge variant={dualTimestampEvent.withinWindow ? "success" : "warning"}>
                      {dualTimestampEvent.withinWindow ? "SYNCHRONIZED (±100ms)" : "DRIFT DETECTED"}
                    </Badge>
                  </div>
                </div>
              ) : (
                <div className="bg-[#FEFDF0] border border-dashed border-[#D5C9A8] rounded-xl p-6 text-center text-xs text-[#7A6552]">
                  Click the button above to simulate a synchronized Tatkal reservation.
                </div>
              )}
            </CardBody>
          </Card>
        </div>

        {/* Experiment Audit Logs */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <h3 className="font-bold text-[#3D2B1F] flex items-center gap-2">
              <Clock className="w-4 h-4 text-[#7A6552]" />
              Experiment Execution Logs
            </h3>
            <button
              onClick={() => setEventLogs([])}
              className="text-xs text-[#7A6552] hover:text-[#D9874C]"
            >
              Clear Logs
            </button>
          </CardHeader>
          <CardBody className="!p-0">
            <div className="bg-[#3D2B1F] rounded-b-2xl p-4 font-mono text-xs space-y-1.5 max-h-60 overflow-y-auto">
              {eventLogs.length === 0 ? (
                <div className="text-[#7A6552] italic">No events recorded. Run an action above.</div>
              ) : (
                eventLogs.map((log, index) => (
                  <div
                    key={index}
                    className={`
                      ${log.startsWith("[Cristian") ? "text-[#A5BF96]" : ""}
                      ${log.startsWith("[Lamport Rule 1") ? "text-[#D9874C]" : ""}
                      ${log.startsWith("[Lamport Rule 2") ? "text-[#FEFDF0]" : ""}
                      ${log.startsWith("[Lamport Rule 3") ? "text-[#8FAB7E]" : ""}
                      ${log.startsWith("[Dual") ? "text-[#C17A30] font-bold" : ""}
                      ${log.startsWith("[Error") ? "text-[#A83232]" : ""}
                      ${log.startsWith("---") ? "text-[#FEFDF0] font-bold" : ""}
                      text-[#D5C9A8]
                    `}
                  >
                    <span className="text-[#7A6552] mr-2">[{new Date().toLocaleTimeString()}]</span>
                    {log}
                  </div>
                ))
              )}
            </div>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
