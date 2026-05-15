import { CSSProperties, HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const CARD_BASE_STYLE: CSSProperties = {
  backdropFilter: "blur(40px)",
  WebkitBackdropFilter: "blur(40px)",
  backgroundColor: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.1)",
  boxShadow: "0 24px 80px rgba(0,0,0,0.22), inset 0 1px 0 rgba(255,255,255,0.08)",
};

export function Card({ className, style, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("rounded-[2rem] overflow-hidden", className)}
      style={{ ...CARD_BASE_STYLE, ...style }}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("flex flex-col gap-2 p-6", className)} {...props} />;
}

export function CardTitle({ className, style, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3
      className={cn("text-lg font-semibold tracking-tight", className)}
      style={{ color: "#f4f4f5", ...style }}
      {...props}
    />
  );
}

export function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("p-6 pt-0", className)} {...props} />;
}
