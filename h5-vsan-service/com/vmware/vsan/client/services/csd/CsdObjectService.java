package com.vmware.vsan.client.services.csd;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectHealth;
import com.vmware.vim.vsan.binding.vim.host.VsanObjectOverallHealth;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CsdObjectService {
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public Map getCsdObjectHealth(ManagedObjectReference clusterRef, List objectUuids) {
      Validate.notNull(clusterRef);
      Validate.notEmpty(objectUuids);
      if (!VsanCapabilityUtils.isCsdSupported(clusterRef)) {
         throw new VsanUiLocalizableException("vsan.common.error.notSupported");
      } else {
         VsanObjectOverallHealth objectHealthSummary = this.getObjectHealthSummary(clusterRef, objectUuids);
         if (objectHealthSummary != null && !ArrayUtils.isEmpty(objectHealthSummary.objectHealthDetail)) {
            Map result = new HashMap();
            VsanObjectHealth[] objectHealthDetail = objectHealthSummary.objectHealthDetail;
            VsanObjectHealth[] var6 = objectHealthDetail;
            int var7 = objectHealthDetail.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               VsanObjectHealth objHealth = var6[var8];
               VsanObjectHealthState healthState = VsanObjectHealthState.fromString(objHealth.health);
               if (!ArrayUtils.isEmpty(objHealth.objUuids)) {
                  Stream.of(objHealth.objUuids).forEach((uuid) -> {
                     VsanObjectHealthState var10000 = (VsanObjectHealthState)result.put(uuid, healthState);
                  });
               }
            }

            return result;
         } else {
            String message = "No object health data returned from the server for: " + Utils.toString(objectUuids);
            Exception ex = new Exception(message);
            throw new VsanUiLocalizableException(ex);
         }
      }
   }

   private VsanObjectOverallHealth getObjectHealthSummary(ManagedObjectReference clusterRef, List objectUuids) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         Object var8;
         try {
            VsanVcClusterHealthSystem vsanVcClusterHealthSystem = conn.getVsanVcClusterHealthSystem();
            Measure m = new Measure("VsanVcClusterHealthSystem.queryObjectHealthSummary");
            Throwable var7 = null;

            try {
               var8 = vsanVcClusterHealthSystem.queryObjectHealthSummary(clusterRef, (String[])objectUuids.toArray(new String[0]), true, (VsanClusterHealthQuerySpec)null, (Boolean)null);
            } catch (Throwable var33) {
               var8 = var33;
               var7 = var33;
               throw var33;
            } finally {
               if (m != null) {
                  if (var7 != null) {
                     try {
                        m.close();
                     } catch (Throwable var32) {
                        var7.addSuppressed(var32);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var35) {
            var4 = var35;
            throw var35;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var4.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return (VsanObjectOverallHealth)var8;
      } catch (Exception var37) {
         throw new VsanUiLocalizableException("vsan.common.generic.error", var37);
      }
   }
}
