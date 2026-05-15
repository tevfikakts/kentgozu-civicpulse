"use client";

import { useEffect, useMemo, useState } from "react";
import { Activity, Radio, ShieldCheck, Siren, Users } from "lucide-react";
import { motion } from "framer-motion";
import { currentUser, highlightedTicket, nearbyTickets } from "@/lib/dashboard-data";
import { NearbyTicketResponse } from "@/lib/api-types";
import { useStomp } from "@/lib/use-stomp";
import { HeroTile } from "@/components/dashboard/hero-tile";
import { MapTile } from "@/components/dashboard/map-tile";
import { MetricTile } from "@/components/dashboard/metric-tile";
import { TicketStreamTile } from "@/components/dashboard/ticket-stream-tile";

export function AdminDashboard() {
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    setToken(localStorage.getItem("kentgozu_token"));
  }, []);

  const { liveTickets, connected } = useStomp(token);

  const allTickets = useMemo<NearbyTicketResponse[]>(() => {
    if (!liveTickets?.length) return nearbyTickets;
    const liveAsNearby: NearbyTicketResponse[] = liveTickets.map((t) => ({
      id: t.id,
      title: t.title,
      status: t.status,
      latitude: t.latitude,
      longitude: t.longitude,
      createdAt: new Date().toISOString(),
    }));
    const liveIds = new Set(liveAsNearby.map((t) => t.id));
    const base = nearbyTickets.filter((t) => !liveIds.has(t.id));
    return [...liveAsNearby, ...base].slice(0, 10);
  }, [liveTickets]);

  return (
    <main
      className="min-h-screen overflow-hidden px-5 py-6 md:px-8 lg:px-10"
      style={{
        background:
          "radial-gradient(circle at 15% 10%, rgba(16,185,129,0.14), transparent 28%), radial-gradient(circle at 85% 0%, rgba(59,130,246,0.12), transparent 30%), linear-gradient(135deg, #09090b 0%, #0f172a 48%, #09090b 100%)",
        color: "#f4f4f5",
      }}
    >
      <div className="mx-auto max-w-7xl">
        <motion.header layout className="mb-8 flex flex-col justify-between gap-5 md:flex-row md:items-end">
          <div>
            <p className="text-sm font-medium uppercase tracking-[0.35em] flex items-center gap-2" style={{ color: "rgb(52,211,153)" }}>
              KentGözü Yönetici Paneli
              {connected && (
                <span className="flex items-center gap-1 text-xs font-normal" style={{ color: "rgb(52,211,153)" }}>
                  <Radio className="h-3 w-3 animate-pulse" />
                  Canlı
                </span>
              )}
            </p>
            <h1 className="mt-3 text-3xl font-semibold tracking-tight md:text-5xl" style={{ color: "#f4f4f5" }}>
              Şehir sinyallerini tek ekranda yönetin
            </h1>
          </div>
          <div
            className="rounded-full px-5 py-3 text-sm"
            style={{
              border: "1px solid rgba(255,255,255,0.1)",
              backgroundColor: "rgba(255,255,255,0.04)",
              color: "rgb(212,212,216)",
              backdropFilter: "blur(40px)",
              WebkitBackdropFilter: "blur(40px)",
            }}
          >
            {currentUser.displayName}
          </div>
        </motion.header>
        <motion.section layout className="grid auto-rows-[minmax(170px,auto)] grid-cols-1 gap-4 md:grid-cols-12">
          <HeroTile ticket={highlightedTicket} />
          <MetricTile title="Açık ihbar" value="128" delta="+18 son 24 saat" icon={Siren} />
          <MetricTile title="Saha ekibi" value="34" delta="9 ekip yönlendirildi" icon={Users} />
          <MetricTile title="SLA sağlığı" value="%94" delta="+4 puan iyileşme" icon={ShieldCheck} />
          <MetricTile title="AI güveni" value="%87" delta="Gemini analiz ort." icon={Activity} />
          <MapTile tickets={allTickets} />
          <TicketStreamTile tickets={allTickets} />
        </motion.section>
      </div>
    </main>
  );
}
