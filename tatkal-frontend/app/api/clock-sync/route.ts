import { NextRequest, NextResponse } from "next/server";

// Cristian's Algorithm + Lamport Logical Clock
// Mirrors BookingServiceImpl.getServerPhysicalTime() and processClockSyncMessage()
let logicalClock = 0;

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => ({}));
  const { clientLogicalTimestamp = 0, t0 } = body as {
    clientLogicalTimestamp?: number;
    t0: number;
  };

  // Simulate ~5ms server processing (as in Java impl)
  await new Promise((r) => setTimeout(r, 5));

  const t1 = Date.now(); // server time (T1 from server's perspective)
  const rtt = t1 - t0;
  const estimatedNetworkDelay = Math.round(rtt / 2);
  const serverTime = t0 + estimatedNetworkDelay; // Cristian's estimate

  // Lamport logical clock update (rule: max + 1)
  logicalClock = Math.max(logicalClock, clientLogicalTimestamp) + 1;
  const serverLogical = logicalClock;

  const adjustment = serverTime - t1;
  const differenceMs = Math.abs(serverTime - t1);
  const withinWindow = differenceMs <= 100;

  const date = new Date(serverTime);
  const formattedServerTime = date.toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });

  return NextResponse.json({
    t0,
    serverTime,
    t1,
    rtt,
    estimatedNetworkDelay,
    adjustment,
    differenceMs,
    withinWindow,
    logicalTimestamp: serverLogical,
    serverNode: "Mumbai",
    formattedServerTime,
  });
}
