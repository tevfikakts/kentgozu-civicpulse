"use client";

import { motion } from "framer-motion";
import { AlertTriangle, ArrowUpRight, RadioTower } from "lucide-react";
import { TicketCreatedResponse } from "@/lib/api-types";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { BentoCard } from "@/components/dashboard/bento-card";

export function HeroTile({ ticket }: { ticket: TicketCreatedResponse }) {
  return (
    <BentoCard className="md:col-span-7 lg:col-span-8 lg:row-span-2">
      <Card
        className="relative h-full"
        style={{ backgroundColor: "rgba(9,9,11,0.92)", color: "#f4f4f5" }}
      >
        <div className="absolute" style={{ inset: 0, pointerEvents: "none", background: "radial-gradient(circle at 20% 10%, rgba(34,197,94,0.30), transparent 35%), radial-gradient(circle at 90% 20%, rgba(59,130,246,0.25), transparent 30%)" }} />
        <div className="absolute" style={{ left: "-4rem", top: "-5rem", height: "16rem", width: "16rem", borderRadius: "50%", pointerEvents: "none", background: "rgba(52,211,153,0.18)", filter: "blur(56px)" }} />
        <div className="absolute" style={{ right: "-5rem", top: "0", height: "18rem", width: "18rem", borderRadius: "50%", pointerEvents: "none", background: "rgba(59,130,246,0.14)", filter: "blur(56px)" }} />
        <CardContent className="relative flex h-full min-h-[390px] flex-col justify-between p-8">
          <div className="flex items-start justify-between gap-4">
            <Badge
              style={{
                backgroundColor: "rgba(52,211,153,0.15)",
                color: "rgb(167,243,208)",
                border: "1px solid rgba(52,211,153,0.3)",
              }}
            >
              <RadioTower className="mr-2 h-3.5 w-3.5" />
              Canlı Operasyon
            </Badge>
            <motion.div
              layout
              className="rounded-full p-3"
              style={{
                backgroundColor: "rgba(255,255,255,0.08)",
                border: "1px solid rgba(255,255,255,0.12)",
                color: "#f4f4f5",
              }}
            >
              <ArrowUpRight className="h-5 w-5" />
            </motion.div>
          </div>
          <div className="max-w-2xl">
            <div className="mb-5 flex items-center gap-3 text-sm font-medium" style={{ color: "rgb(148,163,184)" }}>
              <AlertTriangle className="h-4 w-4" style={{ color: "rgb(252,211,77)" }} />
              #{ticket.id} · {ticket.status}
            </div>
            <h1 className="text-4xl font-semibold tracking-tight md:text-6xl" style={{ color: "#f4f4f5" }}>{ticket.title}</h1>
            <p className="mt-5 max-w-xl text-base leading-7" style={{ color: "rgb(148,163,184)" }}>
              {ticket.description}
            </p>
          </div>
          <div className="grid grid-cols-2 gap-4 text-sm md:grid-cols-4">
            {[
              ["Muhbir", ticket.reporterEmail],
              ["Enlem", ticket.latitude.toFixed(4)],
              ["Boylam", ticket.longitude.toFixed(4)],
              ["Açılış", new Intl.DateTimeFormat("tr-TR", { hour: "2-digit", minute: "2-digit" }).format(new Date(ticket.createdAt))],
            ].map(([label, value]) => (
              <motion.div
                key={label}
                layout
                className="rounded-2xl p-4"
                style={{
                  backdropFilter: "blur(24px)",
                  WebkitBackdropFilter: "blur(24px)",
                  backgroundColor: "rgba(255,255,255,0.06)",
                  border: "1px solid rgba(255,255,255,0.12)",
                  boxShadow: "inset 0 1px 0 rgba(255,255,255,0.12), 0 18px 40px rgba(0,0,0,0.26)",
                }}
              >
                <div className="text-xs font-medium" style={{ color: "rgb(148,163,184)" }}>{label}</div>
                <div className="mt-1 truncate font-semibold" style={{ color: "#f4f4f5" }}>{value}</div>
              </motion.div>
            ))}
          </div>
        </CardContent>
      </Card>
    </BentoCard>
  );
}
