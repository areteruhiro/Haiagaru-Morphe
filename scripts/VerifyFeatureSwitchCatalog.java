package app.morphe.extension.chmate;
import java.util.*;
public class VerifyFeatureSwitchCatalog {
 public static void main(String[]args)throws Exception{
  java.lang.reflect.Field field=FeatureSwitches.class.getDeclaredField("OPTIONS");field.setAccessible(true);String[][] options=(String[][])field.get(null);
  Set<String> keys=new HashSet<>();for(String[] row:options){if(row.length!=4||!keys.add(row[1])||!Arrays.asList("true","false").contains(row[3])||row[2].isEmpty())throw new AssertionError(Arrays.toString(row));}
  for(String key:new String[]{"responseArchiveEnabled","wacchoiSearchEnabled","videoFrameThumbnailsEnabled","threadCommandsEnabled"})if(!keys.contains(key))throw new AssertionError(key);
  System.out.println("PASS: "+options.length+" feature switches, unique real keys, valid defaults and four new feature gates");
 }
}
