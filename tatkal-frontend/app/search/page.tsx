"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar, { Breadcrumb } from "@/components/Navbar";
import { Card, CardHeader, CardBody, Button, Input, Alert, Badge } from "@/components/ui";
import { Search, Train, ArrowRight, Clock, MapPin } from "lucide-react";

interface TrainResult {
  scheduleId: number;
  trainNo: number;
  trainName: string;
  departureTime: string;
  arrivalTime: string;
  dayDiff: number;
}

interface Station {
  code: string;
  name: string;
  city: string;
}

export default function SearchPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [stations, setStations] = useState<Station[]>([]);
  const [source, setSource] = useState("");
  const [destination, setDestination] = useState("");
  const [date, setDate] = useState("");
  const [results, setResults] = useState<TrainResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    if (!user) router.push("/");
    // Fetch stations from database
    fetch("/api/stations")
      .then((res) => res.json())
      .then((data: Station[]) => {
        if (Array.isArray(data) && data.length > 0) {
          setStations(data);
          setSource(data[0].code);
          if (data.length > 1) setDestination(data[1].code);
        }
      })
      .catch(() => {});

    // Default date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    setDate(tomorrow.toISOString().split("T")[0]);
  }, [user, router]);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (source === destination) { setError("Source and destination cannot be the same."); return; }
    setError("");
    setLoading(true);
    setSearched(false);

    try {
      const res = await fetch(`/api/search-trains?source=${source}&destination=${destination}&date=${date}`);
      const data = await res.json();
      if (!res.ok) { setError(data.error ?? "Search failed."); setResults([]); }
      else { setResults(data); setSearched(true); }
    } catch {
      setError("Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleSelectTrain = (t: TrainResult) => {
    sessionStorage.setItem("selected_train", JSON.stringify(t));
    router.push("/book");
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        <Breadcrumb items={[{ label: "Dashboard", href: "/dashboard" }, { label: "Search Trains" }]} />

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#3D2B1F] flex items-center gap-2">
            <Search className="w-6 h-6 text-[#D9874C]" />
            Search Trains
          </h1>
          <p className="text-[#7A6552] text-sm mt-1">Find available Tatkal trains between stations</p>
        </div>

        <Card className="mb-6">
          <CardBody>
            <form onSubmit={handleSearch} className="space-y-4">
              {error && <Alert type="error">{error}</Alert>}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-[#3D2B1F]">From Station</label>
                  <select
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                    className="w-full rounded-xl border border-[#D5C9A8] bg-white px-4 py-2.5 text-sm text-[#3D2B1F] focus:outline-none focus:ring-2 focus:ring-[#D9874C]/40 focus:border-[#D9874C] transition-all"
                  >
                    {stations.map((s) => (
                      <option key={s.code} value={s.code}>{s.code} – {s.name}</option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-[#3D2B1F]">To Station</label>
                  <select
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                    className="w-full rounded-xl border border-[#D5C9A8] bg-white px-4 py-2.5 text-sm text-[#3D2B1F] focus:outline-none focus:ring-2 focus:ring-[#D9874C]/40 focus:border-[#D9874C] transition-all"
                  >
                    {stations.map((s) => (
                      <option key={s.code} value={s.code}>{s.code} – {s.name}</option>
                    ))}
                  </select>
                </div>

                <Input
                  label="Journey Date"
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  min={new Date().toISOString().split("T")[0]}
                />
              </div>

              <Button type="submit" loading={loading} className="w-full sm:w-auto">
                <Search className="w-4 h-4" />
                Search Trains
              </Button>
            </form>
          </CardBody>
        </Card>

        {/* Results */}
        {searched && (
          <div className="animate-slide-up">
            {results.length === 0 ? (
              <Card>
                <CardBody className="text-center py-12">
                  <Train className="w-12 h-12 text-[#D5C9A8] mx-auto mb-3" />
                  <p className="text-[#7A6552] font-medium">No trains found for this route and date.</p>
                  <p className="text-xs text-[#7A6552] mt-1">Try a different date or station combination.</p>
                </CardBody>
              </Card>
            ) : (
              <>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-base font-semibold text-[#3D2B1F]">
                    {results.length} train{results.length !== 1 ? "s" : ""} found
                  </h2>
                  <Badge variant="primary">{source} → {destination}</Badge>
                </div>
                <div className="space-y-3">
                  {results.map((t) => (
                    <Card key={t.scheduleId} className="hover:shadow-[0_8px_32px_rgba(61,43,31,0.12)] transition-all duration-200">
                      <CardBody>
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                          <div className="flex items-start gap-4">
                            <div className="w-11 h-11 rounded-xl bg-[#D9874C]/10 flex items-center justify-center flex-shrink-0">
                              <Train className="w-5 h-5 text-[#D9874C]" />
                            </div>
                            <div>
                              <div className="font-semibold text-[#3D2B1F]">{t.trainName}</div>
                              <div className="text-xs text-[#7A6552] mt-0.5">Train #{t.trainNo} · Schedule ID: {t.scheduleId}</div>
                              <div className="flex items-center gap-3 mt-2">
                                <span className="flex items-center gap-1 text-sm font-medium text-[#3D2B1F]">
                                  <Clock className="w-3.5 h-3.5 text-[#D9874C]" />
                                  {t.departureTime}
                                </span>
                                <ArrowRight className="w-4 h-4 text-[#D5C9A8]" />
                                <span className="flex items-center gap-1 text-sm font-medium text-[#3D2B1F]">
                                  {t.arrivalTime}
                                  {t.dayDiff > 0 && (
                                    <span className="text-xs text-[#D9874C] font-bold ml-0.5">+{t.dayDiff}</span>
                                  )}
                                </span>
                              </div>
                            </div>
                          </div>
                          <div className="flex items-center gap-3">
                            <Badge variant="success">SCHEDULED</Badge>
                            <Button size="sm" onClick={() => handleSelectTrain(t)}>
                              Select
                              <ArrowRight className="w-3.5 h-3.5" />
                            </Button>
                          </div>
                        </div>
                      </CardBody>
                    </Card>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
