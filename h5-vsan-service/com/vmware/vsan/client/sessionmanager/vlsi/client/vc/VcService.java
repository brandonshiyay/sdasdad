package com.vmware.vsan.client.sessionmanager.vlsi.client.vc;

import com.vmware.vise.usersession.ServerInfo;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VcService {
   @Autowired
   public AsyncUserSessionService sessionService;

   public ServerInfo findServerInfo(String vcUuid) {
      if (vcUuid == null) {
         throw new IllegalArgumentException("vcUuid cannot be null, probably coming from MOR without serverGuid.");
      } else {
         ServerInfo[] var2 = this.sessionService.getUserSession().serversInfo;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            ServerInfo vcServer = var2[var4];
            if (vcUuid.equalsIgnoreCase(vcServer.serviceGuid)) {
               return vcServer;
            }
         }

         throw new IllegalStateException("Not found server info for: " + vcUuid);
      }
   }
}
