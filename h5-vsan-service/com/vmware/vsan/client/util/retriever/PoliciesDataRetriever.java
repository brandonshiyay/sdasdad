package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.pbm.profile.Profile;
import com.vmware.vim.binding.pbm.profile.ProfileId;
import com.vmware.vim.binding.pbm.profile.ProfileManager;
import com.vmware.vim.binding.pbm.profile.ResourceType;
import com.vmware.vim.binding.pbm.profile.ResourceTypeEnum;
import com.vmware.vim.binding.pbm.profile.CapabilityBasedProfile.ProfileCategoryEnum;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;
import com.vmware.vsan.client.util.Measure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PoliciesDataRetriever extends AbstractAsyncDataRetriever {
   private final PbmClient pbmClient;
   private final PermissionService permissionService;
   private static final Log logger = LogFactory.getLog(VirtualObjectsService.class);

   public PoliciesDataRetriever(ManagedObjectReference clusterRef, Measure measure, PbmClient pbmClient, PermissionService permissionService) {
      super(clusterRef, measure);
      this.pbmClient = pbmClient;
      this.permissionService = permissionService;
   }

   public void start() {
      boolean hasReadPolicyPermission = this.hasPoliciesPermissions();
      if (!hasReadPolicyPermission) {
         logger.info("User doesn't have permissions to read policies, returning empty result.");
         this.result = new Profile[0];
      } else {
         try {
            PbmConnection pbmConn = this.pbmClient.getConnection(this.clusterRef.getServerGuid());
            Throwable var3 = null;

            try {
               this.future = this.measure.newFuture("ProfileManager.retrieveContent");
               ProfileManager profileManager = pbmConn.getProfileManager();
               ProfileId[] profileIds = profileManager.queryProfile(new ResourceType(ResourceTypeEnum.STORAGE.name()), ProfileCategoryEnum.REQUIREMENT.name());
               profileManager.retrieveContent(profileIds, this.future);
            } catch (Throwable var14) {
               var3 = var14;
               throw var14;
            } finally {
               if (pbmConn != null) {
                  if (var3 != null) {
                     try {
                        pbmConn.close();
                     } catch (Throwable var13) {
                        var3.addSuppressed(var13);
                     }
                  } else {
                     pbmConn.close();
                  }
               }

            }
         } catch (Exception var16) {
            this.future.setException(var16);
         }

      }
   }

   private boolean hasPoliciesPermissions() {
      try {
         return this.permissionService.hasVcPermissions(this.clusterRef, new String[]{"StorageProfile.View"});
      } catch (Exception var2) {
         logger.error("Unable to query user permissions for read policies");
         return true;
      }
   }
}
