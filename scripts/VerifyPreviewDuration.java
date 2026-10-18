package app.morphe.extension.chmate;

public final class VerifyPreviewDuration {
    public static void main(String[] args) {
        int checks=0;
        for(int value:new int[]{0,1,59,60,90,3599,3600,86400,86459}) {
            if(PreviewDuration.parse(PreviewDuration.format(value))!=value)throw new AssertionError(value);
            checks++;
        }
        for(String invalid:new String[]{"", "1:", ":30", "1:60", "1441:00", "-1:00", "1:2:3", "abc", "90"}) {
            try{PreviewDuration.parse(invalid);throw new AssertionError(invalid);}
            catch(IllegalArgumentException expected){checks++;}
        }
        System.out.println("PASS "+checks+" duration cases");
    }
}
