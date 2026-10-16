import app.morphe.extension.chmate.ImgurProcessingWait;
import java.io.IOException;

public class VerifyImgurProcessingWait {
    static class Clock implements ImgurProcessingWait.Clock {
        long now; boolean interrupt;
        public long millis() { return now; }
        public void sleep(long ms) throws InterruptedException { if (interrupt) throw new InterruptedException(); now += ms; }
    }
    static class Source implements ImgurProcessingWait.Source<Integer> {
        int requests, readyAfter = 2; long fetchTime; Clock clock;
        Source(Clock c) { clock = c; }
        public boolean ready(Integer value) { return value >= readyAfter; }
        public Integer fetch(int remaining) { if (remaining <= 0) throw new AssertionError(); requests++; clock.now += fetchTime; return requests; }
    }
    public static void main(String[] args) throws Exception {
        Clock clock = new Clock(); Source source = new Source(clock);
        if (ImgurProcessingWait.await(2, 0, source, clock) != 2 || source.requests != 0) throw new AssertionError("Ready must not poll");
        if (ImgurProcessingWait.await(0, 10, source, clock) != 2 || source.requests != 2) throw new AssertionError("Polling completion");
        clock = new Clock(); source = new Source(clock);
        try { ImgurProcessingWait.await(0, 0, source, clock); throw new AssertionError(); } catch (IOException expected) { }
        if (source.requests != 0) throw new AssertionError("Zero must not poll");
        clock = new Clock(); source = new Source(clock); source.readyAfter = 100;
        try { ImgurProcessingWait.await(0, 5, source, clock); throw new AssertionError(); } catch (IOException expected) { }
        if (clock.now != 5000 || source.requests != 2) throw new AssertionError("Bounded polling");
        clock = new Clock(); source = new Source(clock); source.readyAfter = 1; source.fetchTime = 10000;
        try { ImgurProcessingWait.await(0, 5, source, clock); throw new AssertionError("Late completion accepted"); } catch (IOException expected) { }
        clock = new Clock(); clock.interrupt = true; source = new Source(clock);
        try { ImgurProcessingWait.await(0, 5, source, clock); throw new AssertionError(); } catch (java.io.InterruptedIOException expected) { }
        if (!Thread.interrupted() || source.requests != 0) throw new AssertionError("Interrupt handling");
        if (ImgurProcessingWait.seconds(-1) != 0 || ImgurProcessingWait.seconds(9999) != 1800) throw new AssertionError("Bounds");
        System.out.println("PASS Imgur processing: ready, completion, zero wait, deadline, interruption and bounds");
    }
}
