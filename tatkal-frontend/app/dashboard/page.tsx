"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import { Card, CardBody, Badge, Spinner } from "@/components/ui";
import { Train, Search, Ticket, X, Clock, Users, ArrowRight, Crown } from "lucide-react";
import Link from "next/link";

interface Leader {
  userId: number;
  fullName: string;
  email: string;
}

const QUICK_ACTIONS = [
  { href: "/search",   icon: Search,  label: "Search Trains",  desc: "Find trains by station & date",    color: "#D9874C" },
  { href: "/book",     icon: Ticket,  label: "Book Ticket",    desc: "Reserve Tatkal seats instantly",   color: "#8FAB7E" },
  { href: "/cancel",   icon: X,       label: "Cancel Ticket",  desc: "Cancel and get refund",            color: "#A83232" },
  { href: "/history",  icon: Train,   label: "My Bookings",    desc: "View your booking history",        color: "#3A6EA8" },
  { href: "/election", icon: Users,   label: "Bully Election", desc: "Distributed leader election",      color: "#7A6552" },
  { href: "/clock",    icon: Clock,   label: "Clock Sync",     desc: "Cristian's & Lamport clocks",      color: "#C17A30" },
];

export default function DashboardPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [leader, setLeader] = useState<Leader | null | undefined>(undefined);

  useEffect(() => {
    if (!user) { router.push("/"); return; }
    fetch("/api/leader")
      .then((r) => r.json())
      .then((d) => setLeader(d))
      .catch(() => setLeader(null));
  }, [user, router]);

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#FEFDF0]">
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">

        {/* Hero greeting */}
        <div className="mb-8 animate-slide-up">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h1 className="text-3xl font-bold text-[#3D2B1F]">
                Welcome, <span className="text-[#D9874C]">{user.fullName.split(" ")[0]}</span> 👋
              </h1>
              <p className="text-[#7A6552] mt-1">
                User ID: <span className="font-semibold text-[#3D2B1F]">#{user.userId}</span> ·{" "}
                <span className="text-[#8FAB7E] font-medium">{user.email}</span>
              </p>
            </div>

            {/* Leader card */}
            <Card className="sm:w-auto w-full !rounded-xl">
              <CardBody className="!py-3 !px-4">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-[#D9874C]/10 flex items-center justify-center">
                    <Crown className="w-4 h-4 text-[#D9874C]" />
                  </div>
                  <div>
                    <div className="text-[10px] text-[#7A6552] font-medium uppercase tracking-wide">Current Leader</div>
                    {leader === undefined ? (
                      <div className="text-xs text-[#7A6552]">Loading…</div>
                    ) : leader ? (
                      <div className="text-sm font-semibold text-[#3D2B1F]">
                        {leader.fullName}
                        <span className="ml-1.5">
                          <Badge variant="success" className="text-[10px]">Active</Badge>
                        </span>
                      </div>
                    ) : (
                      <div className="text-sm text-[#A83232] font-medium">No leader elected</div>
                    )}
                  </div>
                </div>
              </CardBody>
            </Card>
          </div>
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
          {[
            { label: "Trains Available", value: "5+", color: "#D9874C" },
            { label: "Stations", value: "10", color: "#8FAB7E" },
            { label: "Coach Classes", value: "4", color: "#3A6EA8" },
            { label: "Booking Mode", value: "TATKAL", color: "#C17A30" },
          ].map((s) => (
            <Card key={s.label} className="animate-fade-in">
              <CardBody className="!py-4">
                <div className="text-2xl font-bold" style={{ color: s.color }}>{s.value}</div>
                <div className="text-xs text-[#7A6552] font-medium mt-0.5">{s.label}</div>
              </CardBody>
            </Card>
          ))}
        </div>

        {/* Quick actions */}
        <h2 className="text-lg font-bold text-[#3D2B1F] mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {QUICK_ACTIONS.map(({ href, icon: Icon, label, desc, color }) => (
            <Link key={href} href={href}>
              <Card className="hover:shadow-[0_8px_32px_rgba(61,43,31,0.12)] hover:-translate-y-0.5 transition-all duration-200 cursor-pointer h-full">
                <CardBody className="!py-5">
                  <div className="flex items-start gap-4">
                    <div
                      className="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0"
                      style={{ background: `${color}18` }}
                    >
                      <Icon className="w-5 h-5" style={{ color }} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-semibold text-[#3D2B1F] text-sm">{label}</div>
                      <div className="text-xs text-[#7A6552] mt-0.5">{desc}</div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-[#D5C9A8] flex-shrink-0 mt-0.5" />
                  </div>
                </CardBody>
              </Card>
            </Link>
          ))}
        </div>

      </div>
    </div>
  );
}
