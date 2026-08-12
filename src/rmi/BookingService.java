package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BookingService extends Remote {
    
    // 1. User login (returns user session DTO if successful, throws exception if credentials fail)
    UserSession login(String email, String password) throws RemoteException;
    
    // 2. Search trains between stations on a specific date (returns list of train search results)
    List<TrainSearchResult> searchTrain(String sourceStation, String destinationStation, String dateStr) throws RemoteException;
    
    // 3. Check seat availability for a train on a given date (details seat counts per coach type)
    List<CoachAvailability> checkAvailability(int trainNo, String dateStr) throws RemoteException;
    
    // 4. Book a Tatkal ticket for a list of passenger inputs (concurrency-locked booking)
    BookingResult bookTatkalTicket(long userId, long scheduleId, String coachType, List<PassengerInput> passengers, String paymentMode) throws RemoteException;
    
    // 5. Cancel a booking/PNR for a user (freed seats can trigger waitlist promotions)
    CancellationResult cancelTicket(String pnr, long userId) throws RemoteException;
    
    // 6. Retrieve booking history for a user
    List<BookingHistoryItem> getBookingHistory(long userId) throws RemoteException;
}
