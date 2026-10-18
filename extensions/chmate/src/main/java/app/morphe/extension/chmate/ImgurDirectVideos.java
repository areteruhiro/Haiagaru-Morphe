package app.morphe.extension.chmate;

import java.util.*;
import java.util.regex.*;

/** Re-read original body, not the mutable attachment cache, after the frame arrives. */
final class ImgurDirectVideos {
    private static final Pattern LINK=Pattern.compile("(?<![A-Za-z0-9_./:@-])(?:(?:https?|ttps?)://|//)?i\\.imgur\\.com/([A-Za-z0-9]+)\\.mp4(?:\\?[^\\s<>\"'()（）]*)?(?=[\\s<>\"'()（）]|$)",Pattern.CASE_INSENSITIVE);
    static String[] extract(String body){
        LinkedHashSet<String> urls=new LinkedHashSet<>();if(body!=null){Matcher m=LINK.matcher(body);while(m.find()&&urls.size()<8)urls.add("https://i.imgur.com/"+m.group(1)+".mp4");}return urls.toArray(new String[0]);
    }
    private ImgurDirectVideos(){}
}
