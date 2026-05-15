import { NearbyTicketResponse, TicketCreatedResponse, UserDTO } from "@/lib/api-types";

export const currentUser: UserDTO = {
  id: 1,
  email: "admin@kentgozu.com",
  displayName: "KentGözü Operasyon",
  createdAt: "2026-05-15T10:00:00Z",
};

export const highlightedTicket: TicketCreatedResponse = {
  id: 1042,
  title: "Kritik yol çökmesi algılandı",
  description: "Ana arterde trafik güvenliğini etkileyen çökme ve şerit daralması bildirildi.",
  status: "IN_PROGRESS",
  reporterEmail: "saha.ekibi@kentgozu.com",
  longitude: 32.8597,
  latitude: 39.9334,
  createdAt: "2026-05-15T09:26:00Z",
};

export const nearbyTickets: NearbyTicketResponse[] = [
  {
    id: 1038,
    title: "Arızalı trafik lambası",
    status: "OPEN",
    longitude: 32.8624,
    latitude: 39.9321,
    createdAt: "2026-05-15T08:12:00Z",
  },
  {
    id: 1039,
    title: "Kaldırım işgali",
    status: "RESOLVED",
    longitude: 32.8562,
    latitude: 39.9345,
    createdAt: "2026-05-15T08:38:00Z",
  },
  {
    id: 1041,
    title: "Su baskını riski",
    status: "OPEN",
    longitude: 32.8661,
    latitude: 39.9363,
    createdAt: "2026-05-15T09:04:00Z",
  },
];
