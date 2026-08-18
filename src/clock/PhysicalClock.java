package clock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PhysicalClock {
    private final String nodeName;
    private long clockOffsetMillis;

    public PhysicalClock(String nodeName, long initialOffsetMillis) {
        this.nodeName = nodeName;
        this.clockOffsetMillis = initialOffsetMillis;
    }

    public synchronized long getTimeMillis() {
        return System.currentTimeMillis() + clockOffsetMillis;
    }

    public synchronized long getClockOffsetMillis() {
        return clockOffsetMillis;
    }

    public synchronized void adjustClock(long adjustmentMillis) {
        this.clockOffsetMillis += adjustmentMillis;
    }

    public synchronized String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date(getTimeMillis()));
    }

    public String getNodeName() {
        return nodeName;
    }
}
