"use client";

import { useEffect, useRef, useState } from "react";
import { TicketLiveEvent } from "@/lib/api-types";

const WS_ENDPOINT =
  (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080") + "/ws";

const MAX_LIVE_TICKETS = 20;

export interface UseStompResult {
  liveTickets: TicketLiveEvent[];
  connected: boolean;
}

/**
 * STOMP/SockJS bağlantısı kurar.
 *
 * Güvenlik notu: token, standart WebSocket API HTTP başlığı taşıyamadığından
 * URL query param'ı olarak DEĞİL, STOMP CONNECT frame'inin connectHeaders
 * bloğuna "Authorization: Bearer <token>" olarak yerleştirilir.
 */
export function useStomp(token: string | null): UseStompResult {
  const [liveTickets, setLiveTickets] = useState<TicketLiveEvent[]>([]);
  const [connected, setConnected] = useState(false);
  // client ref: async IIFE tamamlanmadan cleanup çalışsa bile deactivate garantili
  const clientRef = useRef<{ deactivate: () => Promise<void> } | null>(null);

  useEffect(() => {
    // token null/boş olsa bile bağlan:
    // backend /ws/** permitAll, WebSocketAuthChannelInterceptor header yoksa geçiriyor.
    // Token varsa STOMP CONNECT frame'ine Authorization header eklenir.
    let cancelled = false;

    (async () => {
      const [{ Client }, { default: SockJS }] = await Promise.all([
        import("@stomp/stompjs"),
        import("sockjs-client"),
      ]);

      if (cancelled) return;

      const connectHeaders: Record<string, string> = token
        ? { Authorization: `Bearer ${token}` }
        : {};

      const client = new Client({
        webSocketFactory: () => new SockJS(WS_ENDPOINT) as WebSocket,
        connectHeaders,
        reconnectDelay: 5_000,
        debug: (msg) => console.debug("[STOMP]", msg),
        onConnect: () => {
          if (cancelled) return;
          console.info("[useStomp] Bağlantı kuruldu, /topic/tickets abone olunuyor");
          setConnected(true);
          client.subscribe("/topic/tickets", (message) => {
            // RAW log — herhangi bir işlemden önce, mesajın tarayıcıya girip girmediğini doğrular
            console.log("RAW STOMP MESSAGE:", message.body);

            if (cancelled) return;
            if (!message.body) {
              console.warn("[useStomp] Boş mesaj body");
              return;
            }
            try {
              const raw = JSON.parse(message.body) as Record<string, unknown>;
              if (typeof raw.id !== "number" || !raw.title) {
                console.warn("[useStomp] Eksik/geçersiz alan yapısı:", raw);
                return;
              }
              const event = raw as unknown as TicketLiveEvent;
              // Fonksiyonel güncelleme: stale closure yok.
              // Duplicate gelirse aynı prev ref'i döndür → React re-render'ı atlar.
              setLiveTickets((prev) => {
                if (prev.some((t) => t.id === event.id)) return prev;
                return [event, ...prev].slice(0, MAX_LIVE_TICKETS);
              });
            } catch (err) {
              console.error("[useStomp] Parse hatası:", err, "\nbody:", message.body);
            }
          });
        },
        onDisconnect: () => {
          console.info("[useStomp] Bağlantı kesildi");
          setConnected(false);
        },
        onStompError: (frame) => {
          console.error("[useStomp] STOMP hatası:", frame.headers?.["message"], frame);
        },
      });

      // clientRef ÖNCE set edilir, SONRA activate çağrılır.
      // Strict Mode'da cleanup, activate() ile assignment arasındaki mikro-pencerede
      // çalışırsa clientRef null kalır → deactivate() no-op → onConnect cancelled=true
      // ile karşılaşır → subscription kurulmaz → sessiz düşüş.
      clientRef.current = client;
      client.activate();
    })();

    return () => {
      cancelled = true;
      setConnected(false);
      // clientRef üzerinden deactivate: async IIFE'den önce cleanup çalışsa bile güvenli
      clientRef.current?.deactivate();
      clientRef.current = null;
    };
  // token değişince (null → değer) yeni bağlantı, eski temizlenir
  }, [token]);

  return { liveTickets, connected };
}
