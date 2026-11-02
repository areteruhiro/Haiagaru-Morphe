package app.morphe.extension.chmate;
public final class RandomUserAgentRotationTest {
    public static void main(String[] args){
        String previous=null;int checks=0;java.util.Set<String> seen=new java.util.HashSet<>();
        for(int slot=0;slot<500;slot++){
            String value=RandomUserAgentRotation.at(slot*5000L);seen.add(value);
            if(!ViewingUserAgentRules.valid(value)||value.equals(previous))throw new AssertionError("Invalid or repeated UA at "+slot);
            for(long offset:new long[]{0,1,100,4999})if(!value.equals(RandomUserAgentRotation.at(slot*5000L+offset)))throw new AssertionError("UA changed within slot");
            previous=value;checks+=5;
        }
        if(seen.size()!=5)throw new AssertionError("Pool was not covered");
        System.out.println("PASS "+checks+" UA checks; 5 variants; stable within slot; next slot differs");
    }
}
