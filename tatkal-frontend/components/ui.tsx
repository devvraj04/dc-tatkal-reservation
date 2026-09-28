import React from "react";

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  className?: string;
}

export function Card({ children, className = "", ...props }: CardProps) {
  return (
    <div
      className={`bg-white rounded-2xl border border-[#D5C9A8] shadow-[0_2px_12px_rgba(61,43,31,0.08)] ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function CardHeader({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`px-6 py-4 border-b border-[#D5C9A8] ${className}`}>
      {children}
    </div>
  );
}

export function CardBody({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`px-6 py-5 ${className}`}>
      {children}
    </div>
  );
}

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger" | "ghost" | "outline";
  size?: "sm" | "md" | "lg";
  loading?: boolean;
}

export function Button({
  children, variant = "primary", size = "md", loading = false, className = "", disabled, ...props
}: ButtonProps) {
  const base = "inline-flex items-center justify-center gap-2 font-semibold rounded-xl transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed";
  const sizes = { sm: "px-3 py-1.5 text-xs", md: "px-5 py-2.5 text-sm", lg: "px-7 py-3.5 text-base" };
  const variants = {
    primary:   "bg-[#D9874C] text-white hover:bg-[#C17A3E] focus:ring-[#D9874C] shadow-sm hover:shadow-md active:scale-[0.98]",
    secondary: "bg-[#8FAB7E] text-white hover:bg-[#7A9669] focus:ring-[#8FAB7E] shadow-sm hover:shadow-md active:scale-[0.98]",
    danger:    "bg-[#A83232] text-white hover:bg-[#8C2828] focus:ring-[#A83232] shadow-sm hover:shadow-md active:scale-[0.98]",
    ghost:     "text-[#7A6552] hover:bg-[#F0EDD8] hover:text-[#3D2B1F] focus:ring-[#D5C9A8]",
    outline:   "border-2 border-[#D9874C] text-[#D9874C] hover:bg-[#D9874C]/10 focus:ring-[#D9874C] active:scale-[0.98]",
  };
  return (
    <button
      className={`${base} ${sizes[size]} ${variants[variant]} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading && (
        <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      )}
      {children}
    </button>
  );
}

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  icon?: React.ReactNode;
}

export function Input({ label, error, icon, className = "", ...props }: InputProps) {
  return (
    <div className="space-y-1.5">
      {label && <label className="block text-sm font-medium text-[#3D2B1F]">{label}</label>}
      <div className="relative">
        {icon && <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[#7A6552]">{icon}</span>}
        <input
          className={`w-full rounded-xl border border-[#D5C9A8] bg-white px-4 py-2.5 text-sm text-[#3D2B1F] placeholder:text-[#B0A090]
            focus:outline-none focus:ring-2 focus:ring-[#D9874C]/40 focus:border-[#D9874C] transition-all
            disabled:bg-[#F5F3E0] disabled:cursor-not-allowed
            ${icon ? "pl-10" : ""} ${error ? "border-[#A83232] focus:ring-[#A83232]/40 focus:border-[#A83232]" : ""}
            ${className}`}
          {...props}
        />
      </div>
      {error && <p className="text-xs text-[#A83232]">{error}</p>}
    </div>
  );
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
}

export function Select({ label, error, className = "", children, ...props }: SelectProps) {
  return (
    <div className="space-y-1.5">
      {label && <label className="block text-sm font-medium text-[#3D2B1F]">{label}</label>}
      <select
        className={`w-full rounded-xl border border-[#D5C9A8] bg-white px-4 py-2.5 text-sm text-[#3D2B1F]
          focus:outline-none focus:ring-2 focus:ring-[#D9874C]/40 focus:border-[#D9874C] transition-all
          ${error ? "border-[#A83232]" : ""} ${className}`}
        {...props}
      >
        {children}
      </select>
      {error && <p className="text-xs text-[#A83232]">{error}</p>}
    </div>
  );
}

interface BadgeProps {
  children: React.ReactNode;
  variant?: "success" | "warning" | "danger" | "info" | "neutral" | "primary";
  className?: string;
}

export function Badge({ children, variant = "neutral", className = "" }: BadgeProps) {
  const variants = {
    success: "bg-[#5A8A5A]/10 text-[#5A8A5A] border-[#5A8A5A]/20",
    warning: "bg-[#C17A30]/10 text-[#C17A30] border-[#C17A30]/20",
    danger:  "bg-[#A83232]/10 text-[#A83232] border-[#A83232]/20",
    info:    "bg-[#3A6EA8]/10 text-[#3A6EA8] border-[#3A6EA8]/20",
    neutral: "bg-[#7A6552]/10 text-[#7A6552] border-[#7A6552]/20",
    primary: "bg-[#D9874C]/10 text-[#D9874C] border-[#D9874C]/20",
  };
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border ${variants[variant]} ${className}`}>
      {children}
    </span>
  );
}

export function Alert({
  children, type = "info", className = ""
}: { children: React.ReactNode; type?: "success" | "error" | "warning" | "info"; className?: string }) {
  const styles = {
    success: "bg-[#5A8A5A]/10 border-[#5A8A5A]/30 text-[#5A8A5A]",
    error:   "bg-[#A83232]/10 border-[#A83232]/30 text-[#A83232]",
    warning: "bg-[#C17A30]/10 border-[#C17A30]/30 text-[#C17A30]",
    info:    "bg-[#3A6EA8]/10 border-[#3A6EA8]/30 text-[#3A6EA8]",
  };
  return (
    <div className={`rounded-xl border px-4 py-3 text-sm font-medium ${styles[type]} ${className}`}>
      {children}
    </div>
  );
}

export function Spinner({ className = "" }: { className?: string }) {
  return (
    <div className={`flex items-center justify-center ${className}`}>
      <svg className="animate-spin h-8 w-8 text-[#D9874C]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const map: Record<string, "success" | "warning" | "danger" | "info" | "neutral"> = {
    CONFIRMED: "success", PARTIAL: "warning", WAITING: "info",
    CANCELLED: "danger", PAYMENT_FAILED: "danger", PENDING: "neutral",
  };
  return <Badge variant={map[status?.toUpperCase()] ?? "neutral"}>{status}</Badge>;
}
