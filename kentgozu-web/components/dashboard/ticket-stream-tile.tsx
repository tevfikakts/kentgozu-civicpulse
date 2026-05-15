"use client";

import { CSSProperties } from "react";
import { motion } from "framer-motion";
import { NearbyTicketResponse } from "@/lib/api-types";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BentoCard } from "@/components/dashboard/bento-card";

function getStatusStyle(status: string): CSSProperties {
  if (status === "OPEN") {
    return {
      backgroundColor: "rgba(249,115,22,0.15)",
      color: "rgb(251,146,60)",
      border: "1px solid rgba(249,115,22,0.2)",
      boxShadow: "0 0 24px rgba(249,115,22,0.12)",
    };
  }
  if (status === "RESOLVED") {
    return {
      backgroundColor: "rgba(16,185,129,0.15)",
      color: "rgb(52,211,153)",
      border: "1px solid rgba(16,185,129,0.2)",
      boxShadow: "0 0 24px rgba(16,185,129,0.12)",
    };
  }
  return {
    backgroundColor: "rgba(255,255,255,0.04)",
    color: "rgb(161,161,170)",
    border: "1px solid rgba(255,255,255,0.1)",
  };
}

export function TicketStreamTile({ tickets }: { tickets: NearbyTicketResponse[] }) {
  return (
    <BentoCard className="md:col-span-6 lg:col-span-4">
      <Card className="h-full">
        <CardHeader>
          <CardTitle>Olay Akışı</CardTitle>
          <p className="text-sm" style={{ color: "rgb(100,116,139)" }}>Liste değil, önceliklendirilmiş operasyon şeritleri</p>
        </CardHeader>
        <CardContent className="space-y-3">
          {tickets.map((ticket, index) => (
            <motion.div
              layout
              key={ticket.id}
              initial={{ opacity: 0, x: 18 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.06 }}
              className="rounded-3xl p-4"
              style={{
                backdropFilter: "blur(24px)",
                WebkitBackdropFilter: "blur(24px)",
                backgroundColor: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.1)",
                boxShadow: "inset 0 1px 0 rgba(255,255,255,0.06), 0 16px 36px rgba(0,0,0,0.18)",
              }}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="font-medium" style={{ color: "#f4f4f5" }}>{ticket.title}</div>
                  <div className="mt-1 text-xs" style={{ color: "rgb(100,116,139)" }}>
                    {ticket.latitude.toFixed(4)}, {ticket.longitude.toFixed(4)}
                  </div>
                </div>
                <Badge style={getStatusStyle(ticket.status)}>{ticket.status}</Badge>
              </div>
            </motion.div>
          ))}
        </CardContent>
      </Card>
    </BentoCard>
  );
}
