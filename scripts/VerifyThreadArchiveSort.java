package app.morphe.extension.chmate;
import java.io.File;
import java.util.*;
public class VerifyThreadArchiveSort {
 static int checks;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static class Entry extends File {final long date; Entry(String name,long date){super(name);this.date=date;} public long lastModified(){return date;}}
 public static void main(String[] args){
  File old=new Entry("z[1700000000]",20),recent=new Entry("a[1800000000]",10),unknown=new Entry("板フォルダ",0);
  check(ThreadArchiveSort.created(old)==1700000000L,"directory thread key");
  check(ThreadArchiveSort.created(new File("android_1700000000.dat"))==1700000000L,"flat DAT thread key");
  check(ThreadArchiveSort.created(new File("title[abc]"))==0,"unknown date");
  for(int mode=1;mode<=4;mode++){
   File[] files={unknown,old,recent};final int m=mode;Arrays.sort(files,(a,b)->ThreadArchiveSort.compare(a,b,m));
   check(files[2]==unknown,"unknown at end "+mode);
   check(files[0]==(mode==1||mode==4?recent:old),"correct time not title "+mode);
   check(ThreadArchiveSort.compare(old,old,mode)==0,"reflexive "+mode);
   check(Integer.signum(ThreadArchiveSort.compare(old,recent,mode))==-Integer.signum(ThreadArchiveSort.compare(recent,old,mode)),"antisymmetric "+mode);
  }
  check(ThreadArchiveSort.compare(new Entry("a[1700000000]",5),new Entry("b[1700000000]",5),1)<0,"stable tie break");
  System.out.println("PASS "+checks+" thread archive sort checks");
 }
}
