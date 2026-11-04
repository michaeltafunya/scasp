package scasp.ejb;

import jakarta.ejb.*;
import java.util.concurrent.atomic.AtomicLong;

/** Singleton bean: shared metrics, plus a timer callback that logs them every minute. */
@Singleton
@Startup
public class AppMetrics {
    private final AtomicLong registrations = new AtomicLong();
    public void count() { registrations.incrementAndGet(); }

    @Schedule(minute = "*", hour = "*", persistent = false)
    void report() { System.out.println("SCASP registrations so far: " + registrations.get()); }
}
