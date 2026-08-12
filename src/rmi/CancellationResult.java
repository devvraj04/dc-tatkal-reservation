package rmi;

import java.io.Serializable;

public class CancellationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String pnr;
    private double refundAmount;
    private double cancellationCharge;
    private String message;

    public CancellationResult(boolean success, String pnr, double refundAmount, double cancellationCharge, String message) {
        this.success = success;
        this.pnr = pnr;
        this.refundAmount = refundAmount;
        this.cancellationCharge = cancellationCharge;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getPnr() { return pnr; }
    public double getRefundAmount() { return refundAmount; }
    public double getCancellationCharge() { return cancellationCharge; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        if (!success) {
            return "Cancellation Failed: " + message;
        }
        return String.format("Cancellation Successful! PNR: %s | Cancellation Charge: Rs. %.2f | Refund Amount: Rs. %.2f", 
            pnr, cancellationCharge, refundAmount);
    }
}
