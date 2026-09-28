"use client";

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar, { Breadcrumb } from "@/components/Navbar";
import { Card, CardBody, Button, Alert, StatusBadge, Spinner } from "@/components/ui";
import { BarChart3, Train, User, ChevronDown, ChevronUp, RefreshCw } from "lucide-react";

interface PassengerDetail {
  passengerName: string;
  coachNumber: string | null;
  seatNumber: number;
  berthType: string | null;
  status: string;
}

interface BookingItem {
  pnr: string;
  trainNo: number;
  trainName: string;
  journeyDate: string;
  sourceStation: string;
  destinationStation: string;
  bookingStatus: string;
  totalFare: number;
  bookingTimestamp: string;
  passengers: PassengerDetail[];
}

function BookingCard({ item }: { item: BookingItem }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <Card className="animate-fade-in">
      <CardBody>
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#D9874C]/10 flex items-center justify-center flex-shrink-0">
              <Train className="w-5 h-5 text-[#D9874C]" />
            </div>
            <div>
              <div className="font-semibold text-[#3D2B1F]">{item.trainName}</div>
              <div className="text-xs text-[#7A6552] mt-0.5">#{item.trainNo} · {item.sourceStation} → {item.destinationStation}</div>
              <div className="flex flex-wrap gap-2 items-center mt-2">
                <StatusBadge status={item.bookingStatus} />
                <span className="text-xs text-[#7A6552]">{new Date(item.journeyDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })}</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-[10px] text-[#7A6552] uppercase tracking-wide">PNR</div>
              <div className="font-bold text-[#D9874C] tracking-wider text-sm">{item.pnr}</div>
              <div className="text-xs text-[#7A6552] mt-0.5">₹{item.totalFare.toFixed(2)}</div>
            </div>
            <button
              onClick={() => setExpanded(!expanded)}
              className="p-2 rounded-lg hover:bg-[#F0EDD8] text-[#7A6552] transition-colors"
            >
              {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {expanded && (
          <div className="mt-4 pt-4 border-t border-[#D5C9A8] animate-fade-in">
            <div className="text-xs text-[#7A6552] mb-2 font-medium">PASSENGER DETAILS</div>
            <div className="space-y-2">
              {item.passengers.map((p, i) => (
                <div key={i} className="flex items-center gap-3 bg-[#FEFDF0] rounded-lg px-3 py-2 border border-[#D5C9A8] text-sm">
                  <User className="w-4 h-4 text-[#7A6552] flex-shrink-0" />
                  <span className="font-medium text-[#3D2B1F] flex-1">{p.passengerName}</span>
                  {p.status === "CONFIRMED" && p.coachNumber ? (
                    <span className="text-[#7A6552] text-xs">Coach {p.coachNumber} · Seat {p.seatNumber} ({p.berthType})</span>
                  ) : null}
                  <StatusBadge status={p.status} />
                </div>
              ))}
            </div>
            <div className="text-[10px] text-[#7A6552] mt-2">
              Booked on: {new Date(item.bookingTimestamp).toLocaleString("en-IN")}
            </div>
          </div>
        )}
      </CardBody>
    </Card>
  );
}

export default function HistoryPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [bookings, setBookings] = useState<BookingItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchHistory = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`/api/history?userId=${user.userId}`);
      const data = await res.json();
      if (!res.ok) setError(data.error ?? "Failed to load history.");
      else setBookings(data);
    } catch {
      setError("Network error.");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (!user) { router.push("/"); return; }
    fetchHistory();
  }, [user, router, fetchHistory]);

  if (!user) return null;

  const confirmed = bookings.filter((b) => b.bookingStatus === "CONFIRMED").length;
  const cancelled = bookings.filter((b) => b.bookingStatus === "CANCELLED").length;
  const waiting  = bookings.filter((b) => b.bookingStatus === "WAITING" || b.bookingStatus === "PARTIAL").length;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        <Breadcrumb items={[{ label: "Dashboard", href: "/dashboard" }, { label: "My Bookings" }]} />

        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-[#3D2B1F] flex items-center gap-2">
              <BarChart3 className="w-6 h-6 text-[#D9874C]" />
              My Bookings
            </h1>
            <p className="text-[#7A6552] text-sm mt-1">Your complete Tatkal booking history</p>
          </div>
          <Button size="sm" variant="ghost" onClick={fetchHistory} loading={loading}>
            <RefreshCw className="w-4 h-4" />
            Refresh
          </Button>
        </div>

        {/* Summary */}
        <div className="grid grid-cols-3 gap-3 mb-6">
          {[
            { label: "Confirmed", value: confirmed, color: "#5A8A5A" },
            { label: "Waitlisted", value: waiting,   color: "#C17A30" },
            { label: "Cancelled",  value: cancelled,  color: "#A83232" },
          ].map((s) => (
            <Card key={s.label}>
              <CardBody className="!py-3 text-center">
                <div className="text-2xl font-bold" style={{ color: s.color }}>{s.value}</div>
                <div className="text-xs text-[#7A6552] font-medium">{s.label}</div>
              </CardBody>
            </Card>
          ))}
        </div>

        {loading ? (
          <Spinner className="py-20" />
        ) : error ? (
          <Alert type="error">{error}</Alert>
        ) : bookings.length === 0 ? (
          <Card>
            <CardBody className="text-center py-16">
              <BarChart3 className="w-12 h-12 text-[#D5C9A8] mx-auto mb-3" />
              <p className="text-[#7A6552] font-medium">No bookings found</p>
              <Button size="sm" variant="outline" className="mt-3" onClick={() => router.push("/search")}>
                Book Your First Ticket
              </Button>
            </CardBody>
          </Card>
        ) : (
          <div className="space-y-3">
            {bookings.map((b) => <BookingCard key={b.pnr} item={b} />)}
          </div>
        )}
      </div>
    </div>
  );
}
