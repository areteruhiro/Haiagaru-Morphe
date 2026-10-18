package app.morphe.extension.chmate;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
public final class VerifyDeletedDatRows {
    static int tests;
    static byte[] run(byte[] data,int expected) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        int count=DeletedDatRows.copy(new ByteArrayInputStream(data),out);
        if(count!=expected||out.size()!=data.length)throw new AssertionError("count/length");
        tests++;return out.toByteArray();
    }
    public static void main(String[] args)throws Exception {
        for(byte[] data:new byte[][]{"a<>b<>c<>d<>title\nx<>y<>z<>body<>\n".getBytes(),new byte[100],new byte[]{10,0,0,10},new byte[]{10,0,65}})
            if(!Arrays.equals(data,run(data,0)))throw new AssertionError("unchanged");
        byte[] row=new byte[300];row[0]=10;row[289]='x';row[299]=10;
        byte[] fixed=run(row,1);
        if(fixed[288]!=10||fixed[289]!='x')throw new AssertionError("boundary");
        byte[] actual=Files.readAllBytes(Path.of(args[0]));
        byte[] result=run(actual,1);
        long before=0,after=0;for(byte b:actual)if(b==10)before++;for(byte b:result)if(b==10)after++;
        if(before!=360||after!=361)throw new AssertionError("real fixture count");
        System.out.println("PASS "+tests+" DAT cases; device fixture 360 -> 361 slots, same byte length");
    }
}
