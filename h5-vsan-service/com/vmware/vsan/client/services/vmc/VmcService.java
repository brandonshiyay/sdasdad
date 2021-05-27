package com.vmware.vsan.client.services.vmc;

import com.google.common.collect.ImmutableSet;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.security.ClientSessionEndListener;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import com.vmware.vsan.client.sessionmanager.common.SessionLocal;
import com.vmware.vsan.client.sessionmanager.vlsi.client.explorer.VcLsExplorer;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.ls.LookupSvcConnection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VmcService extends SessionLocal implements ClientSessionEndListener {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   @Autowired
   private LookupSvcClient lsClient;
   @Autowired
   protected AsyncUserSessionService userSessionService;

   public boolean isVmc(String serverGuid) {
      return this.getVmcServerGuids().contains(serverGuid);
   }

   public boolean isVmc(ManagedObjectReference moRef) {
      return this.isVmc(moRef.getServerGuid());
   }

   protected Set create(String objectId) {
      try {
         LookupSvcConnection conn = this.lsClient.getConnection();
         Throwable var3 = null;

         ImmutableSet var6;
         try {
            VcLsExplorer vcLsExplorer = new VcLsExplorer(conn.getServiceRegistration());
            Set cloudVcGuids = (Set)vcLsExplorer.getVcRegistrations().stream().filter((vcReg) -> {
               return vcReg.isVmc();
            }).map((vcReg) -> {
               return vcReg.getServiceId();
            }).collect(Collectors.toSet());
            var6 = ImmutableSet.copyOf(cloudVcGuids);
         } catch (Throwable var16) {
            var3 = var16;
            throw var16;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var15) {
                     var3.addSuppressed(var15);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var6;
      } catch (Exception var18) {
         this.logger.warn("VMC check failed: ", var18);
         return Collections.EMPTY_SET;
      }
   }

   protected String sessionKey() {
      return this.userSessionService.getUserSession().clientId;
   }

   public void sessionEnded(String sessionId) {
      this.remove(sessionId);
   }

   protected void destroy(Set entity) {
   }

   private Set getVmcServerGuids() {
      return (Set)this.get();
   }
}
