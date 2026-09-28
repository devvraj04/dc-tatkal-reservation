"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar, { Breadcrumb } from "@/components/Navbar";
import { Card, CardBody, Button, Input, Alert } from "@/components/ui";
import { X, AlertTriangle, CheckCircle } from "lucide-react";

export default function CancelPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [pnr, setPnr] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<{
    success: boolean; pnr: string; refundAmount: number; cancellationCharge: number; message: string;
  } | null>(null);

  useEffect(() => { if (!user) router.push("/"); }, [user, router]);

  const handleCancel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pnr.trim() || !user) return;
    setError("");
    setLoading(true);
    setResult(null);

    try {
      const res = await fetch("/api/cancel", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pnr: pnr.trim(), userId: user.userId }),
      });
      const data = await res.json();
      if (!res.ok) setError(data.error ?? "Cancellation failed.");
      else setResult(data);
    } catch {
      setError("Network error.");
    } finally {
      setLoading(false);
    }
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-xl mx-auto px-4 sm:px-6 py-8">
        <Breadcrumb items={[{ label: "Dashboard", href: "/dashboard" }, { label: "Cancel Ticket" }]} />

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#3D2B1F] flex items-center gap-2">
            <X className="w-6 h-6 text-[#A83232]" />
            Cancel Ticket
          </h1>
          <p className="text-[#7A6552] text-sm mt-1">Cancel a confirmed booking and receive your refund</p>
        </div>

        {/* Cancellation policy */}
        <Card className="mb-5 border-[#C17A30]/30 bg-[#C17A30]/5">
          <CardBody className="!py-3">
            <div className="flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 text-[#C17A30] flex-shrink-0 mt-0.5" />
              <div className="text-xs text-[#C17A30]">
                <span className="font-semibold">Cancellation Policy:</span> A flat cancellation charge of ₹100 per passenger applies.
                Refund = Total Fare − (₹100 × No. of passengers). Waitlisted passengers will be automatically promoted.
              </div>
            </div>
          </CardBody>
        </Card>

        {result ? (
          <Card className="animate-slide-up border-[#5A8A5A]/30 bg-[#5A8A5A]/5">
            <CardBody>
              <div className="flex items-start gap-3">
                <CheckCircle className="w-6 h-6 text-[#5A8A5A] flex-shrink-0" />
                <div>
                  <h3 className="font-bold text-[#3D2B1F]">Cancellation Successful!</h3>
                  <p className="text-sm text-[#7A6552] mt-1">{result.message}</p>
                  <div className="grid grid-cols-3 gap-3 mt-4">
                    {[
                      { label: "PNR", value: result.pnr },
                      { label: "Cancellation Charge", value: `₹${result.cancellationCharge.toFixed(2)}` },
                      { label: "Refund Amount", value: `₹${result.refundAmount.toFixed(2)}` },
                    ].map((s) => (
                      <div key={s.label} className="bg-white rounded-xl p-3 border border-[#D5C9A8] text-center">
                        <div className="text-[10px] text-[#7A6552] uppercase tracking-wide">{s.label}</div>
                        <div className="font-bold text-[#3D2B1F] mt-0.5">{s.value}</div>
                      </div>
                    ))}
                  </div>
                  <div className="flex gap-3 mt-4">
                    <Button size="sm" variant="ghost" onClick={() => { setResult(null); setPnr(""); }}>Cancel Another</Button>
                    <Button size="sm" variant="secondary" onClick={() => router.push("/history")}>View History</Button>
                  </div>
                </div>
              </div>
            </CardBody>
          </Card>
        ) : (
          <Card>
            <CardBody>
              <form onSubmit={handleCancel} className="space-y-4">
                {error && <Alert type="error">{error}</Alert>}
                <Input
                  label="PNR Number"
                  placeholder="e.g. 1023456789"
                  value={pnr}
                  onChange={(e) => setPnr(e.target.value)}
                  required
                />
                <div className="text-xs text-[#7A6552] bg-[#F0EDD8] rounded-lg px-3 py-2">
                  Your User ID (<strong>{user.userId}</strong>) will be verified against the booking.
                </div>
                <Button type="submit" variant="danger" loading={loading} className="w-full">
                  <X className="w-4 h-4" />
                  Cancel Ticket
                </Button>
              </form>
            </CardBody>
          </Card>
        )}
      </div>
    </div>
  );
}
