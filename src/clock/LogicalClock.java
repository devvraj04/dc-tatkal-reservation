package clock;

public class LogicalClock {
    private long value;

    public LogicalClock() {
        this.value = 0;
    }

    public LogicalClock(long initialValue) {
        this.value = initialValue;
    }

    /**
     * Rule 1 (Local Event) & Rule 2 (Send Event): L = L + 1
     */
    public synchronized long increment() {
        this.value++;
        return this.value;
    }

    public synchronized long getValue() {
        return this.value;
    }

    /**
     * Rule 3 (Receive Event): L = max(localL, receivedL) + 1
     */
    public synchronized long updateFromRemote(long remoteTimestamp) {
        long oldLocal = this.value;
        this.value = Math.max(this.value, remoteTimestamp) + 1;
        return this.value;
    }
}
