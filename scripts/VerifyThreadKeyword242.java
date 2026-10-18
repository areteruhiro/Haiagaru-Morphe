package app.morphe.extension.chmate;

import java.lang.reflect.*;
import java.util.*;

/** Runs native 242 coroutine code without creating an app, database or network request. */
public final class VerifyThreadKeyword242 {
    public static void main(String[] args)throws Exception{
        try{run(args);}catch(Throwable error){error.printStackTrace(System.out);System.exit(1);}
    }
    private static void run(String[] args)throws Exception{
        android.os.Looper.prepareMainLooper();
        Class<?> target=Class.forName(args[0]);
        boolean patched=Boolean.parseBoolean(args[1]);
        Constructor<?> ctor=target.getDeclaredConstructors()[0];ctor.setAccessible(true);
        Field list=target.getDeclaredField("L$0"),sort=target.getDeclaredField("I$0"),index=target.getDeclaredField("I$1");
        list.setAccessible(true);sort.setAccessible(true);index.setAccessible(true);
        Method invoke=target.getDeclaredMethod("invokeSuspend",Object.class);invoke.setAccessible(true);
        Class<?> unitClass=Class.forName("o.zzbwq");Field unit=unitClass.getDeclaredField("INSTANCE");unit.setAccessible(true);
        int checks=0;
        for(int id:new int[]{101,200,201,202})for(int position:new int[]{0,1,2,3})for(boolean empty:new boolean[]{false,true}){
            Object coroutine=ctor.newInstance(new Object[]{null});List<String> keywords=empty?Collections.emptyList():Arrays.asList("test-keyword");
            list.set(coroutine,keywords);sort.setInt(coroutine,id);index.setInt(coroutine,position);
            Object result=invoke.invoke(coroutine,unit.get(null));
            boolean expected=!empty&&position<2&&(patched||id!=200);
            if((result==keywords)!=expected)throw new AssertionError("id="+id+" index="+position+" empty="+empty+" patched="+patched);
            checks++;
        }
        System.out.println("PASS "+checks+" native keyword/sort cases; patched="+patched);System.exit(0);
    }
}
