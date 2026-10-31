package app.morphe.extension.chmate;

import android.content.*;
import android.util.SparseIntArray;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Independent, opt-in Edge visibility overlay and consented fact synchronization. */
final class EdgeLevelNg {
    static final String OBSERVED="edgeLevelObservedNg",ESTIMATE="edgeLevelEstimateNg",RECORD="edgeLevelRecordAuth",CONSENT="edgeLevelShareConsent";
    private static final Map<Object,String> ORIGINS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean queued=new AtomicBoolean(),loaded=new AtomicBoolean();
    private static volatile Map<String,EdgeLevelPolicy.Fact> facts=Collections.emptyMap();
    private static volatile Map<String,String> authDays=Collections.emptyMap();
    private static volatile long nextSync;
    private static volatile String factsSource="";
    static volatile String status="未同期";
    static Context context(){return Haiagaru.applicationContextForExtension();}
    static SharedPreferences config(){return context().getSharedPreferences(WacchoiSearchOptions.PREFS,0);}
    static SharedPreferences local(){return context().getSharedPreferences("haiagaru.edge-levels",0);}
    static boolean option(String key){return context()!=null&&(key.equals(CONSENT)?local().getBoolean("consentV1",false)&&local().getString("consentEndpoint","").equals(endpoint()):config().getBoolean(key,false));}
    static String endpoint(){String s=context().getSharedPreferences(SharedNg.PREFS,0).getString("endpoint","");return s==null||s.trim().isEmpty()?SharedNg.DEFAULT_ENDPOINT:s;}
    static void bind(Object source,Object board){
        if(!option(OBSERVED)&&!option(ESTIMATE)&&!option(RECORD)&&!option(CONSENT))return;
        try{List<?> rows=rows(source);if(rows.isEmpty())return;ORIGINS.remove(rows.get(0));String url=ResponseArchiveEntry.snapshot(rows.get(0),board,"").url;String t=EdgeLevelPolicy.thread(url);if(t!=null)ORIGINS.put(rows.get(0),t);}catch(Exception ignored){}
    }
    static void bindLegacy(Object owner,boolean middle){try{bind(field(owner,middle?"J":"K"),field(field(owner,middle?"E":"M"),middle?"j":"f"));captureOwn(owner);}catch(Exception ignored){}}
    static List<?> rows(Object source)throws Exception{if(!(source instanceof List))return Collections.emptyList();try{Object b=field(source,"mBase");if(b instanceof List)return (List<?>)b;}catch(NoSuchFieldException ignored){}return (List<?>)source;}
    static Object field(Object o,String n)throws Exception{return ResponseArchiveEntry.field(o,n);}
    static void apply(Object owner){
        if(owner==null||(!option(OBSERVED)&&!option(ESTIMATE)))return;
        try{
            String s,m,enabled,no,id,name,date;boolean middle=false;
            switch(owner.getClass().getName()){
                case "o.m9ExternalSyntheticLambda1":s="K";m="N";enabled="p";no="o";id="h";name="n";date="q";break;
                case "o.listener":s="J";m="K";enabled="r";no="l";id="j";name="m";date="t";middle=true;break;
                case "o.zzaW":s="a";m="j";enabled="i";no="o";id="g";name="n";date="p";break;
                default:return;
            }
            if(!Boolean.TRUE.equals(field(owner,enabled)))return;
            List<?> all=rows(field(owner,s));if(all.isEmpty())return;
            if(!owner.getClass().getName().equals("o.zzaW")&&!ORIGINS.containsKey(all.get(0)))bindLegacy(owner,middle);
            String thread=ORIGINS.get(all.get(0));if(thread==null)return;
            Map<String,EdgeLevelPolicy.Fact> current=new HashMap<>();
            for(Object r:all){EdgeLevelPolicy.Fact f=EdgeLevelPolicy.parse(thread,((Number)field(r,no)).intValue(),(String)field(r,name),(String)field(r,date)+" ID:"+field(r,id));if(f!=null)EdgeLevelPolicy.put(current,f);}
            Map<String,EdgeLevelPolicy.Fact> known=factsSource.equals(endpoint())?facts:Collections.emptyMap();Object map=field(owner,m);
            Set<String> exceptions=config().getStringSet("edgeLevelExceptions",Collections.emptySet());
            for(Object r:all){EdgeLevelPolicy.Fact who=EdgeLevelPolicy.identity(thread,((Number)field(r,no)).intValue(),String.valueOf(field(r,id)),String.valueOf(field(r,date)));if(who==null||exceptions.contains(who.id)||exceptions.contains(who.key()))continue;
                EdgeLevelPolicy.Fact observed=current.get(who.key()),cached=known.get(who.key());if(cached!=null&&(observed==null||cached.at>observed.at))observed=cached;
                boolean hide=observed!=null&&option(OBSERVED)&&observed.level<=config().getInt("edgeLevelThreshold",5);
                // An observed level always overrides a date-based estimate, including a high level.
                if(observed==null&&option(CONSENT)&&option(ESTIMATE)){String day=factsSource.equals(endpoint())?authDays.get(who.key()):null;int estimate=EdgeLevelPolicy.estimate(day,who.day,config().getInt("edgeLevelInitial",1),config().getInt("edgeLevelHours",23),9999);hide=estimate>=0&&estimate<=config().getInt("edgeLevelEstimateThreshold",5);}
                if(hide){if(map instanceof SparseIntArray){SparseIntArray a=(SparseIntArray)map;a.put(who.response,a.get(who.response,0)|0x1008);}else if(map instanceof Map){@SuppressWarnings("unchecked") Map<Integer,Integer> a=(Map<Integer,Integer>)map;a.put(who.response,a.getOrDefault(who.response,0)|0x1008);}}
            }
            schedule(false);
        }catch(Exception unavailable){status="対象レスの情報を確認できません";}
    }
    static void schedule(boolean manual){
        if(context()==null||(!manual&&!config().getBoolean("edgeLevelAutoSync",true))||(!manual&&!option(OBSERVED)&&!option(ESTIMATE)&&!option(CONSENT))||(!manual&&System.currentTimeMillis()<nextSync)||!queued.compareAndSet(false,true))return;
        IO.execute(()->{try{if(loaded.compareAndSet(false,true))load();boolean claimsPending=false;try{flushClaims();}catch(Exception retryLater){claimsPending=true;}String source=endpoint();JSONObject feed=SharedNg.request("GET",source+"?action=edgeLevels",null);if(!feed.optBoolean("ok")||feed.optInt("format")!=1)throw new IOException("配信側が未対応です");
            if(!source.equals(endpoint()))throw new IOException("同期中に接続先が変更されました");feed.put("_endpoint",source);accept(feed);save(feed);nextSync=System.currentTimeMillis()+interval();status="同期済み：確認済み"+facts.size()+"件／認証日（自己申告）"+authDays.size()+"件。スレを開き直すと反映"+(claimsPending?"（認証日の送信は再試行待ち）":"");
        }catch(Exception e){nextSync=System.currentTimeMillis()+Math.max(interval(),30*60000L);status="同期失敗："+e.getMessage()+"（保存済みの情報を使用）";}finally{queued.set(false);}});
    }
    static long interval(){return Math.max(15,Math.min(1440,config().getInt("edgeLevelIntervalMinutes",30)))*60000L;}
    private static boolean recent(String day){try{long age=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(day),LocalDate.now(EdgeLevelPolicy.JST));return age>=0&&age<=7;}catch(Exception e){return false;}}
    private static EdgeLevelPolicy.Fact decode(JSONObject o){
        String thread=EdgeLevelPolicy.thread(o.optString("thread")),b=o.optString("board"),day=EdgeLevelPolicy.day(o.optString("day")),id=EdgeLevelPolicy.id(o.optString("id"));int level=o.optInt("level",-1),no=o.optInt("response");long at=o.optLong("at");
        String actualDay=at>0?Instant.ofEpochMilli(at).atZone(EdgeLevelPolicy.JST).toLocalDate().toString():"";
        return thread!=null&&b.equals(EdgeLevelPolicy.board(thread))&&day!=null&&recent(day)&&day.equals(actualDay)&&id!=null&&level>=0&&level<=9999&&no>0&&no<=10000&&at>0&&at<=System.currentTimeMillis()+300000?new EdgeLevelPolicy.Fact(b,day,id,level,at,thread,no):null;
    }
    private static void accept(JSONObject feed)throws Exception{
        if(!feed.optBoolean("ok")||feed.optInt("format")!=1||!endpoint().equals(feed.optString("_endpoint")))throw new IOException("未対応の配信形式または接続先");
        Map<String,EdgeLevelPolicy.Fact> incoming=new HashMap<>();JSONArray list=feed.getJSONArray("observed");if(list.length()>10000)throw new IOException("受信件数の上限");
        for(int i=0;i<list.length();i++){JSONObject o=list.getJSONObject(i);EdgeLevelPolicy.Fact f=decode(o);if(f!=null&&o.optBoolean("verified"))EdgeLevelPolicy.put(incoming,f);}
        Map<String,String> dates=new HashMap<>();JSONArray estimates=feed.optJSONArray("authDays");
        if(option(CONSENT)&&estimates!=null){if(estimates.length()>10000)throw new IOException("受信件数の上限");for(int i=0;i<estimates.length();i++){JSONObject o=estimates.getJSONObject(i);String b=o.optString("board"),d=EdgeLevelPolicy.day(o.optString("day")),id=EdgeLevelPolicy.id(o.optString("id")),auth=EdgeLevelPolicy.day(o.optString("authDay"));if(o.optBoolean("selfReported")&&b.matches("edge/[A-Za-z0-9_-]{1,80}")&&d!=null&&recent(d)&&id!=null&&auth!=null&&auth.compareTo(d)<=0)dates.put(b+"|"+d+"|"+id,auth);}}
        facts=Collections.unmodifiableMap(incoming);authDays=Collections.unmodifiableMap(dates);factsSource=endpoint();
    }
    private static File cache(){return new File(context().getFilesDir(),"edge-level-facts.json");}
    private static void load(){try(InputStream in=new android.util.AtomicFile(cache()).openRead()){accept(new JSONObject(SharedNg.read(in,4*1024*1024)));}catch(Exception ignored){}}
    static void revoke(){SharedPreferences.Editor e=local().edit().putBoolean("consentV1",false);for(String key:local().getAll().keySet())if(key.startsWith("pending:"))e.remove(key);e.apply();authDays=Collections.emptyMap();}
    private static synchronized void current(String key){if(!key.equals(local().getString("current","")))local().edit().putString("current",key).putLong("currentSince",System.currentTimeMillis()).apply();}
    private static void save(JSONObject feed)throws Exception{android.util.AtomicFile file=new android.util.AtomicFile(cache());FileOutputStream out=file.startWrite();try{out.write(feed.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){file.failWrite(out);throw e;}}
    static void cookies(Object url,List<?> cookies){try{if(!option(RECORD)||url==null||cookies==null||!EdgeLevelPolicy.edge(url.toString()))return;for(Object cookie:cookies){if(cookie==null)continue;String pair=cookie.toString().split(";",2)[0];if(pair.startsWith("edge-token=")){String token=pair.substring(11);if(!token.isEmpty())current(fingerprint(token));}}}catch(RuntimeException ignored){/* Observation must never interrupt native networking. */}}
    static synchronized String fingerprint(String secret){try{String salt=local().getString("salt","");if(salt.isEmpty()){salt=UUID.randomUUID().toString();local().edit().putString("salt",salt).commit();}byte[] digest=MessageDigest.getInstance("SHA-256").digest((salt+":"+secret).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(Exception e){return "";}}
    static void authentication(String board,String url,String title,String text,String header){
        if(!option(RECORD)||!EdgeLevelPolicy.edge(board)||!EdgeLevelPolicy.edge(url)||!java.net.URI.create(url).getPath().matches("/(?:auth|auth-code|auth_code)/?")||!title.equals("認証成功 - Successful")||!text.contains("認証に成功しました")||header==null)return;
        for(String pair:header.split(";"))if(pair.trim().startsWith("edge-token=")){String key=fingerprint(pair.trim().substring(11));if(key.isEmpty())return;long now=System.currentTimeMillis();current(key);SharedPreferences p=local();SharedPreferences.Editor e=p.edit();if(!p.contains("auth:"+key))e.putLong("auth:"+key,now);e.apply();return;}
    }
    static void own(Object row,String thread,int flags){
        if((flags&1)==0||!option(CONSENT)||!option(RECORD)||thread==null)return;
        try{boolean old=row.getClass().getName().equals("o.processAdDisplayErrorPostbackForUserError"),mid=row.getClass().getName().equals("o.BouncyCastleSocketAdapterCompanion");String key=local().getString("current","");long auth=local().getLong("auth:"+key,0);if(auth<=0)return;
            EdgeLevelPolicy.Fact f=EdgeLevelPolicy.identity(thread,((Number)field(row,mid?"l":"o")).intValue(),String.valueOf(field(row,old?"h":mid?"j":"g")),String.valueOf(field(row,old?"q":mid?"t":"p")));if(f==null||f.at<auth||f.at<local().getLong("currentSince",Long.MAX_VALUE)||f.at>System.currentTimeMillis()||System.currentTimeMillis()-f.at>10*60000L)return;
            String authDay=Instant.ofEpochMilli(auth).atZone(EdgeLevelPolicy.JST).toLocalDate().toString();String reportKey=f.key()+"|"+authDay;if(local().getBoolean("sent:"+reportKey,false)||local().contains("pending:"+reportKey))return;
            IO.execute(()->{if(!option(CONSENT)||!key.equals(local().getString("current",""))||local().getBoolean("sent:"+reportKey,false)||local().contains("pending:"+reportKey))return;try{pruneClaims();JSONObject report=new JSONObject().put("board",f.board).put("day",f.day).put("id",f.id).put("authDay",authDay).put("thread",f.thread).put("response",f.response);local().edit().putString("pending:"+reportKey,report.toString()).apply();flushClaims();}catch(Exception ignored){}});
        }catch(Exception ignored){}
    }
    static void captureOwn(Object owner){
        if(!option(CONSENT)||!option(RECORD))return;
        try{boolean middle=owner.getClass().getName().equals("o.listener");Object index=ExtraQuickFilters.index(owner);java.lang.reflect.Method get=index.getClass().getMethod(middle?"a":"d",int.class);get.setAccessible(true);List<?> all=rows(field(owner,middle?"J":"K"));if(all.isEmpty())return;String thread=ORIGINS.get(all.get(0));for(Object r:all)own(r,thread,((Number)get.invoke(index,((Number)field(r,middle?"l":"o")).intValue())).intValue());schedule(false);}catch(Exception ignored){}
    }
    static void ownLegacy(Object owner,Object row,int flags){if(!option(CONSENT))return;try{boolean mid=owner.getClass().getName().equals("o.listener");List<?> all=rows(field(owner,mid?"J":"K"));if(!all.isEmpty())own(row,ORIGINS.get(all.get(0)),flags);}catch(Exception ignored){}}
    @SuppressWarnings("rawtypes") static void ownModern(Object source,Map flags){if(!option(CONSENT)||flags==null)return;try{List<?> all=rows(source);if(all.isEmpty())return;String thread=ORIGINS.get(all.get(0));for(Object row:all){Object value=flags.get(((Number)field(row,"o")).intValue());if(value instanceof Number)own(row,thread,((Number)value).intValue());}}catch(Exception ignored){}}
    private static void pruneClaims(){SharedPreferences p=local();SharedPreferences.Editor edit=p.edit();String today=LocalDate.now(EdgeLevelPolicy.JST).toString();int pending=0;for(String k:p.getAll().keySet())if(k.startsWith("sent:")||k.startsWith("pending:")){String[] parts=k.split("\\|");if(parts.length<2||!parts[1].equals(today)||(k.startsWith("pending:")&&++pending>100))edit.remove(k);}edit.apply();}
    private static void flushClaims()throws Exception{pruneClaims();if(!option(CONSENT))return;JSONArray reports=new JSONArray();List<String> keys=new ArrayList<>();for(Map.Entry<String,?> e:local().getAll().entrySet())if(e.getKey().startsWith("pending:")&&reports.length()<20){reports.put(new JSONObject(String.valueOf(e.getValue())));keys.add(e.getKey());}if(keys.isEmpty())return;if(!option(CONSENT))return;JSONObject result=SharedNg.request("POST",endpoint(),new JSONObject().put("version",3).put("action","edgeLevelAuth").put("device",SharedNg.deviceHash()).put("reports",reports));if(!result.optBoolean("ok"))throw new IOException("認証日を共有できません");SharedPreferences.Editor edit=local().edit();for(String key:keys)edit.remove(key).putBoolean("sent:"+key.substring(8),true);edit.apply();}
    private EdgeLevelNg(){}
}
