package com.vmware.vsan.client.services.config;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ConfigInfoService {
   private static final Log logger = LogFactory.getLog(ConfigInfoService.class);
   @Autowired
   private VsanClient vsanClient;

   @Async
   public CompletableFuture getVsanConfigInfoAsync(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.getVsanConfigInfo(clusterRef);
      return CompletableFuture.completedFuture(configInfoEx);
   }

   public ConfigInfoEx getVsanConfigInfo(ManagedObjectReference clusterRef) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         Object var7;
         try {
            VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
            Measure measure = new Measure("VsanVcClusterConfigSystem.getConfigInfoEx");
            Throwable var6 = null;

            try {
               var7 = vsanConfigSystem.getConfigInfoEx(clusterRef);
            } catch (Throwable var32) {
               var7 = var32;
               var6 = var32;
               throw var32;
            } finally {
               if (measure != null) {
                  if (var6 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var31) {
                        var6.addSuppressed(var31);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var34) {
            var3 = var34;
            throw var34;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var30) {
                     var3.addSuppressed(var30);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return (ConfigInfoEx)var7;
      } catch (Exception var36) {
         logger.error("Unable to fetch ConfigInfoEx");
         throw new VsanUiLocalizableException("vsan.common.cluster.configuration.error", var36);
      }
   }
}
