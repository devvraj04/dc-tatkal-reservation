// Shared TypeScript types mirroring the Java DTOs

export interface UserSession {
  userId: number;
  fullName: string;
  email: string;
}

export interface TrainSearchResult {
  scheduleId: number;
  trainNo: number;
  trainName: string;
  departureTime: string;
  arrivalTime: string;
  dayDiff: number;
}

export interface CoachAvailability {
  coachType: string;
  availableSeats: number;
  totalSeats: number;
}

export interface SeatAssignment {
  passengerName: string;
  coachNumber: string | null;
  seatNumber: number;
  berthType: string | null;
  status: string;
}

export interface BookingResult {
  success: boolean;
  pnr: string | null;
  status: string;
  totalFare: number;
  message: string;
  seatAssignments: SeatAssignment[];
}

export interface PassengerDetail {
  passengerName: string;
  coachNumber: string | null;
  seatNumber: number;
  berthType: string | null;
  status: string;
}

export interface BookingHistoryItem {
  pnr: string;
  trainNo: number;
  trainName: string;
  journeyDate: string;
  sourceStation: string;
  destinationStation: string;
  bookingStatus: string;
  totalFare: number;
  bookingTimestamp: string;
  passengers: PassengerDetail[];
}

export interface CancellationResult {
  success: boolean;
  pnr: string;
  refundAmount: number;
  cancellationCharge: number;
  message: string;
}

export interface PassengerInput {
  passengerId?: number;   // -1 or absent = new passenger
  name?: string;
  age?: number;
  gender?: string;
  berthPreference?: string;
  idProofType?: string;
  idProofNumber?: string;
}

export interface ClockSyncResult {
  t0: number;
  serverTime: number;
  t1: number;
  rtt: number;
  estimatedNetworkDelay: number;
  adjustment: number;
  differenceMs: number;
  withinWindow: boolean;
  logicalTimestamp: number;
  serverNode: string;
  formattedServerTime: string;
}
