"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { Card, CardBody, Button, Input, Alert } from "@/components/ui";
import { Train, Mail, Lock, ArrowRight } from "lucide-react";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch("/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error ?? "Login failed.");
      } else {
        login(data);
        router.push("/dashboard");
      }
    } catch {
      setError("Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#FEFDF0] px-4">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-[#D9874C]/8 blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-[#8FAB7E]/8 blur-3xl" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-[#A5BF96]/5 blur-3xl" />
      </div>

      <div className="w-full max-w-md relative animate-slide-up">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-[#D9874C] shadow-lg mb-4">
            <Train className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-[#3D2B1F]">Welcome Back</h1>
          <p className="text-[#7A6552] mt-1.5 text-sm">Sign in to your Tatkal account</p>
        </div>

        <Card>
          <CardBody className="p-8">
            <form onSubmit={handleSubmit} className="space-y-5">
              {error && <Alert type="error">{error}</Alert>}

              <Input
                label="Email Address"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                icon={<Mail className="w-4 h-4" />}
                required
              />

              <Input
                label="Password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                icon={<Lock className="w-4 h-4" />}
                required
              />

              <Button type="submit" size="lg" className="w-full mt-2" loading={loading}>
                Sign In
                <ArrowRight className="w-4 h-4" />
              </Button>
            </form>

            <div className="mt-6 pt-5 border-t border-[#D5C9A8]">
              <p className="text-xs text-[#7A6552] text-center mb-3">Demo credentials</p>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { name: "Devraj", email: "devraj@example.com" },
                  { name: "Rahul", email: "rahul@example.com" },
                ].map((u) => (
                  <button
                    key={u.email}
                    onClick={() => { setEmail(u.email); setPassword("password123"); }}
                    className="text-xs px-3 py-2 rounded-lg bg-[#F0EDD8] text-[#7A6552] hover:bg-[#D9874C]/10 hover:text-[#D9874C] transition-all text-left"
                  >
                    <div className="font-medium">{u.name}</div>
                    <div className="opacity-70 truncate">{u.email}</div>
                  </button>
                ))}
              </div>
            </div>
          </CardBody>
        </Card>

        <p className="text-center text-xs text-[#7A6552] mt-6">
          Distributed Tatkal Reservation System · All rights reserved
        </p>
      </div>
    </div>
  );
}
