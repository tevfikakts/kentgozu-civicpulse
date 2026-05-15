"use client";

import { ComponentProps } from "react";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

export function BentoCard({ className, ...props }: ComponentProps<typeof motion.div>) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 18, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ type: "spring", stiffness: 220, damping: 26 }}
      className={cn("relative z-10", className)}
      {...props}
    />
  );
}
