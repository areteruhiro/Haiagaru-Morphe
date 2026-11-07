package app.morphe.extension.chmate;

import android.content.Context;import java.lang.reflect.*;import java.util.*;import java.util.regex.Pattern;

/** Adds real native BBS descriptors, so native BbsPreference/dialog/import/grouping remain in use. */
public final class SiteNativeBoards {
 private static final Map<String,Object> MODELS=new HashMap<>();
 /** Modern ChMate upgrades compatible-board URL producers to HTTPS. */
 public static String rewriteInfoUrl(Object info,String value){
  if(info==null||value==null||!value.startsWith("https://127.0.0.1:"))return value;
  Context context=Haiagaru.applicationContextForExtension();if(context==null)return value;
  try{
   java.net.URI uri=java.net.URI.create(value);if(!"127.0.0.1".equals(uri.getHost())||uri.getRawUserInfo()!=null)return value;
   String[] fields=ExternalBoardEditor.infoFields().split(",");if(fields.length!=2)return value;
   Field host=info.getClass().getDeclaredField(fields[0]),path=info.getClass().getDeclaredField(fields[1]);host.setAccessible(true);path.setAccessible(true);
   String key=(String)path.get(info);if(!uri.getRawAuthority().equals(host.get(info))||key==null||!key.matches(SiteBoardGateway.KEY_PATTERN))return value;
   if(SiteBoardGateway.get(context).registrationUrl(key).equals("http://"+uri.getRawAuthority()+"/"+key+"/"))return "http://"+value.substring("https://".length());
  }catch(Exception failure){android.util.Log.w("Haiagaru","Native added-board transport unavailable",failure);}
  return value;
 }
 /** Native URL parsing drops ports and assumes timestamp-sized thread numbers. */
 public static Object parse(String address){
  if(address==null||!address.startsWith("http://127.0.0.1:"))return null;
  Context context=Haiagaru.applicationContextForExtension();if(context==null)return null;
  try{
   java.net.URI uri=java.net.URI.create(address);SiteBoardGateway gateway=SiteBoardGateway.get(context);
   if(!"127.0.0.1".equals(uri.getHost())||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null)return null;
   java.util.regex.Matcher thread=Pattern.compile("/test/read\\.cgi/("+SiteBoardGateway.KEY_PATTERN+")/([0-9]{1,12})/(?:([0-9]+)|[ln][0-9]+)?").matcher(uri.getRawPath());
   java.util.regex.Matcher board=Pattern.compile("/("+SiteBoardGateway.KEY_PATTERN+")/").matcher(uri.getRawPath());
   String key;Long id=null;Integer response=null;
   if(thread.matches()){key=thread.group(1);id=SiteThreadIds.nativeInput(thread.group(2));if(thread.group(3)!=null)response=Integer.valueOf(thread.group(3));}
   else if(board.matches())key=board.group(1);else return null;
   if(!gateway.registrationUrl(key).equals("http://"+uri.getRawAuthority()+"/"+key+"/"))return null;
   Class<?> type=Class.forName("jp.syoboi.a2chMate.client.BBSUrlInfo",false,context.getClassLoader());
   Constructor<?> ctor=type.getDeclaredConstructor(String.class,String.class,Long.class,int.class,Integer.class,String.class);ctor.setAccessible(true);
   return ctor.newInstance(uri.getRawAuthority(),key,id,0,response,null);
  }catch(Exception failure){android.util.Log.w("Haiagaru","Native added-board URL unavailable",failure);return null;}
 }
 public static Iterator<?> providers(Iterator<?> original){
  Context context=Haiagaru.applicationContextForExtension();if(context==null)return original;
  return providers(original,SiteBoardGateway.get(context));
 }
 static synchronized Iterator<?> providers(Iterator<?> original,SiteBoardGateway gateway){
  ArrayList<Object> result=new ArrayList<>();original.forEachRemaining(result::add);if(result.isEmpty())return result.iterator();
  try{
   Class<?> type=result.get(0).getClass();Constructor<?> ctor=null;
   for(Constructor<?> c:type.getDeclaredConstructors()){Class<?>[] p=c.getParameterTypes();if(p.length==14&&p[0]==android.content.SharedPreferences.class&&p[6]==Pattern.class&&p[7]==Pattern.class){ctor=c;break;}}
   if(ctor==null)throw new ReflectiveOperationException("Native BBS constructor changed");ctor.setAccessible(true);Class<?>[] params=ctor.getParameterTypes();
   for(String kind:SiteBoardMenuProtocol.KINDS){
    String address=gateway.siteMenuUrl(kind),name=SiteBoardMenuProtocol.title(kind),home=SiteBoardMenuProtocol.home(kind);Pattern pattern=gateway.sitePattern(kind);String signature=type.getName()+":"+kind+":"+address+":"+pattern.pattern();
    Object model=MODELS.get(signature);if(model==null){
     Object options=params[11].getDeclaredConstructor().newInstance();Object decoder=java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{params[12]},(proxy,method,args)->{
      if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);if(method.getName().equals("equals"))return proxy==args[0];if(method.getName().equals("toString"))return "Haiagaru board URL decoder";return args[0];
     });
     Object flags=Array.newInstance(params[13].getComponentType(),0);
     model=ctor.newInstance(gateway.prefs,SiteBoardGateway.siteProviderId(kind),name,"",home,address,pattern,null,"","",false,options,decoder,flags);
     MODELS.put(signature,model);
    }result.add(model);
   }
   if(MODELS.size()>512){Set<Object> used=Collections.newSetFromMap(new IdentityHashMap<>());used.addAll(result);MODELS.entrySet().removeIf(e->!used.contains(e.getValue()));}
  }catch(Exception failure){android.util.Log.w("Haiagaru","Native added-board descriptors unavailable",failure);}
  return result.iterator();
 }
 private SiteNativeBoards(){}
}
