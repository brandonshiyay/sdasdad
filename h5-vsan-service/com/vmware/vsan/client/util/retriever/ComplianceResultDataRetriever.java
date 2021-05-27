package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.pbm.ServerObjectRef;
import com.vmware.vim.binding.pbm.ServerObjectRef.ObjectType;
import com.vmware.vim.binding.pbm.compliance.ComplianceManager;
import com.vmware.vim.binding.pbm.profile.ProfileId;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.cns.model.Volume;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;
import com.vmware.vsan.client.util.Measure;

public class ComplianceResultDataRetriever extends AbstractAsyncDataRetriever {
   private final PbmClient pbmClient;
   private final Volume volume;

   public ComplianceResultDataRetriever(ManagedObjectReference clusterRef, Measure measure, PbmClient pbmClient, Volume volume) {
      super(clusterRef, measure);
      this.pbmClient = pbmClient;
      this.volume = volume;
   }

   public void start() {
      try {
         PbmConnection pbmConn = this.pbmClient.getConnection(this.clusterRef.getServerGuid());
         Throwable var2 = null;

         try {
            this.future = this.measure.newFuture("ComplianceManager.FetchComplianceResult");
            ComplianceManager complianceManager = pbmConn.getComplianceManager();
            ServerObjectRef[] serverObjectRefs = this.createServerObjectRefs(this.volume.id, this.clusterRef.getServerGuid());
            complianceManager.fetchComplianceResult(serverObjectRefs, (ProfileId)null, this.future);
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (pbmConn != null) {
               if (var2 != null) {
                  try {
                     pbmConn.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  pbmConn.close();
               }
            }

         }
      } catch (Exception var15) {
         this.future.setException(var15);
      }

   }

   private ServerObjectRef[] createServerObjectRefs(String volumeId, String serverGuild) {
      ServerObjectRef serverObjectRef = new ServerObjectRef();
      serverObjectRef.setObjectType(ObjectType.virtualDiskUUID.toString());
      serverObjectRef.setKey(volumeId);
      serverObjectRef.setServerUuid(serverGuild);
      return new ServerObjectRef[]{serverObjectRef};
   }
}
