"use client";

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar, { Breadcrumb } from "@/components/Navbar";
import { Card, CardHeader, CardBody, Button, Alert, Badge, Spinner } from "@/components/ui";
import { Users, Crown, Zap, ShieldAlert, Database, CheckCircle, RefreshCw, Power, ArrowRight, Activity } from "lucide-react";

interface UserSession {
  userId: number;
  fullName: string;
  email: string;
}

export default function ElectionPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [allUsers, setAllUsers] = useState<UserSession[]>([]);
  const [leader, setLeader] = useState<UserSession | null>(null);
  const [loading, setLoading] = useState(true);

  // Map of userId -> boolean (true = ACTIVE, false = FAILED / CRASHED)
  const [nodeHealth, setNodeHealth] = useState<Record<number, boolean>>({});

  const [electionLog, setElectionLog] = useState<string[]>([]);
  const [electionRunning, setElectionRunning] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState("");
  const [initiatorId, setInitiatorId] = useState<number>(1);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [usersRes, leaderRes] = await Promise.all([
        fetch("/api/users"),
        fetch("/api/leader"),
      ]);
      const users = await usersRes.json();
      const leaderData = await leaderRes.json();
      if (Array.isArray(users)) {
        setAllUsers(users);
        // Initialize node health for any new users
        setNodeHealth((prev) => {
          const next = { ...prev };
          users.forEach((u) => {
            if (next[u.userId] === undefined) {
              next[u.userId] = true; // default active
            }
          });
          return next;
        });
      }
      setLeader(leaderData);
    } catch {
      setError("Failed to load users or leader data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!user) {
      router.push("/");
      return;
    }
    setInitiatorId(user.userId);
    fetchData();
  }, [user, router, fetchData]);

  // Toggle single node health
  const toggleNodeHealth = (targetUserId: number) => {
    setNodeHealth((prev) => {
      const isCurrentlyActive = prev[targetUserId] !== false;
      const updated = { ...prev, [targetUserId]: !isCurrentlyActive };

      const targetUser = allUsers.find((u) => u.userId === targetUserId);
      const name = targetUser ? targetUser.fullName : `Node ${targetUserId}`;

      if (isCurrentlyActive) {
        setElectionLog((l) => [
          `[Node Crash] ${name} (ID: ${targetUserId}) marked as FAILED / DOWN.`,
          ...l,
        ]);
      } else {
        setElectionLog((l) => [
          `[Node Recovery] ${name} (ID: ${targetUserId}) has RECOVERED and is now ACTIVE.`,
          ...l,
        ]);
      }
      return updated;
    });
  };

  // Bully Election Algorithm
  const runBullyElection = async (explicitInitiatorId?: number) => {
    if (allUsers.length === 0) return;
    setElectionRunning(true);
    const startInitiator = explicitInitiatorId ?? initiatorId ?? user?.userId ?? allUsers[0].userId;

    const log: string[] = [];
    const initiatorNode = allUsers.find((u) => u.userId === startInitiator);

    log.push(`============================================================`);
    log.push(`BULLY ELECTION ALGORITHM INITIATED`);
    log.push(`Initiator: ${initiatorNode?.fullName || "User"} (ID: ${startInitiator})`);
    log.push(`Time: ${new Date().toLocaleTimeString()}`);
    log.push(`============================================================`);

    // Ensure initiator is active
    if (nodeHealth[startInitiator] === false) {
      log.push(`[Error] Initiator Node ${startInitiator} is currently FAILED. An inactive node cannot initiate an election.`);
      setElectionLog(log);
      setElectionRunning(false);
      return;
    }

    // Step 1: Find all nodes with higher ID than initiator
    const higherNodes = allUsers
      .filter((u) => u.userId > startInitiator)
      .sort((a, b) => a.userId - b.userId);

    log.push(`[Step 1] Initiator (ID: ${startInitiator}) searching for nodes with higher priority (ID > ${startInitiator})...`);

    if (higherNodes.length === 0) {
      log.push(`[Result] No higher-priority nodes exist in cluster.`);
      log.push(`[Bully Victory] Node ${startInitiator} (${initiatorNode?.fullName}) has the highest ID and wins by default.`);
      await finalizeCoordinator(startInitiator, initiatorNode?.fullName || "", log);
      setElectionRunning(false);
      return;
    }

    log.push(`Higher priority nodes found: ${higherNodes.map((u) => `${u.fullName} (ID: ${u.userId})`).join(", ")}`);
    log.push(`[Step 2] Sending ELECTION message to all higher-priority nodes...`);

    // Check responses from higher nodes (only ACTIVE nodes respond with ALIVE)
    const respondingActiveNodes: UserSession[] = [];

    for (const target of higherNodes) {
      const isTargetActive = nodeHealth[target.userId] !== false;
      if (isTargetActive) {
        log.push(`  → Sending ELECTION to ${target.fullName} (ID: ${target.userId}) ... [ALIVE ACK RECEIVED ✓]`);
        respondingActiveNodes.push(target);
      } else {
        log.push(`  → Sending ELECTION to ${target.fullName} (ID: ${target.userId}) ... [FAILED / TIMEOUT - NO ACK ✗]`);
      }
    }

    if (respondingActiveNodes.length === 0) {
      // None of the higher nodes responded! Initiator wins!
      log.push(`[Election Notice] None of the higher nodes responded (all are down/failed).`);
      log.push(`[Bully Victory] Initiator Node ${startInitiator} (${initiatorNode?.fullName}) takes over and wins the election!`);
      await finalizeCoordinator(startInitiator, initiatorNode?.fullName || "", log);
      setElectionRunning(false);
      return;
    }

    // Active higher nodes responded! The highest ACTIVE responding node takes over
    log.push(`[Step 3] Active higher nodes took over election: ${respondingActiveNodes.map((n) => `${n.fullName} (ID: ${n.userId})`).join(", ")}`);

    // Sort descending to find the highest active node
    const highestActiveNode = [...respondingActiveNodes].sort((a, b) => b.userId - a.userId)[0];

    log.push(`[Bully Hierarchy] Process ${highestActiveNode.fullName} (ID: ${highestActiveNode.userId}) holds the highest active ID.`);
    log.push(`[Verification] Checking if any active processes exist above ID ${highestActiveNode.userId}... None!`);
    log.push(`[Bully Victory] Process ${highestActiveNode.fullName} (ID: ${highestActiveNode.userId}) DECLARS VICTORY!`);

    await finalizeCoordinator(highestActiveNode.userId, highestActiveNode.fullName, log);
    setElectionRunning(false);
  };

  const finalizeCoordinator = async (winnerId: number, winnerName: string, log: string[]) => {
    log.push(`[Step 4] Broadcasting COORDINATOR message to all active nodes in cluster:`);
    allUsers.forEach((u) => {
      const isTargetActive = nodeHealth[u.userId] !== false;
      if (u.userId !== winnerId) {
        if (isTargetActive) {
          log.push(`  → COORDINATOR (${winnerName}, ID: ${winnerId}) delivered to ${u.fullName} (ID: ${u.userId}) [ACK ✓]`);
        } else {
          log.push(`  → COORDINATOR message to ${u.fullName} (ID: ${u.userId}) skipped [NODE FAILED ✗]`);
        }
      }
    });

    log.push(`[Database Transaction] Updating database users.is_leader = TRUE for User ID ${winnerId}...`);
    setElectionLog([...log]);

    // Update database
    setUpdating(true);
    try {
      const res = await fetch("/api/leader/update", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ winnerUserId: winnerId }),
      });
      const data = await res.json();
      if (res.ok) {
        log.push(`[DB Commit Success] users.is_leader updated to TRUE for ${winnerName} (ID: ${winnerId}). Single-leader constraint verified.`);
        setElectionLog([...log]);
        await fetchData();
      } else {
        log.push(`[DB Error] ${data.error}`);
        setElectionLog([...log]);
      }
    } catch {
      log.push(`[DB Error] Failed to update leader in database.`);
      setElectionLog([...log]);
    } finally {
      setUpdating(false);
    }
  };

  // Simulate Current Leader Failure
  const simulateLeaderFailure = () => {
    if (!leader) return;

    // 1. Mark current leader as FAILED
    const failedLeaderId = leader.userId;
    setNodeHealth((prev) => ({
      ...prev,
      [failedLeaderId]: false,
    }));

    const failureLogs = [
      `============================================================`,
      `>>> LEADER FAILURE SIMULATION TRIGGERED <<<`,
      `Current Leader: ${leader.fullName} (ID: ${failedLeaderId}) marked as FAILED / UNRESPONSIVE.`,
      `[Heartbeat Monitor] Health check request to leader failed with Connection Refused / Timeout.`,
      `[Trigger] Election recovery required. Selecting first active client to initiate election...`,
      `============================================================`,
    ];
    setElectionLog(failureLogs);

    // 2. Find an active initiator (preferably current user, or next available active node)
    const activeCandidates = allUsers.filter(
      (u) => u.userId !== failedLeaderId && nodeHealth[u.userId] !== false
    );

    if (activeCandidates.length === 0) {
      setElectionLog((l) => [
        ...l,
        `[Critical Error] No active nodes remaining in cluster to initiate an election!`,
      ]);
      return;
    }

    const nextInitiator = activeCandidates.find((u) => u.userId === user?.userId)?.userId || activeCandidates[0].userId;

    // 3. Run election automatically after 800ms
    setTimeout(() => {
      runBullyElection(nextInitiator);
    }, 800);
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">
        <Breadcrumb items={[{ label: "Dashboard", href: "/dashboard" }, { label: "Bully Election" }]} />

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-[#3D2B1F] flex items-center gap-2.5">
              <Users className="w-7 h-7 text-[#D9874C]" />
              Bully Election Algorithm
            </h1>
            <p className="text-[#7A6552] text-sm mt-1">
              Distributed leader election with interactive node failure simulation and atomic database persistence
            </p>
          </div>
          <Button size="sm" variant="ghost" onClick={fetchData} loading={loading}>
            <RefreshCw className="w-4 h-4" /> Refresh State
          </Button>
        </div>

        {error && <Alert type="error" className="mb-6">{error}</Alert>}

        {/* Current Leader Banner */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
          <Card className="md:col-span-2 border-[#D9874C]/30 bg-gradient-to-r from-white to-[#FEFDF0]">
            <CardHeader className="flex items-center justify-between !py-3">
              <h3 className="font-bold text-[#3D2B1F] flex items-center gap-2 text-sm">
                <Crown className="w-4 h-4 text-[#D9874C]" />
                Current Elected Leader (Database Verified)
              </h3>
              {leader && (
                <Badge variant={nodeHealth[leader.userId] !== false ? "success" : "danger"}>
                  {nodeHealth[leader.userId] !== false ? "Leader Alive" : "Leader FAILED"}
                </Badge>
              )}
            </CardHeader>
            <CardBody className="!py-4">
              {loading ? (
                <Spinner className="py-4" />
              ) : leader ? (
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-white text-xl font-bold shadow-sm ${
                      nodeHealth[leader.userId] !== false ? "bg-[#D9874C]" : "bg-[#A83232]"
                    }`}>
                      {leader.fullName.charAt(0)}
                    </div>
                    <div>
                      <div className="text-lg font-bold text-[#3D2B1F] flex items-center gap-2">
                        {leader.fullName}
                        {nodeHealth[leader.userId] === false && (
                          <span className="text-xs text-[#A83232] font-semibold">(CRASHED)</span>
                        )}
                      </div>
                      <div className="text-xs text-[#7A6552]">{leader.email}</div>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="primary">Process ID: {leader.userId}</Badge>
                        <Badge variant="success">users.is_leader = TRUE</Badge>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant={nodeHealth[leader.userId] !== false ? "danger" : "secondary"}
                      size="sm"
                      onClick={simulateLeaderFailure}
                      disabled={electionRunning || updating}
                    >
                      <ShieldAlert className="w-4 h-4" />
                      {nodeHealth[leader.userId] !== false ? "Simulate Leader Failure" : "Re-trigger Recovery"}
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="text-center py-4 text-[#A83232]">
                  <p className="font-semibold">No active leader currently configured in database</p>
                </div>
              )}
            </CardBody>
          </Card>

          {/* Controls Summary Card */}
          <Card>
            <CardHeader className="!py-3">
              <h3 className="font-bold text-[#3D2B1F] text-sm flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-[#8FAB7E]" />
                Cluster Status
              </h3>
            </CardHeader>
            <CardBody className="!py-4 space-y-3 text-xs">
              <div className="flex justify-between">
                <span className="text-[#7A6552]">Total Nodes:</span>
                <span className="font-bold text-[#3D2B1F]">{allUsers.length}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#7A6552]">Active Nodes:</span>
                <span className="font-bold text-[#5A8A5A]">
                  {allUsers.filter((u) => nodeHealth[u.userId] !== false).length}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#7A6552]">Failed / Crashed Nodes:</span>
                <span className="font-bold text-[#A83232]">
                  {allUsers.filter((u) => nodeHealth[u.userId] === false).length}
                </span>
              </div>
              <div className="pt-2 border-t border-[#D5C9A8]">
                <Button
                  size="sm"
                  variant="primary"
                  className="w-full text-xs"
                  onClick={() => runBullyElection()}
                  loading={electionRunning}
                  disabled={electionRunning || updating}
                >
                  <Zap className="w-3.5 h-3.5" />
                  Run Bully Election Now
                </Button>
              </div>
            </CardBody>
          </Card>
        </div>

        {/* Participating Nodes List with Failure Toggles */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-bold text-[#3D2B1F]">
                Participating Distributed Nodes ({allUsers.length})
              </h2>
              <p className="text-xs text-[#7A6552]">
                Click <strong>Simulate Crash</strong> on any node (e.g. Node 5 Ananya) to observe how lower nodes bully and elect the highest active node.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {allUsers.map((u) => {
              const isActive = nodeHealth[u.userId] !== false;
              const isCurrentLeader = leader?.userId === u.userId;
              const isLoggedUser = user?.userId === u.userId;

              return (
                <Card
                  key={u.userId}
                  className={`transition-all duration-200 ${
                    !isActive
                      ? "border-[#A83232]/40 bg-[#A83232]/5 opacity-80"
                      : isCurrentLeader
                      ? "border-[#D9874C] ring-2 ring-[#D9874C]/20 bg-white"
                      : "bg-white"
                  }`}
                >
                  <CardBody className="p-4 space-y-3">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2.5">
                        <div
                          className={`w-9 h-9 rounded-xl flex items-center justify-center text-sm font-bold ${
                            !isActive
                              ? "bg-[#A83232] text-white"
                              : isCurrentLeader
                              ? "bg-[#D9874C] text-white"
                              : "bg-[#8FAB7E] text-white"
                          }`}
                        >
                          {u.userId}
                        </div>
                        <div>
                          <div className="font-bold text-sm text-[#3D2B1F] flex items-center gap-1.5">
                            {u.fullName}
                            {isCurrentLeader && (
                              <Crown className="w-3.5 h-3.5 text-[#D9874C]" />
                            )}
                          </div>
                          <div className="text-[11px] text-[#7A6552]">{u.email}</div>
                        </div>
                      </div>
                      <Badge variant={isActive ? "success" : "danger"}>
                        {isActive ? "ACTIVE" : "FAILED"}
                      </Badge>
                    </div>

                    <div className="flex items-center justify-between text-xs pt-2 border-t border-[#D5C9A8]">
                      <span className="text-[#7A6552]">
                        {isLoggedUser ? (
                          <span className="font-semibold text-[#3A6EA8]">(Your Session)</span>
                        ) : (
                          `Process ID: ${u.userId}`
                        )}
                      </span>

                      {/* Crash / Recover Toggle */}
                      <button
                        type="button"
                        onClick={() => toggleNodeHealth(u.userId)}
                        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
                          isActive
                            ? "bg-[#A83232]/10 text-[#A83232] hover:bg-[#A83232] hover:text-white"
                            : "bg-[#5A8A5A]/10 text-[#5A8A5A] hover:bg-[#5A8A5A] hover:text-white"
                        }`}
                      >
                        <Power className="w-3 h-3" />
                        {isActive ? "Crash Node" : "Recover Node"}
                      </button>
                    </div>

                    {/* Quick Trigger as Initiator */}
                    {isActive && (
                      <button
                        type="button"
                        onClick={() => runBullyElection(u.userId)}
                        disabled={electionRunning}
                        className="w-full text-center text-[11px] py-1 text-[#7A6552] hover:text-[#D9874C] hover:bg-[#F0EDD8] rounded transition-colors"
                      >
                        Start Election from Node {u.userId} →
                      </button>
                    )}
                  </CardBody>
                </Card>
              );
            })}
          </div>
        </div>

        {/* Election Execution Log Visualizer */}
        <Card>
          <CardHeader className="flex items-center justify-between !py-3">
            <h3 className="font-bold text-[#3D2B1F] flex items-center gap-2 text-sm">
              <CheckCircle className="w-4 h-4 text-[#5A8A5A]" />
              Bully Election Protocol Log & Message Trace
            </h3>
            {electionLog.length > 0 && (
              <button
                onClick={() => setElectionLog([])}
                className="text-xs text-[#7A6552] hover:text-[#D9874C]"
              >
                Clear Log
              </button>
            )}
          </CardHeader>
          <CardBody className="!p-0">
            <div className="bg-[#3D2B1F] rounded-b-2xl p-5 font-mono text-xs space-y-1.5 max-h-80 overflow-y-auto">
              {electionLog.length === 0 ? (
                <div className="text-[#D5C9A8] italic py-2">
                  No election events recorded yet. Click <strong>Simulate Leader Failure</strong> or <strong>Crash Node</strong> to initiate.
                </div>
              ) : (
                electionLog.map((line, i) => (
                  <div
                    key={i}
                    className={`
                      ${line.startsWith("===") ? "text-[#D9874C] font-bold" : ""}
                      ${line.startsWith("[Bully Victory]") ? "text-[#FEFDF0] font-bold text-sm bg-[#5A8A5A]/30 p-1 rounded" : ""}
                      ${line.startsWith("[Step") ? "text-[#A5BF96] font-semibold" : ""}
                      ${line.includes("ALIVE ACK RECEIVED") ? "text-[#5A8A5A]" : ""}
                      ${line.includes("FAILED") || line.includes("NO ACK") ? "text-[#E06C75]" : ""}
                      ${line.startsWith("[DB") ? "text-[#98C379]" : ""}
                      ${line.startsWith(">>>") ? "text-[#E5C07B] font-bold" : ""}
                      text-[#D5C9A8]
                    `}
                  >
                    {line}
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
