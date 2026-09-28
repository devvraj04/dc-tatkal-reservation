import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        amber: {
          warm: "#D9874C",
        },
        cream: {
          DEFAULT: "#FEFDF0",
          dark: "#F5F3E0",
        },
        sage: {
          DEFAULT: "#8FAB7E",
          light: "#A5BF96",
          muted: "#9DB08E",
        },
        tatkal: {
          primary:   "#D9874C",  // warm amber
          secondary: "#8FAB7E",  // sage green
          accent:    "#A5BF96",  // light sage
          bg:        "#FEFDF0",  // cream
          bgDark:    "#F0EDD8",  // slightly deeper cream
          text:      "#3D2B1F",  // deep brown
          textMuted: "#7A6552",  // muted brown
          border:    "#D5C9A8",  // warm beige border
          success:   "#5A8A5A",
          warning:   "#C17A30",
          danger:    "#A83232",
          info:      "#3A6EA8",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
      boxShadow: {
        card: "0 2px 12px rgba(61, 43, 31, 0.08)",
        elevated: "0 8px 32px rgba(61, 43, 31, 0.12)",
        glow: "0 0 20px rgba(217, 135, 76, 0.3)",
      },
      animation: {
        "fade-in": "fadeIn 0.3s ease-in-out",
        "slide-up": "slideUp 0.4s ease-out",
        "pulse-soft": "pulseSoft 2s ease-in-out infinite",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        slideUp: {
          "0%": { opacity: "0", transform: "translateY(16px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        pulseSoft: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0.6" },
        },
      },
    },
  },
  plugins: [],
};

export default config;
