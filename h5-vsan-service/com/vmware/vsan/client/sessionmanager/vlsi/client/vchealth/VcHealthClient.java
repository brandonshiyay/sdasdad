package com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth;

import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiExploratorySettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanSessionCookieAuth;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class VcHealthClient {
   @Autowired
   private VcClient vcClient;
   @Autowired
   @Qualifier("vcHealthFactory")
   private ResourceFactory vcHealthConnectionFactory;
   @Autowired
   private VlsiSettings vlsiSettingsTemplate;

   public VcHealthConnection getConnection(String vcUuid) {
      VcConnection vcConnection = this.vcClient.getConnection(vcUuid);
      Throwable var3 = null;

      VcHealthConnection var5;
      try {
         VlsiExploratorySettings exploratorySettings = new VlsiExploratorySettings(this.vlsiSettingsTemplate.setHttpSettings(vcConnection.getSettings().getHttpSettings()).setAuthenticator(new VsanSessionCookieAuth()).setSessionCookie(vcConnection.getSessionCookie()), UUID.fromString(vcUuid));
         var5 = (VcHealthConnection)this.vcHealthConnectionFactory.acquire(exploratorySettings);
      } catch (Throwable var14) {
         var3 = var14;
         throw var14;
      } finally {
         if (vcConnection != null) {
            if (var3 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var13) {
                  var3.addSuppressed(var13);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var5;
   }
}
