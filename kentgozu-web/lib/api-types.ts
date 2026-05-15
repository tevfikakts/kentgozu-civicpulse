export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "REJECTED" | string;

export interface TicketCreateRequest {
  reporterEmail: string;
  title: string;
  description: string;
  status: string;
  longitude: number;
  latitude: number;
}

export interface TicketCreatedResponse {
  id: number;
  title: string;
  description: string;
  status: string;
  reporterEmail: string;
  longitude: number;
  latitude: number;
  createdAt: string;
}

export interface NearbyTicketResponse {
  id: number;
  title: string;
  status: string;
  longitude: number;
  latitude: number;
  createdAt: string;
}

export interface UserDTO {
  id: number;
  email: string;
  displayName: string;
  createdAt: string;
}

export type TicketDTO = TicketCreatedResponse;

/** Backend TicketCreatedEvent record'unun TypeScript karşılığı; /topic/tickets STOMP kanalından gelir */
export interface TicketLiveEvent {
  id: number;
  title: string;
  status: string;
  latitude: number;
  longitude: number;
  reporterEmail: string;
  category: string;
  urgencyScore: number;
}
