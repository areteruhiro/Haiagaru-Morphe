package app.morphe.extension.chmate;

/** Single foreground-only timer; no reference to an Activity. Calls are on the main thread. */
final class ForegroundBackupChecks {
    interface Queue {
        void post(Runnable task, long delay);
        void remove(Runnable task);
    }
    static final long PERIOD = 60_000L;
    private final Queue queue;
    private final Runnable check;
    private int started;
    private final Runnable tick = this::tick;
    ForegroundBackupChecks(Queue queue, Runnable check) { this.queue=queue;this.check=check; }
    void started() {
        if(started++!=0)return;
        queue.remove(tick);
        check.run();
        queue.post(tick,PERIOD);
    }
    void stopped() {
        started=Math.max(0,started-1);
        if(started==0)queue.remove(tick);
    }
    private void tick() {
        if(started==0)return;
        check.run();
        queue.post(tick,PERIOD);
    }
}
