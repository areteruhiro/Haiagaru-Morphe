from pathlib import Path
import subprocess
root=Path(__file__).resolve().parents[1]
s=(root/'extensions/chmate/src/main/java/app/morphe/extension/chmate/SharedNg.java').read_text(encoding='utf-8')
a=s.index('on.setOnCheckedChangeListener(');b=s.index('\n',a)
listener=s[a:b].rstrip()
code='''public class ConsentTest {
 static int checks;static boolean enabled;
 static class Activity{} interface Listener{void changed(Switch v,boolean b);}
 static class Switch{boolean value;Listener l;void setOnCheckedChangeListener(Listener x){l=x;}void setChecked(boolean v){if(v==value)return;value=v;if(l!=null)l.changed(this,v);}}
 static class Prefs{Prefs edit(){return this;}Prefs putBoolean(String k,boolean v){enabled=v;return this;}void apply(){}}
 static Prefs prefs(){return new Prefs();}static void reload(){}static void schedule(){}
 static class PersonalNgSubmissions{static boolean consent;static Runnable awaiting;static boolean consented(){return consent;}static void enableWithConsent(Activity a,Runnable r){awaiting=r;}static void accept(){consent=true;Runnable r=awaiting;awaiting=null;r.run();}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 public static void main(String[] args){Activity a=new Activity();Switch on=new Switch();
'''+listener+'''
 on.setChecked(true);check(!on.value&&!enabled&&!PersonalNgSubmissions.consent,"no enable before consent");check(PersonalNgSubmissions.awaiting!=null,"consent screen requested");
 PersonalNgSubmissions.awaiting=null;check(!enabled,"cancel retains OFF");
 on.setChecked(true);PersonalNgSubmissions.accept();check(enabled&&on.value&&PersonalNgSubmissions.consent,"save consent enables both");
 on.setChecked(false);check(!enabled,"receive OFF");on.setChecked(true);check(enabled&&PersonalNgSubmissions.awaiting==null,"existing consent needs no repeated dialog");
 System.out.println("PASS "+checks+" production consent listener checks");}}
'''
out=root/'build/consent-host';out.mkdir(exist_ok=True);p=out/'ConsentTest.java';p.write_text(code,encoding='utf-8');jdk=Path('C:/Users/narum/jdk-21.0.9+10/bin')
subprocess.run([str(jdk/'javac.exe'),'-J-Xmx96m','-d',str(out),str(p)],check=True)
subprocess.run([str(jdk/'java.exe'),'-Xmx64m','-cp',str(out),'ConsentTest'],check=True)
