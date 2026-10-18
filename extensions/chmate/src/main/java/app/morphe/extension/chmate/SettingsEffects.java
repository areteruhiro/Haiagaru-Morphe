package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;

/** Shared by both the detailed settings and the feature-switch index. */
final class SettingsEffects {
    static boolean needsRestart(String key){
        // Existing native views/catalogs and process-start hooks retain these values.
        return "hideAd".equals(key)||"removeMonaKey".equals(key)
                ||"chtoio".equals(key)||"topAlignShortThreads".equals(key)
                ||"compactQuickFilters".equals(key)||"threadCommandsEnabled".equals(key)
                ||"imageViewerRotationButton".equals(key)||"wacchoiSearchEnabled".equals(key);
    }
    static void changed(Context context,String key,boolean on){
        if("forceHttps".equals(key))HttpsTransport.setEnabled(on);
        if(!on&&("wacchoiSearchEnabled".equals(key)||"wacchoiBackgroundSearch".equals(key))){
            WacchoiBackgroundJob job=WacchoiSearchService.current;if(job!=null&&job.running)job.cancel();
        }
        if("readThreadsFirst".equals(key)||HiddenDroppedThreads.KEY.equals(key)||"groupBrowsingHistoryByBoard".equals(key)
                ||EdgeReporterOptions.MENU.equals(key)||EdgeReporterOptions.BOARD.equals(key))
            Haiagaru.refreshBoardListAfterReadSortChange();
        if(needsRestart(key)&&context instanceof Activity)SettingsRestart.confirm((Activity)context);
    }
    private SettingsEffects(){}
}
