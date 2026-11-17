package app.morphe.extension.chmate;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Avoid eagerly parsing unrelated XML stores before ChMate's first screen. */
final class RestoredPreferenceScan {
    static boolean needsParsing(File file,String originalPackage){
        byte[] token=originalPackage.getBytes(StandardCharsets.UTF_8);
        if(token.length==0)return true;
        int[] fallback=new int[token.length];
        for(int i=1,j=0;i<token.length;i++){while(j>0&&token[i]!=token[j])j=fallback[j-1];if(token[i]==token[j])j++;fallback[i]=j;}
        try(InputStream input=new BufferedInputStream(new FileInputStream(file))){
            int value,matched=0;
            while((value=input.read())!=-1){
                // Entities and non-UTF-8 XML stay on Android's existing parser path.
                if(value=='&'||value==0||value==0xff||value==0xfe)return true;
                while(matched>0&&(byte)value!=token[matched])matched=fallback[matched-1];
                if((byte)value==token[matched]&&++matched==token.length)return true;
            }
            return false;
        }catch(IOException unreadable){return true;}
    }
    private RestoredPreferenceScan(){}
}
