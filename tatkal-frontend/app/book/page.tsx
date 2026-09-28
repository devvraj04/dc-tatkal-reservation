"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar, { Breadcrumb } from "@/components/Navbar";
import { Card, CardHeader, CardBody, Button, Input, Select, Alert, Badge, StatusBadge } from "@/components/ui";
import { Plus, Trash2, Ticket, CheckCircle, User, Train, BookmarkCheck } from "lucide-react";

interface PassengerInput {
  passengerId?: number;
  name: string;
  age: string;
  gender: string;
  berthPreference: string;
  idProofType: string;
  idProofNumber: string;
  isSaved?: boolean;
}

interface SavedPassenger {
  passengerId: number;
  name: string;
  age: number;
  gender: string;
  berthPreference: string;
  idProofType: string;
  idProofNumber: string;
}

interface CoachAvail {
  coachType: string;
  availableSeats: number;
  totalSeats: number;
}

interface ScheduleOption {
  scheduleId: number;
  trainNo: number;
  trainName: string;
  journeyDate: string;
  departureTime: string;
  arrivalTime: string;
  sourceStation: string;
  destinationStation: string;
}

const blankPassenger = (): PassengerInput => ({
  name: "",
  age: "",
  gender: "MALE",
  berthPreference: "NO_PREFERENCE",
  idProofType: "AADHAAR",
  idProofNumber: "",
  isSaved: false,
});

const FARES: Record<string, number> = { "1A": 1800, "2A": 1100, "3A": 650, SL: 250 };

export default function BookPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [schedules, setSchedules] = useState<ScheduleOption[]>([]);
  const [selectedTrain, setSelectedTrain] = useState<{
    scheduleId: number;
    trainName: string;
    trainNo: number;
    departureTime?: string;
    arrivalTime?: string;
    journeyDate?: string;
  } | null>(null);

  const [savedPassengers, setSavedPassengers] = useState<SavedPassenger[]>([]);
  const [coachType, setCoachType] = useState("SL");
  const [paymentMode, setPaymentMode] = useState("UPI");
  const [passengers, setPassengers] = useState<PassengerInput[]>([blankPassenger()]);
  const [availability, setAvailability] = useState<CoachAvail[]>([]);
  const [availLoading, setAvailLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<{
    success: boolean;
    pnr: string;
    status: string;
    totalFare: number;
    message: string;
    seatAssignments: {
      passengerName: string;
      coachNumber: string | null;
      seatNumber: number;
      berthType: string | null;
      status: string;
    }[];
  } | null>(null);

  // Initialize
  useEffect(() => {
    if (!user) {
      router.push("/");
      return;
    }

    // Load pre-saved passengers
    fetch(`/api/passengers?userId=${user.userId}`)
      .then((r) => r.json())
      .then((d) => {
        if (Array.isArray(d)) setSavedPassengers(d);
      })
      .catch(() => {});

    // Load available schedules
    fetch("/api/schedules")
      .then((r) => r.json())
      .then((d) => {
        if (Array.isArray(d)) {
          setSchedules(d);
          const stored = sessionStorage.getItem("selected_train");
          if (stored) {
            try {
              setSelectedTrain(JSON.parse(stored));
            } catch {}
          } else if (d.length > 0) {
            setSelectedTrain(d[0]);
          }
        }
      })
      .catch(() => {});
  }, [user, router]);

  // Load availability when train or date changes
  useEffect(() => {
    if (!selectedTrain) return;
    setAvailLoading(true);
    const date = selectedTrain.journeyDate || new Date().toISOString().split("T")[0];
    fetch(`/api/availability?trainNo=${selectedTrain.trainNo}&date=${date}`)
      .then((r) => r.json())
      .then((d) => {
        if (Array.isArray(d)) setAvailability(d);
      })
      .catch(() => {})
      .finally(() => setAvailLoading(false));
  }, [selectedTrain]);

  const addPassenger = () => {
    if (passengers.length >= 6) return;
    setPassengers([...passengers, blankPassenger()]);
  };

  const removePassenger = (i: number) => {
    if (passengers.length === 1) return;
    setPassengers(passengers.filter((_, idx) => idx !== i));
  };

  const updatePassenger = (i: number, field: keyof PassengerInput, value: unknown) => {
    const updated = [...passengers];
    updated[i] = { ...updated[i], [field]: value };
    setPassengers(updated);
  };

  const applySavedPassenger = (i: number, savedIdStr: string) => {
    if (!savedIdStr) {
      updatePassenger(i, "isSaved", false);
      updatePassenger(i, "passengerId", undefined);
      return;
    }
    const found = savedPassengers.find((p) => p.passengerId === Number(savedIdStr));
    if (found) {
      const updated = [...passengers];
      updated[i] = {
        passengerId: found.passengerId,
        name: found.name,
        age: String(found.age),
        gender: found.gender,
        berthPreference: found.berthPreference,
        idProofType: found.idProofType,
        idProofNumber: found.idProofNumber,
        isSaved: true,
      };
      setPassengers(updated);
    }
  };

  const handleBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTrain || !user) return;
    setError("");
    setLoading(true);
    setResult(null);

    const payload = {
      userId: user.userId,
      scheduleId: selectedTrain.scheduleId,
      coachType,
      paymentMode,
      passengers: passengers.map((p) => {
        if (p.isSaved && p.passengerId) {
          return { passengerId: p.passengerId };
        }
        return {
          name: p.name.trim(),
          age: Number(p.age),
          gender: p.gender,
          berthPreference: p.berthPreference,
          idProofType: p.idProofType,
          idProofNumber: p.idProofNumber.trim(),
        };
      }),
    };

    try {
      const res = await fetch("/api/book", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (!res.ok) setError(data.error ?? "Booking failed.");
      else setResult(data);
    } catch {
      setError("Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const selectedCoachAvail = availability.find((a) => a.coachType === coachType);
  const totalFare = (FARES[coachType] ?? 200) * passengers.length;

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        <Breadcrumb items={[{ label: "Dashboard", href: "/dashboard" }, { label: "Book Ticket" }]} />

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#3D2B1F] flex items-center gap-2">
            <Ticket className="w-6 h-6 text-[#D9874C]" />
            Book Tatkal Ticket
          </h1>
          <p className="text-[#7A6552] text-sm mt-1">
            High-concurrency distributed Tatkal booking with row-level locks
          </p>
        </div>

        {/* Success state */}
        {result && result.success && (
          <div className="animate-slide-up mb-6">
            <Card className="border-[#5A8A5A]/30 bg-[#5A8A5A]/5">
              <CardBody>
                <div className="flex items-start gap-3">
                  <CheckCircle className="w-6 h-6 text-[#5A8A5A] flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <h3 className="font-bold text-[#3D2B1F] text-lg">Booking Confirmed!</h3>
                    <div className="flex flex-wrap gap-4 mt-2">
                      <div>
                        <span className="text-xs text-[#7A6552]">PNR</span>
                        <div className="font-bold text-[#D9874C] text-xl tracking-widest">{result.pnr}</div>
                      </div>
                      <div>
                        <span className="text-xs text-[#7A6552]">Status</span>
                        <div className="mt-0.5"><StatusBadge status={result.status} /></div>
                      </div>
                      <div>
                        <span className="text-xs text-[#7A6552]">Total Fare</span>
                        <div className="font-bold text-[#3D2B1F] text-lg">₹{result.totalFare.toFixed(2)}</div>
                      </div>
                    </div>

                    <div className="mt-4 space-y-2">
                      <div className="text-xs font-semibold text-[#7A6552]">ASSIGNED SEATS & COACHES:</div>
                      {result.seatAssignments.map((sa, i) => (
                        <div key={i} className="flex items-center gap-3 text-sm bg-white rounded-lg px-3 py-2 border border-[#D5C9A8]">
                          <User className="w-4 h-4 text-[#7A6552]" />
                          <span className="font-medium text-[#3D2B1F] flex-1">{sa.passengerName}</span>
                          {sa.status === "CONFIRMED" && sa.coachNumber ? (
                            <span className="text-[#7A6552] text-xs">
                              Coach <strong>{sa.coachNumber}</strong>, Seat <strong>{sa.seatNumber}</strong> ({sa.berthType})
                            </span>
                          ) : (
                            <span className="text-[#C17A30] text-xs font-medium">Waitlisted (TQWL)</span>
                          )}
                          <StatusBadge status={sa.status} />
                        </div>
                      ))}
                    </div>

                    <div className="flex gap-3 mt-5">
                      <Button size="sm" variant="secondary" onClick={() => { setResult(null); setPassengers([blankPassenger()]); }}>
                        Book Another Ticket
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => router.push("/history")}>
                        View Booking History
                      </Button>
                    </div>
                  </div>
                </div>
              </CardBody>
            </Card>
          </div>
        )}

        {!result && (
          <form onSubmit={handleBook} className="space-y-5">
            {error && <Alert type="error">{error}</Alert>}

            {/* Train selection */}
            <Card>
              <CardHeader className="flex items-center justify-between">
                <h3 className="font-semibold text-[#3D2B1F] flex items-center gap-2">
                  <Train className="w-4 h-4 text-[#D9874C]" />
                  Train & Schedule Selection
                </h3>
                <Button size="sm" variant="ghost" onClick={() => router.push("/search")}>
                  Search Route
                </Button>
              </CardHeader>
              <CardBody>
                {schedules.length === 0 ? (
                  <div className="text-sm text-[#7A6552]">Loading scheduled trains from database...</div>
                ) : (
                  <div className="space-y-3">
                    <Select
                      label="Select Scheduled Train"
                      value={selectedTrain?.scheduleId || ""}
                      onChange={(e) => {
                        const s = schedules.find((x) => x.scheduleId === Number(e.target.value));
                        if (s) setSelectedTrain(s);
                      }}
                    >
                      {schedules.map((s) => (
                        <option key={s.scheduleId} value={s.scheduleId}>
                          {s.trainName} (#{s.trainNo}) | {s.sourceStation} → {s.destinationStation} | Date: {s.journeyDate}
                        </option>
                      ))}
                    </Select>
                  </div>
                )}
              </CardBody>
            </Card>

            {/* Coach & Payment */}
            <Card>
              <CardHeader><h3 className="font-semibold text-[#3D2B1F]">Class & Payment Mode</h3></CardHeader>
              <CardBody>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Select
                    label="Coach Class"
                    value={coachType}
                    onChange={(e) => setCoachType(e.target.value)}
                  >
                    {[["SL","Sleeper Class (₹250)"],["3A","AC 3-Tier (₹650)"],["2A","AC 2-Tier (₹1100)"],["1A","AC 1st Class (₹1800)"]].map(([v,l]) => (
                      <option key={v} value={v}>{v} – {l}</option>
                    ))}
                  </Select>

                  <Select
                    label="Payment Mode"
                    value={paymentMode}
                    onChange={(e) => setPaymentMode(e.target.value)}
                  >
                    {["UPI","CREDIT_CARD","DEBIT_CARD","NET_BANKING","WALLET"].map((m) => (
                      <option key={m} value={m}>{m.replace("_", " ")}</option>
                    ))}
                  </Select>
                </div>

                {/* Availability chip */}
                {!availLoading && selectedCoachAvail && (
                  <div className="mt-3 flex items-center gap-2">
                    <span className="text-xs text-[#7A6552]">Current Seat Availability:</span>
                    <Badge variant={selectedCoachAvail.availableSeats > 0 ? "success" : "danger"}>
                      {selectedCoachAvail.availableSeats} / {selectedCoachAvail.totalSeats} seats available
                    </Badge>
                  </div>
                )}
              </CardBody>
            </Card>

            {/* Passengers */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-[#3D2B1F]">Passengers ({passengers.length})</h3>
                  <Button size="sm" variant="outline" onClick={addPassenger} type="button" disabled={passengers.length >= 6}>
                    <Plus className="w-3.5 h-3.5" /> Add Passenger
                  </Button>
                </div>
              </CardHeader>
              <CardBody className="space-y-6">
                {passengers.map((p, i) => (
                  <div key={i} className="relative p-4 rounded-xl bg-[#FEFDF0] border border-[#D5C9A8]">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-sm font-semibold text-[#3D2B1F] flex items-center gap-1.5">
                        <User className="w-4 h-4 text-[#D9874C]" />
                        Passenger {i + 1}
                      </span>
                      {passengers.length > 1 && (
                        <button type="button" onClick={() => removePassenger(i)} className="text-[#A83232] hover:bg-[#A83232]/10 rounded-lg p-1 transition-colors">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>

                    {/* Pre-saved Passenger Select */}
                    {savedPassengers.length > 0 && (
                      <div className="mb-3">
                        <Select
                          label="Use Pre-saved Profile (Optional)"
                          value={p.passengerId || ""}
                          onChange={(e) => applySavedPassenger(i, e.target.value)}
                        >
                          <option value="">-- Enter Details Manually --</option>
                          {savedPassengers.map((sp) => (
                            <option key={sp.passengerId} value={sp.passengerId}>
                              {sp.name} ({sp.age}y, {sp.gender}) · {sp.idProofType}: {sp.idProofNumber}
                            </option>
                          ))}
                        </Select>
                      </div>
                    )}

                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                      <Input
                        label="Full Name"
                        placeholder="Passenger name"
                        value={p.name}
                        onChange={(e) => updatePassenger(i, "name", e.target.value)}
                        required
                        disabled={p.isSaved}
                      />
                      <Input
                        label="Age"
                        type="number"
                        placeholder="25"
                        min="0"
                        max="120"
                        value={p.age}
                        onChange={(e) => updatePassenger(i, "age", e.target.value)}
                        required
                        disabled={p.isSaved}
                      />
                      <Select
                        label="Gender"
                        value={p.gender}
                        onChange={(e) => updatePassenger(i, "gender", e.target.value)}
                        disabled={p.isSaved}
                      >
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="OTHER">Other</option>
                      </Select>
                      <Select
                        label="Berth Preference"
                        value={p.berthPreference}
                        onChange={(e) => updatePassenger(i, "berthPreference", e.target.value)}
                        disabled={p.isSaved}
                      >
                        <option value="NO_PREFERENCE">No Preference</option>
                        <option value="LOWER">Lower</option>
                        <option value="MIDDLE">Middle</option>
                        <option value="UPPER">Upper</option>
                        <option value="SIDE_LOWER">Side Lower</option>
                        <option value="SIDE_UPPER">Side Upper</option>
                      </Select>
                      <Select
                        label="ID Proof Type"
                        value={p.idProofType}
                        onChange={(e) => updatePassenger(i, "idProofType", e.target.value)}
                        disabled={p.isSaved}
                      >
                        <option value="AADHAAR">Aadhaar</option>
                        <option value="PAN">PAN</option>
                        <option value="PASSPORT">Passport</option>
                        <option value="VOTER_ID">Voter ID</option>
                        <option value="DRIVING_LICENSE">Driving License</option>
                      </Select>
                      <Input
                        label="ID Number"
                        placeholder="ID number"
                        value={p.idProofNumber}
                        onChange={(e) => updatePassenger(i, "idProofNumber", e.target.value)}
                        required
                        disabled={p.isSaved}
                      />
                    </div>
                  </div>
                ))}
              </CardBody>
            </Card>

            {/* Summary & Submit */}
            <Card className="bg-[#D9874C]/5 border-[#D9874C]/20">
              <CardBody>
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div>
                    <div className="text-xs text-[#7A6552]">Total Fare (Tatkal Quota)</div>
                    <div className="text-3xl font-bold text-[#D9874C]">₹{totalFare.toFixed(2)}</div>
                    <div className="text-xs text-[#7A6552]">
                      {passengers.length} passenger(s) × ₹{FARES[coachType] ?? 200}
                    </div>
                  </div>
                  <Button type="submit" size="lg" loading={loading} disabled={!selectedTrain}>
                    <Ticket className="w-5 h-5" />
                    Confirm & Pay Tatkal Ticket
                  </Button>
                </div>
              </CardBody>
            </Card>
          </form>
        )}
      </div>
    </div>
  );
}
