package app.morphe.extension.chmate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.content.SharedPreferences;

/** Additional preview traffic only; explicit URL actions remain available. */
final class MobileMediaPolicy {
    static final String IMAGE="urlImageFetchPolicy",LIST="threadListFetchPolicy",WEB="webCardFetchPolicy",X="xCardFetchPolicy";
    static boolean wifi(){
        Context c=Haiagaru.applicationContextForExtension();if(c==null)return false;
        try{ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities caps=cm==null?null:cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }catch(RuntimeException ignored){return false;}
    }
    static int configured(String key){
        Context c=Haiagaru.applicationContextForExtension();if(c==null)return 2;
        SharedPreferences p=c.getSharedPreferences(WacchoiSearchOptions.PREFS,0);
        int value=p.getInt(key,2);
        if(IMAGE.equals(key)&&!p.contains(key)&&p.contains("urlImageFetchEnabled")&&!p.getBoolean("urlImageFetchEnabled",true))return 1;
        return value>=0&&value<=2?value:2;
    }
    static boolean allowed(String key){int mode=configured(key);return mode==0||mode==2&&wifi();}
    static int videoConfigured(){Context c=Haiagaru.applicationContextForExtension();if(c==null)return 4;SharedPreferences p=c.getSharedPreferences(WacchoiSearchOptions.PREFS,0);return p.getInt("urlVideoFetchMode",p.getInt("mediaAutoFetchMode",2)==3?3:4);}
    static int gifConfigured(){Context c=Haiagaru.applicationContextForExtension();return c==null?4:c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getInt(GifAutoFetchPolicy.KEY,4);}
    static long fileLimit(long existing) {
        Context c=Haiagaru.applicationContextForExtension();
        int mb=c==null?0:c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getInt("previewFileLimitMb",0);
        return mb<=0?existing:Math.min(existing,Math.min(1024,mb)*1024L*1024L);
    }
    private MobileMediaPolicy(){}
}
