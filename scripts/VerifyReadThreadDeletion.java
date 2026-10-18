package app.morphe.extension.chmate;

/** No Android runtime or user data: exercise the destructive-action selection policy. */
public final class VerifyReadThreadDeletion {
    static int checks;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    public static void main(String[] args) {
        check(!DeleteReadThreads.eligible(-1,10),"unopened threads excluded");
        check(!DeleteReadThreads.eligible(0,10),"no local history excluded");
        check(!DeleteReadThreads.eligible(1,0),"unread starred or patrol-only thread excluded");
        check(!DeleteReadThreads.eligible(1,-1),"unknown read state excluded");
        check(DeleteReadThreads.eligible(1,1),"partially read local thread included");
        check(DeleteReadThreads.eligible(1,10),"fully read local thread included");
        boolean[] checked={true,true,true};
        check(DeleteReadThreads.selectionCount(checked)==3,"initial all checked");
        checked[1]=false;
        check(DeleteReadThreads.selectionCount(checked)==2,"individual toggle off");
        DeleteReadThreads.toggleAll(checked);
        check(DeleteReadThreads.selectionCount(checked)==3,"partial selection selects all");
        DeleteReadThreads.toggleAll(checked);
        check(DeleteReadThreads.selectionCount(checked)==0,"all selected clears all");
        DeleteReadThreads.toggleAll(checked);
        check(DeleteReadThreads.selectionCount(checked)==3,"none selected selects all");
        checked[0]=false;checked[1]=false;
        check(DeleteReadThreads.selectionCount(checked)==1,"only selected thread counts");
        boolean[] empty={};DeleteReadThreads.toggleAll(empty);
        check(DeleteReadThreads.selectionCount(empty)==0,"empty candidate list safe");
        System.out.println("PASS "+checks+" read-thread deletion policy checks");
    }
}
