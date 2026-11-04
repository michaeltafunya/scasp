package scasp.proc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Deliberate race (check-then-act without a lock) versus the fixed version. */
public class RaceDemo {
    /** BUGGY: two threads can both see seats > 0 and both take the last seat. */
    static class UnsafeCourse {
        int seats; final AtomicInteger taken = new AtomicInteger();
        UnsafeCourse(int s) { seats = s; }
        void register() { if (seats > 0) { Thread.yield(); seats--; taken.incrementAndGet(); } }
    }
    /** FIXED: the check and the update are one atomic step. */
    static class SafeCourse {
        int seats; final AtomicInteger taken = new AtomicInteger();
        SafeCourse(int s) { seats = s; }
        synchronized void register() { if (seats > 0) { Thread.yield(); seats--; taken.incrementAndGet(); } }
    }

    /** Returns {unsafeTaken, safeTaken} for the given capacity and number of competing threads. */
    public static int[] run(int capacity, int threads) throws Exception {
        UnsafeCourse u = new UnsafeCourse(capacity);
        SafeCourse s = new SafeCourse(capacity);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads * 2);
        for (int i = 0; i < threads; i++) {
            Thread.ofPlatform().start(() -> { await(start); u.register(); done.countDown(); });
            Thread.ofPlatform().start(() -> { await(start); s.register(); done.countDown(); });
        }
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        return new int[]{u.taken.get(), s.taken.get()};
    }
    private static void await(CountDownLatch l) { try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
