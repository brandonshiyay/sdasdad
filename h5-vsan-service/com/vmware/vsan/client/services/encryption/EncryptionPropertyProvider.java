package com.vmware.vsan.client.services.encryption;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.encryption.CryptoManagerKmip;
import com.vmware.vim.binding.vim.encryption.KmipClusterInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsphere.client.vsan.data.KmipClusterData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptionPropertyProvider {
   @Autowired
   private VcClient _vcClient;
   @Autowired
   private PermissionService permissionService;

   @TsService
   public KmipClusterData getKmipClusterData(ManagedObjectReference clusterRef) throws Exception {
      KmipClusterData result = new KmipClusterData();
      result.hasManageKeyServersPermissions = this.permissionService.hasVcPermissions(clusterRef, new String[]{"Cryptographer.ManageKeyServers"});
      if (!result.hasManageKeyServersPermissions) {
         return result;
      } else {
         VcConnection vcConnection = this._vcClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         KmipClusterData var6;
         try {
            ManagedObjectReference cryptoManagerMOR = vcConnection.getContent().cryptoManager;
            if (cryptoManagerMOR != null) {
               CryptoManagerKmip cryptoManagerKmip = (CryptoManagerKmip)vcConnection.createStub(CryptoManagerKmip.class, cryptoManagerMOR);
               KmipClusterInfo[] clusters = cryptoManagerKmip.listKmipServers((Integer)null);
               if (clusters == null) {
                  return result;
               }

               KmipClusterInfo[] var8 = clusters;
               int var9 = clusters.length;

               for(int var10 = 0; var10 < var9; ++var10) {
                  KmipClusterInfo clusterInfo = var8[var10];
                  result.availableKmipClusters.add(clusterInfo.clusterId.id);
                  if (clusterInfo.useAsDefault) {
                     result.defaultKmipCluster = clusterInfo.clusterId.id;
                  }
               }

               return result;
            }

            var6 = result;
         } catch (Throwable var20) {
            var4 = var20;
            throw var20;
         } finally {
            if (vcConnection != null) {
               if (var4 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var19) {
                     var4.addSuppressed(var19);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

         return var6;
      }
   }

   @TsService
   public boolean getEncryptionPermissions(ManagedObjectReference clusterRef) throws Exception {
      return this.permissionService.hasPermissions(clusterRef, new String[]{"Cryptographer.ManageKeys", "Cryptographer.ManageEncryptionPolicy", "Cryptographer.ManageKeyServers"});
   }

   @TsService
   public boolean getReKeyPermissions(ManagedObjectReference clusterRef) throws Exception {
      return this.permissionService.hasPermissions(clusterRef, new String[]{"Cryptographer.ManageKeys"});
   }
}
