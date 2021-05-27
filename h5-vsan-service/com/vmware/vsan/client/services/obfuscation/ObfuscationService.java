package com.vmware.vsan.client.services.obfuscation;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.common.CeipService;
import com.vmware.vsan.client.services.obfuscation.model.ObfuscationData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ObfuscationService {
   @Autowired
   private CeipService ceipService;
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public ObfuscationData getObfuscationData(ManagedObjectReference clusterRef) throws Exception {
      ObfuscationData data = new ObfuscationData();
      Measure measure = new Measure("phoneHomeSystem.vsanGetPhoneHomeObfuscationMap");
      Throwable var4 = null;

      try {
         try {
            VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
            Throwable var6 = null;

            try {
               String obfuscationData = conn.getPhoneHomeSystem().vsanGetPhoneHomeObfuscationMap(clusterRef);
               data.obfuscationMap = obfuscationData;
               data.obfuscationSupported = true;
            } catch (Throwable var31) {
               var6 = var31;
               throw var31;
            } finally {
               if (conn != null) {
                  if (var6 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var30) {
                        var6.addSuppressed(var30);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Exception var33) {
            data.obfuscationSupported = false;
         }
      } catch (Throwable var34) {
         var4 = var34;
         throw var34;
      } finally {
         if (measure != null) {
            if (var4 != null) {
               try {
                  measure.close();
               } catch (Throwable var29) {
                  var4.addSuppressed(var29);
               }
            } else {
               measure.close();
            }
         }

      }

      data.ceipEnabled = this.ceipService.getCeipServiceEnabled(clusterRef);
      data.clusterVsanConfigUuid = (String)QueryUtil.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.defaultConfig.uuid", (Object)null);
      return data;
   }
}
