"use client";

import { LucideIcon } from "lucide-react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BentoCard } from "@/components/dashboard/bento-card";

export function MetricTile({
  title,
  value,
  delta,
  icon: Icon,
}: {
  title: string;
  value: string;
  delta: string;
  icon: LucideIcon;
}) {
  return (
    <BentoCard className="md:col-span-3 lg:col-span-2">
      <Card className="h-full">
        <CardHeader className="flex-row items-center justify-between space-y-0 pb-3">
          <CardTitle className="text-sm text-zinc-500 dark:text-zinc-400">{title}</CardTitle>
          <div className="rounded-2xl bg-zinc-100 p-2 dark:bg-white/10">
            <Icon className="h-4 w-4" />
          </div>
        </CardHeader>
        <CardContent>
          <motion.div layout className="text-4xl font-semibold tracking-tight">
            {value}
          </motion.div>
          <p className="mt-3 text-sm text-emerald-600 dark:text-emerald-400">{delta}</p>
        </CardContent>
      </Card>
    </BentoCard>
  );
}
