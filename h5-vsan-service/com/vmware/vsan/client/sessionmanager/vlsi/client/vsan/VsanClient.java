package com.vmware.vsan.client.sessionmanager.vlsi.client.vsan;

import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import com.vmware.vsan.client.sessionmanager.resource.ResourceFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class VsanClient {
   @Autowired
   private VcClient vcClient;
   @Autowired
   @Qualifier("vsanFactory")
   private ResourceFactory connectionFactory;
   @Autowired
   private VlsiSettings vlsiSettingsTemplate;

   public VsanConnection getConnection(String vcUuid) {
      VcConnection vcConnection = this.vcClient.getConnection(vcUuid);
      Throwable var3 = null;

      VsanConnection var6;
      try {
         VlsiSettings vlsiSettings = this.vlsiSettingsTemplate.setHttpSettings(vcConnection.getSettings().getHttpSettings()).setAuthenticator(new VsanSessionCookieAuth()).setSessionCookie(vcConnection.getSessionCookie());
         VsanVlsiSettings vsanSettings = new VsanVlsiSettings(vlsiSettings, RequestUtil.getVsanRequestIdKey());
         var6 = (VsanConnection)this.connectionFactory.acquire(vsanSettings);
      } catch (Throwable var15) {
         var3 = var15;
         throw var15;
      } finally {
         if (vcConnection != null) {
            if (var3 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var14) {
                  var3.addSuppressed(var14);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var6;
   }
}
