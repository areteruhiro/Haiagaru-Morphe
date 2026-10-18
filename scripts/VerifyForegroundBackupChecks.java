package app.morphe.extension.chmate;

public final class VerifyForegroundBackupChecks {
    static final class Queue implements ForegroundBackupChecks.Queue {
        Runnable pending;
        public void post(Runnable task,long delay){if(pending!=null||delay!=60000)throw new AssertionError("duplicate timer / wrong interval");pending=task;}
        public void remove(Runnable task){if(pending==task)pending=null;}
        void advance(){Runnable task=pending;pending=null;task.run();}
    }
    public static void main(String[] args){
        Queue q=new Queue();int[] count={0};ForegroundBackupChecks checks=new ForegroundBackupChecks(q,()->count[0]++);
        checks.started();if(count[0]!=1||q.pending==null)throw new AssertionError("entry check");
        q.advance();q.advance();if(count[0]!=3)throw new AssertionError("foreground repeats");
        checks.started();if(count[0]!=3)throw new AssertionError("second activity duplicates");
        checks.stopped();if(q.pending==null)throw new AssertionError("other activity visible");
        Runnable stale=q.pending;checks.stopped();if(q.pending!=null)throw new AssertionError("background cancellation");
        stale.run();if(count[0]!=3||q.pending!=null)throw new AssertionError("stale callback starts backup");
        checks.started();if(count[0]!=4||q.pending==null)throw new AssertionError("foreground resumes");
        checks.stopped();checks.stopped();checks.started();if(count[0]!=5)throw new AssertionError("unbalanced stops");
        System.out.println("PASS 8 foreground backup scheduling cases");
    }
}
