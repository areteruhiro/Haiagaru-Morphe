package app.morphe.extension.chmate;
import java.net.URI;
/** Navigation policy for the user-operated additional-board authentication dialog. */
final class SiteAuthenticationPolicy {
 static boolean allow(String boardHost,String address,boolean mainFrame){
  try{URI uri=URI.create(address);if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getUserInfo()!=null||uri.getPort()!=-1&&uri.getPort()!=443)return false;String host=uri.getHost();return boardHost.equalsIgnoreCase(host)||!mainFrame&&"challenges.cloudflare.com".equalsIgnoreCase(host);}catch(RuntimeException invalid){return false;}
 }
 private SiteAuthenticationPolicy(){}
}
