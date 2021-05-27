package com.vmware.vsphere.client.vsan.base.impl;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.pbm.capability.CapabilityInstance;
import com.vmware.vim.binding.pbm.placement.CompatibilityResult;
import com.vmware.vim.binding.pbm.placement.PlacementHub;
import com.vmware.vim.binding.pbm.placement.PlacementSolver;
import com.vmware.vim.binding.pbm.profile.CapabilityBasedProfile;
import com.vmware.vim.binding.pbm.profile.Profile;
import com.vmware.vim.binding.pbm.profile.ProfileId;
import com.vmware.vim.binding.pbm.profile.ProfileManager;
import com.vmware.vim.binding.pbm.profile.ResourceType;
import com.vmware.vim.binding.pbm.profile.ResourceTypeEnum;
import com.vmware.vim.binding.pbm.profile.SubProfileCapabilityConstraints;
import com.vmware.vim.binding.pbm.profile.CapabilityBasedProfile.ProfileCategoryEnum;
import com.vmware.vim.binding.pbm.profile.SubProfileCapabilityConstraints.SubProfile;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vmomi.core.impl.BlockingFuture;
import com.vmware.vim.vmomi.core.types.VmodlType;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap.Factory;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsphere.client.vsan.base.data.StoragePolicyData;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PbmDataProvider {
   @Autowired
   private PbmClient pbmClient;
   @Autowired
   private PermissionService permissionService;
   @Autowired
   private VsanInventoryHelper inventoryHelper;
   private static final Logger logger = LoggerFactory.getLogger(PbmDataProvider.class);
   private static final String VMCRYPT_POLICY_NAMESPACE = "vmwarevmcrypt";
   private static final String ENCRYPTION_LINE_OF_SERVICE = "ENCRYPTION";
   private static final String DATASERVICE_POLICY_NAMESPACE = "com.vmware.storageprofile.dataservice";
   private static final String DATASTORE_WSDL_NAME;

   @TsService
   public Map getStoragePolicyIdNameMap(ManagedObjectReference clusterRef) {
      ProfileId[] profileIds = this.getProfileIds(clusterRef);
      if (ArrayUtils.isEmpty(profileIds)) {
         return new HashMap();
      } else {
         Profile[] storageProfiles = this.getProfiles(clusterRef, profileIds);
         if (ArrayUtils.isEmpty(storageProfiles)) {
            return new HashMap();
         } else {
            Map result = new HashMap(profileIds.length);
            Profile[] var5 = storageProfiles;
            int var6 = storageProfiles.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               Profile profile = var5[var7];
               result.put(profile.profileId.uniqueId, profile.name);
            }

            return result;
         }
      }
   }

   @TsService
   public List getStoragePolicies(ManagedObjectReference clusterRef) throws Exception {
      return this.getStoragePolicies(clusterRef, false);
   }

   @TsService
   public List getObjectCompatibleStoragePolicies(ManagedObjectReference objectRef) throws Exception {
      ManagedObjectReference clusterRef = BaseUtils.getCluster(objectRef);
      return this.getStoragePolicies(clusterRef, true);
   }

   public List getStoragePolicies(ManagedObjectReference clusterRef, boolean compatibleOnly) throws Exception {
      ProfileId[] requirementProfileIds = this.getProfileIds(clusterRef);
      if (ArrayUtils.isEmpty(requirementProfileIds)) {
         return Collections.EMPTY_LIST;
      } else {
         ManagedObjectReference vsanDatastore = this.inventoryHelper.getVsanDatastore(clusterRef);
         Map compatibleProfiles = this.getCompatibleProfiles(vsanDatastore, requirementProfileIds);
         if (vsanDatastore != null && compatibleOnly) {
            requirementProfileIds = (ProfileId[])compatibleProfiles.values().toArray(new ProfileId[compatibleProfiles.size()]);
         }

         if (ArrayUtils.isEmpty(requirementProfileIds)) {
            return Collections.EMPTY_LIST;
         } else {
            Profile[] requirementProfiles = this.getProfiles(clusterRef, requirementProfileIds);
            if (ArrayUtils.isEmpty(requirementProfiles)) {
               return Collections.EMPTY_LIST;
            } else {
               Set encryptionProfiles = this.getEncryptionProfiles(clusterRef);
               String defaultProfileId = vsanDatastore == null ? null : this.getDefaultStorageProfileId(clusterRef, vsanDatastore);
               List result = new ArrayList();
               Profile[] var10 = requirementProfiles;
               int var11 = requirementProfiles.length;

               for(int var12 = 0; var12 < var11; ++var12) {
                  Profile profile = var10[var12];
                  StoragePolicyData policy = new StoragePolicyData(profile);
                  if (policy.id.equals(defaultProfileId)) {
                     policy.isDefault = true;
                  }

                  ProfileId compatibleProfile = (ProfileId)compatibleProfiles.get(profile.getProfileId().getUniqueId());
                  if (compatibleProfile != null && policy.hasVsanNamespace) {
                     policy.isCompatible = true;
                  }

                  policy.isVmCrypt = this.isVmCryptProfile(profile, encryptionProfiles);
                  result.add(policy);
               }

               Collections.sort(result, new Comparator() {
                  public int compare(StoragePolicyData lhs, StoragePolicyData rhs) {
                     return lhs.name.compareToIgnoreCase(rhs.name);
                  }
               });
               return result;
            }
         }
      }
   }

   public ProfileId[] getProfileIds(ManagedObjectReference clusterRef) {
      boolean hasReadPoliciesPermission = true;

      try {
         hasReadPoliciesPermission = this.permissionService.hasVcPermissions(clusterRef, new String[]{"StorageProfile.View"});
      } catch (Exception var36) {
         logger.error("Unable to query user permissions for read policies");
      }

      if (!hasReadPoliciesPermission) {
         logger.warn("User doesn't have permissions to read policies, returning empty result.");
         return new ProfileId[0];
      } else {
         ProfileId[] profileIds = null;

         try {
            PbmConnection pbmConn = this.pbmClient.getConnection(clusterRef.getServerGuid());
            Throwable var5 = null;

            try {
               ProfileManager profileManager = pbmConn.getProfileManager();
               Measure measure = new Measure("ProfileManager.queryProfile");
               Throwable var8 = null;

               try {
                  profileIds = profileManager.queryProfile(new ResourceType(ResourceTypeEnum.STORAGE.name()), ProfileCategoryEnum.REQUIREMENT.name());
               } catch (Throwable var35) {
                  var8 = var35;
                  throw var35;
               } finally {
                  if (measure != null) {
                     if (var8 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var34) {
                           var8.addSuppressed(var34);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }
            } catch (Throwable var38) {
               var5 = var38;
               throw var38;
            } finally {
               if (pbmConn != null) {
                  if (var5 != null) {
                     try {
                        pbmConn.close();
                     } catch (Throwable var33) {
                        var5.addSuppressed(var33);
                     }
                  } else {
                     pbmConn.close();
                  }
               }

            }
         } catch (Exception var40) {
            logger.error("Failed to query profiles on cluster " + clusterRef, var40);
         }

         return profileIds;
      }
   }

   private Profile[] getProfiles(ManagedObjectReference clusterRef, ProfileId[] profileIds) {
      Profile[] profiles = null;

      try {
         PbmConnection pbmConn = this.pbmClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            ProfileManager profileManager = pbmConn.getProfileManager();
            Measure measure = new Measure("ProfileManager.retrieveContent");
            Throwable var8 = null;

            try {
               profiles = profileManager.retrieveContent(profileIds);
            } catch (Throwable var33) {
               var8 = var33;
               throw var33;
            } finally {
               if (measure != null) {
                  if (var8 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var32) {
                        var8.addSuppressed(var32);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var35) {
            var5 = var35;
            throw var35;
         } finally {
            if (pbmConn != null) {
               if (var5 != null) {
                  try {
                     pbmConn.close();
                  } catch (Throwable var31) {
                     var5.addSuppressed(var31);
                  }
               } else {
                  pbmConn.close();
               }
            }

         }
      } catch (Exception var37) {
         logger.error("Unable to get list of storage policies!", var37);
      }

      return profiles;
   }

   public Profile getProfile(ManagedObjectReference clusterRef, ProfileId profileId) {
      ProfileId[] profileIds = new ProfileId[]{profileId};
      Profile[] profiles = this.getProfiles(clusterRef, profileIds);
      return ArrayUtils.isNotEmpty(profiles) ? profiles[0] : null;
   }

   private Map getCompatibleProfiles(ManagedObjectReference vsanDatastore, ProfileId[] profileIds) throws ExecutionException, InterruptedException {
      Map compatibleProfiles = new HashMap();
      if (vsanDatastore != null && !ArrayUtils.isEmpty(profileIds)) {
         PbmConnection pbmConn = this.pbmClient.getConnection(vsanDatastore.getServerGuid());
         Throwable var5 = null;

         try {
            PlacementSolver placementSolver = pbmConn.getPlacementSolver();
            Map compatibilityFutures = new HashMap();
            PlacementHub pbmHub = new PlacementHub(DATASTORE_WSDL_NAME, vsanDatastore.getValue());
            ProfileId[] var9 = profileIds;
            int var10 = profileIds.length;

            ProfileId profileId;
            for(int var11 = 0; var11 < var10; ++var11) {
               profileId = var9[var11];
               Future compatibilityResult = new BlockingFuture();
               placementSolver.checkCompatibility(new PlacementHub[]{pbmHub}, profileId, compatibilityResult);
               compatibilityFutures.put(compatibilityResult, profileId);
            }

            Iterator var23 = compatibilityFutures.keySet().iterator();

            while(var23.hasNext()) {
               Future requirementFuture = (Future)var23.next();
               CompatibilityResult[] results = (CompatibilityResult[])requirementFuture.get();
               if (results != null && results.length > 0 && results[0].error == null) {
                  profileId = (ProfileId)compatibilityFutures.get(requirementFuture);
                  compatibleProfiles.put(profileId.getUniqueId(), profileId);
               }
            }

            HashMap var24 = compatibleProfiles;
            return var24;
         } catch (Throwable var21) {
            var5 = var21;
            throw var21;
         } finally {
            if (pbmConn != null) {
               if (var5 != null) {
                  try {
                     pbmConn.close();
                  } catch (Throwable var20) {
                     var5.addSuppressed(var20);
                  }
               } else {
                  pbmConn.close();
               }
            }

         }
      } else {
         return compatibleProfiles;
      }
   }

   private Set getEncryptionProfiles(ManagedObjectReference clusterRef) {
      Set encryptionProfiles = new HashSet();
      PbmConnection pbmConn = this.pbmClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         ProfileManager profileManager = pbmConn.getProfileManager();
         ResourceType resource = new ResourceType(ResourceTypeEnum.STORAGE.name());
         ProfileId[] dataServiceProfileIds = profileManager.queryProfile(resource, ProfileCategoryEnum.DATA_SERVICE_POLICY.name());
         Profile[] dsProfiles = profileManager.retrieveContent(dataServiceProfileIds);
         Profile[] var9 = dsProfiles;
         int var10 = dsProfiles.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            Profile profile = var9[var11];
            if (profile instanceof CapabilityBasedProfile) {
               CapabilityBasedProfile capabilityBasedProfile = (CapabilityBasedProfile)profile;
               if ("ENCRYPTION".equals(capabilityBasedProfile.lineOfService)) {
                  encryptionProfiles.add(profile.profileId.getUniqueId());
               }
            }
         }

         HashSet var23 = encryptionProfiles;
         return var23;
      } catch (Throwable var21) {
         var4 = var21;
         throw var21;
      } finally {
         if (pbmConn != null) {
            if (var4 != null) {
               try {
                  pbmConn.close();
               } catch (Throwable var20) {
                  var4.addSuppressed(var20);
               }
            } else {
               pbmConn.close();
            }
         }

      }
   }

   private String getDefaultStorageProfileId(ManagedObjectReference clusterRef, ManagedObjectReference vsanDatastore) {
      String defaultProfileId = null;

      try {
         PbmConnection pbmConn = this.pbmClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            PlacementHub pbmHub = new PlacementHub(DATASTORE_WSDL_NAME, vsanDatastore.getValue());
            ProfileManager profileManager = pbmConn.getProfileManager();
            ProfileId profileId = profileManager.queryDefaultRequirementProfile(pbmHub);
            if (profileId != null) {
               defaultProfileId = profileId.getUniqueId();
            } else {
               logger.warn("There is no default storage policy for datastore " + vsanDatastore + ".");
            }
         } catch (Throwable var17) {
            var5 = var17;
            throw var17;
         } finally {
            if (pbmConn != null) {
               if (var5 != null) {
                  try {
                     pbmConn.close();
                  } catch (Throwable var16) {
                     var5.addSuppressed(var16);
                  }
               } else {
                  pbmConn.close();
               }
            }

         }
      } catch (Exception var19) {
         logger.error("Unable to find the default storage policy.", var19);
      }

      return defaultProfileId;
   }

   private boolean isVmCryptProfile(Profile profile, Set encryptionProfiles) {
      if (!(profile instanceof CapabilityBasedProfile)) {
         return false;
      } else {
         CapabilityBasedProfile capabilityBasedProfile = (CapabilityBasedProfile)profile;
         if (!(capabilityBasedProfile.constraints instanceof SubProfileCapabilityConstraints)) {
            return false;
         } else {
            SubProfileCapabilityConstraints constraints = (SubProfileCapabilityConstraints)capabilityBasedProfile.constraints;
            if (ArrayUtils.isEmpty(constraints.subProfiles)) {
               return false;
            } else {
               SubProfile[] var5 = constraints.subProfiles;
               int var6 = var5.length;

               for(int var7 = 0; var7 < var6; ++var7) {
                  SubProfile subProfile = var5[var7];
                  CapabilityInstance[] var9 = subProfile.capability;
                  int var10 = var9.length;

                  for(int var11 = 0; var11 < var10; ++var11) {
                     CapabilityInstance capabilityInstance = var9[var11];
                     String capabilityNamespace = capabilityInstance.id.namespace;
                     if ("vmwarevmcrypt".equals(capabilityNamespace)) {
                        return true;
                     }

                     if ("com.vmware.storageprofile.dataservice".equals(capabilityNamespace) && encryptionProfiles.contains(capabilityInstance.id.getId())) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      }
   }

   static {
      VmodlTypeMap vmodlTypes = Factory.getTypeMap();
      VmodlType dsVmodlType = vmodlTypes.getVmodlType(Datastore.class);
      DATASTORE_WSDL_NAME = dsVmodlType.getWsdlName();
   }
}
