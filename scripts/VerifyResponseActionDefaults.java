package app.morphe.extension.chmate;
public class VerifyResponseActionDefaults {
 public static void main(String[]args){
  for(int id:new int[]{0x7e484b01,0x7e484a01,0x7e484a03,75,0x7e000077,0x7e000078})if(ResponseMenuSettings.defaultVisible(id))throw new AssertionError("must default OFF: "+id);
  if(!ResponseMenuSettings.defaultVisible(76)||!ResponseMenuSettings.defaultVisible(1))throw new AssertionError("unrelated defaults changed");
  System.out.println("PASS: six response actions default OFF; suffix NG and stock actions unchanged");
 }
}
