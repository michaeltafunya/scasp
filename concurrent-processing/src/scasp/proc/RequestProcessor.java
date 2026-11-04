package scasp.proc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded executor with back-pressure (rejection), timeouts, statistics and orderly shutdown. */
public class RequestProcessor implements AutoCloseable {
    private final ExecutorService ex;
    private final AtomicLong ok = new AtomicLong(), failed = new AtomicLong(), rejected = new AtomicLong(), nanos = new AtomicLong();

    private RequestProcessor(ExecutorService ex) { this.ex = ex; }

    /** Fixed workers + bounded queue; submit() throws RejectedExecutionException when full. */
    public static RequestProcessor platform(int workers, int queueSize) {
        return new RequestProcessor(new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize), new ThreadPoolExecutor.AbortPolicy()));
    }

    /** One virtual thread per task. */
    public static RequestProcessor virtual() {
        return new RequestProcessor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public <T> Future<T> submit(Callable<T> task) {
        try {
            return ex.submit((Callable<T>) () -> {
                long t = System.nanoTime();
                try {
                    T r = task.call();
                    ok.incrementAndGet();
                    return r;
                } catch (Exception e) {
                    failed.incrementAndGet();
                    throw e;
                } finally {
                    nanos.addAndGet(System.nanoTime() - t);
                }
            });
        } catch (RejectedExecutionException e) {
            rejected.incrementAndGet();
            throw e;
        }
    }

    /** Submit and wait with a timeout; the task is cancelled (interrupted) on timeout. */
    public <T> T call(Callable<T> task, long timeoutMs) throws Exception {
        Future<T> f = submit(task);
        try {
            return f.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception x ? x : e;
        }
    }

    public long succeeded() { return ok.get(); }
    public long failures() { return failed.get(); }
    public long rejections() { return rejected.get(); }
    public long totalMillis() { return nanos.get() / 1_000_000; }

    @Override public void close() {
        ex.shutdown();
        try {
            if (!ex.awaitTermination(5, TimeUnit.SECONDS)) ex.shutdownNow();
        } catch (InterruptedException e) {
            ex.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
