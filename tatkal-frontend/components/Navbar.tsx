"use client";

import { useAuth } from "@/context/AuthContext";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import {
  Train, Search, Ticket, X, ClockIcon, Users, LogOut,
  BarChart3, Home, ChevronRight
} from "lucide-react";

const NAV_ITEMS = [
  { href: "/dashboard", label: "Dashboard", icon: Home },
  { href: "/search",    label: "Search Trains", icon: Search },
  { href: "/book",      label: "Book Ticket", icon: Ticket },
  { href: "/cancel",    label: "Cancel Ticket", icon: X },
  { href: "/history",   label: "My Bookings", icon: BarChart3 },
  { href: "/election",  label: "Bully Election", icon: Users },
  { href: "/clock",     label: "Clock Sync", icon: ClockIcon },
];

export default function Navbar() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const handleLogout = () => {
    logout();
    router.push("/");
  };

  if (!user) return null;

  return (
    <nav className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-[#D5C9A8] shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/dashboard" className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl bg-[#D9874C] flex items-center justify-center shadow-sm group-hover:shadow-md transition-shadow">
              <Train className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="text-sm font-bold text-[#3D2B1F] leading-none">Tatkal</div>
              <div className="text-[10px] text-[#7A6552] font-medium leading-none mt-0.5">Reservation System</div>
            </div>
          </Link>

          {/* Nav Links */}
          <div className="hidden lg:flex items-center gap-1">
            {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
              const active = pathname === href || pathname.startsWith(href + "/");
              return (
                <Link
                  key={href}
                  href={href}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200
                    ${active
                      ? "bg-[#D9874C]/10 text-[#D9874C]"
                      : "text-[#7A6552] hover:text-[#3D2B1F] hover:bg-[#F0EDD8]"
                    }`}
                >
                  <Icon className="w-4 h-4" />
                  {label}
                </Link>
              );
            })}
          </div>

          {/* User + Logout */}
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-[#8FAB7E] flex items-center justify-center text-white text-xs font-semibold">
                {user.fullName.charAt(0).toUpperCase()}
              </div>
              <div className="hidden md:block">
                <div className="text-xs font-semibold text-[#3D2B1F] leading-none">{user.fullName}</div>
                <div className="text-[10px] text-[#7A6552] leading-none mt-0.5">ID: {user.userId}</div>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium text-[#A83232] hover:bg-[#A83232]/10 transition-all duration-200"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>

        {/* Mobile nav */}
        <div className="lg:hidden flex items-center gap-1 pb-2 overflow-x-auto scrollbar-hide">
          {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
            const active = pathname === href;
            return (
              <Link
                key={href}
                href={href}
                className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all
                  ${active ? "bg-[#D9874C]/10 text-[#D9874C]" : "text-[#7A6552] hover:bg-[#F0EDD8]"}`}
              >
                <Icon className="w-3.5 h-3.5" />
                {label}
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}

// Breadcrumb helper
export function Breadcrumb({ items }: { items: { label: string; href?: string }[] }) {
  return (
    <nav className="flex items-center gap-1.5 text-xs text-[#7A6552] mb-6">
      {items.map((item, i) => (
        <span key={i} className="flex items-center gap-1.5">
          {item.href ? (
            <Link href={item.href} className="hover:text-[#D9874C] transition-colors">{item.label}</Link>
          ) : (
            <span className="text-[#3D2B1F] font-medium">{item.label}</span>
          )}
          {i < items.length - 1 && <ChevronRight className="w-3 h-3" />}
        </span>
      ))}
    </nav>
  );
}
