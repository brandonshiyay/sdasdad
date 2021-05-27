package com.vmware.vsan.client.services.spbm;

import com.vmware.vim.binding.pbm.placement.PlacementHub;
import com.vmware.vim.binding.pbm.profile.CapabilityBasedProfileCreateSpec;
import com.vmware.vim.binding.pbm.profile.ProfileManager;
import com.vmware.vim.binding.vim.host.VsanInternalSystem.NewPolicyBatch;
import com.vmware.vim.binding.vim.host.VsanInternalSystem.PolicySatisfiability;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.VsanPolicyManager;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpbmService {
   @Autowired
   private PbmClient pbmClient;
   @Autowired
   private VsanClient vsanClient;

   public String getPolicyAsStringXml(String serverGuid, CapabilityBasedProfileCreateSpec policySpec, ManagedObjectReference hub) throws Exception {
      if (policySpec != null && hub != null) {
         PbmConnection conn = this.pbmClient.getConnection(serverGuid);
         Throwable var5 = null;

         String var37;
         try {
            ProfileManager profileManager = conn.getProfileManager();
            PlacementHub placementHub = this.getPlacementHub(hub);
            Measure measure = new Measure("ProfileManager.retrieveContentAsStringBySpec");
            Throwable var10 = null;

            String policy;
            try {
               policy = profileManager.retrieveContentAsStringBySpec(policySpec, placementHub);
            } catch (Throwable var33) {
               var10 = var33;
               throw var33;
            } finally {
               if (measure != null) {
                  if (var10 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var32) {
                        var10.addSuppressed(var32);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            var37 = policy;
         } catch (Throwable var35) {
            var5 = var35;
            throw var35;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var5.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var37;
      } else {
         return "";
      }
   }

   private PlacementHub getPlacementHub(ManagedObjectReference hub) {
      PlacementHub placementHub = new PlacementHub();
      placementHub.setHubType(hub.getType());
      placementHub.setHubId(hub.getValue());
      return placementHub;
   }

   public PolicySatisfiability getPolicySatisfiability(String serverGuid, String policy, long capacityInBytes) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(serverGuid);
      Throwable var6 = null;

      PolicySatisfiability var40;
      try {
         VsanPolicyManager vsanPolicyManager = conn.getVsanPolicyManager();
         ManagedObjectReference clusterRef = null;
         NewPolicyBatch[] policyBatches = this.getPolicyBatches(policy, capacityInBytes);
         boolean ignoreSatisfiability = true;
         Measure measure = new Measure("VsanPolicyManager.evaluateObjectSize");
         Throwable var13 = null;

         PolicySatisfiability[] policySatisfiabilities;
         try {
            policySatisfiabilities = vsanPolicyManager.evaluateObjectSize((ManagedObjectReference)clusterRef, policyBatches, ignoreSatisfiability);
         } catch (Throwable var36) {
            var13 = var36;
            throw var36;
         } finally {
            if (measure != null) {
               if (var13 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var35) {
                     var13.addSuppressed(var35);
                  }
               } else {
                  measure.close();
               }
            }

         }

         var40 = policySatisfiabilities[0];
      } catch (Throwable var38) {
         var6 = var38;
         throw var38;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var34) {
                  var6.addSuppressed(var34);
               }
            } else {
               conn.close();
            }
         }

      }

      return var40;
   }

   private NewPolicyBatch[] getPolicyBatches(String policy, long capacityInBytes) {
      NewPolicyBatch policySpec = new NewPolicyBatch();
      policySpec.size = new long[]{capacityInBytes};
      policySpec.policy = policy;
      return new NewPolicyBatch[]{policySpec};
   }
}
