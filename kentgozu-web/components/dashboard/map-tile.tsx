"use client";

import { motion } from "framer-motion";
import { MapPinned } from "lucide-react";
import { NearbyTicketResponse } from "@/lib/api-types";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BentoCard } from "@/components/dashboard/bento-card";

export function MapTile({ tickets }: { tickets: NearbyTicketResponse[] }) {
  return (
    <BentoCard className="md:col-span-6 lg:col-span-5">
      <Card className="h-full overflow-hidden">
        <CardHeader className="flex-row items-center justify-between">
          <div>
            <CardTitle>Uzamsal Yoğunluk</CardTitle>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">WGS 84 koordinat akışı</p>
          </div>
          <Badge>
            <MapPinned className="mr-2 h-3.5 w-3.5" />
            Ankara
          </Badge>
        </CardHeader>
        <CardContent>
          <div className="relative h-72 overflow-hidden rounded-[1.5rem] bg-zinc-100 dark:bg-zinc-900">
            <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(113,113,122,.18)_1px,transparent_1px),linear-gradient(to_bottom,rgba(113,113,122,.18)_1px,transparent_1px)] bg-[size:36px_36px] pointer-events-none" />
            {tickets.map((ticket, index) => (
              <motion.div
                layout
                key={ticket.id}
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: index * 0.08, type: "spring", stiffness: 260, damping: 18 }}
                className="absolute"
                style={{ left: `${24 + index * 22}%`, top: `${28 + (index % 2) * 24}%` }}
              >
                <div className="relative">
                  <span className="absolute inline-flex h-8 w-8 animate-ping rounded-full bg-emerald-400/40" />
                  <span className="relative inline-flex h-8 w-8 items-center justify-center rounded-full bg-emerald-500 text-xs font-bold text-white">
                    {index + 1}
                  </span>
                </div>
              </motion.div>
            ))}
            <div className="absolute bottom-4 left-4 right-4 rounded-2xl border border-white/50 bg-white/80 p-4 backdrop-blur dark:border-white/10 dark:bg-zinc-950/70">
              <div className="text-sm font-medium">{tickets.length} aktif yakın ihbar</div>
              <div className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                Harita kartı canlı konumsal olaylar için ayrıldı.
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </BentoCard>
  );
}
