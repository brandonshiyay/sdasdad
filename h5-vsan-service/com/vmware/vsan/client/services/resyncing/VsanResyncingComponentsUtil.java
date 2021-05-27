package com.vmware.vsan.client.services.resyncing;

import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.common.data.VmData;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectsFilter;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VsanResyncingComponentsUtil {
   private static final String IDENTITY_TYPE_FILE_SHARE = "fileShare";
   private static final String IDENTITY_TYPE_ISCSI_PREFFIX = "iscsi";
   private static final Log _logger = LogFactory.getLog(VsanResyncingComponentsUtil.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanResyncingComponentsUtil.class);
   private static final String[] DISK_MAPPINGS_VM_PROPERTIES = new String[]{"name", "primaryIconId", "config.hardware.device", "summary.config.vmPathName"};

   public static Map getVirtualObjectsFilterToObjectIdentities(VsanObjectIdentityAndHealth vsanObjectIdentityAndHealth) {
      Map result = initVsanObjectIdentitiesMap();
      if (vsanObjectIdentityAndHealth != null && !ArrayUtils.isEmpty(vsanObjectIdentityAndHealth.identities)) {
         VsanObjectIdentity[] var2 = vsanObjectIdentityAndHealth.identities;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanObjectIdentity objectIdentity = var2[var4];
            if (isVmObject(objectIdentity)) {
               ((List)result.get(VirtualObjectsFilter.VMS)).add(objectIdentity);
            } else if (isVrTargetObject(objectIdentity)) {
               ((List)result.get(VirtualObjectsFilter.VR_TARGETS)).add(objectIdentity);
            } else if (isIsciObject(objectIdentity)) {
               ((List)result.get(VirtualObjectsFilter.ISCSI_TARGETS)).add(objectIdentity);
            } else if (isFileShare(objectIdentity)) {
               ((List)result.get(VirtualObjectsFilter.FILE_SHARES)).add(objectIdentity);
            } else {
               ((List)result.get(VirtualObjectsFilter.OTHERS)).add(objectIdentity);
            }
         }

         return result;
      } else {
         return result;
      }
   }

   private static Map initVsanObjectIdentitiesMap() {
      Map result = new HashMap();
      result.put(VirtualObjectsFilter.VMS, new ArrayList());
      result.put(VirtualObjectsFilter.ISCSI_TARGETS, new ArrayList());
      result.put(VirtualObjectsFilter.FILE_SHARES, new ArrayList());
      result.put(VirtualObjectsFilter.VR_TARGETS, new ArrayList());
      result.put(VirtualObjectsFilter.OTHERS, new ArrayList());
      return result;
   }

   private static boolean isVmObject(VsanObjectIdentity vsanObjectIdentity) {
      return vsanObjectIdentity.vm != null;
   }

   private static boolean isVrTargetObject(VsanObjectIdentity vsanObjectIdentity) {
      VsanObjectType objectType = VsanObjectType.parse(vsanObjectIdentity.type);
      return VsanObjectType.hbrCfg.equals(objectType) || VsanObjectType.hbrDisk.equals(objectType);
   }

   private static boolean isIsciObject(VsanObjectIdentity vsanObjectIdentity) {
      return vsanObjectIdentity.type.startsWith("iscsi");
   }

   private static boolean isFileShare(VsanObjectIdentity identity) {
      return "fileShare".equals(identity.type);
   }

   public static Map getVmData(ManagedObjectReference clusterRef, List vsanObjectIdentities) {
      Set vmRefs = getVmsFromVsanObjects(clusterRef, vsanObjectIdentities);
      if (vmRefs != null && vmRefs.size() != 0) {
         try {
            VsanProfiler.Point point = _profiler.point("getVmProperties");
            Throwable var4 = null;

            HashMap result;
            try {
               PropertyValue[] propValues = QueryUtil.getProperties((ManagedObjectReference[])vmRefs.toArray(new ManagedObjectReference[0]), DISK_MAPPINGS_VM_PROPERTIES).getPropertyValues();
               if (!ArrayUtils.isEmpty(propValues)) {
                  result = new HashMap();
                  PropertyValue[] var7 = propValues;
                  int var8 = propValues.length;

                  for(int var9 = 0; var9 < var8; ++var9) {
                     PropertyValue propValue = var7[var9];
                     ManagedObjectReference vmRef = (ManagedObjectReference)propValue.resourceObject;
                     VmData vmData = (VmData)result.get(vmRef);
                     if (vmData == null) {
                        vmData = new VmData(vmRef);
                        result.put(vmRef, vmData);
                     }

                     String var13 = propValue.propertyName;
                     byte var14 = -1;
                     switch(var13.hashCode()) {
                     case -1099694814:
                        if (var13.equals("namespaceCapabilityMetadata")) {
                           var14 = 4;
                        }
                        break;
                     case -826278890:
                        if (var13.equals("primaryIconId")) {
                           var14 = 1;
                        }
                        break;
                     case -637434256:
                        if (var13.equals("config.hardware.device")) {
                           var14 = 2;
                        }
                        break;
                     case 3373707:
                        if (var13.equals("name")) {
                           var14 = 0;
                        }
                        break;
                     case 814403083:
                        if (var13.equals("summary.config.vmPathName")) {
                           var14 = 3;
                        }
                     }

                     switch(var14) {
                     case 0:
                        vmData.name = (String)propValue.value;
                        break;
                     case 1:
                        vmData.primaryIconId = (String)propValue.value;
                        break;
                     case 2:
                        vmData.setVirtualDiskMaps((VirtualDevice[])((VirtualDevice[])propValue.value));
                        break;
                     case 3:
                        vmData.vmPathUuid = getVmHomeVsanUuid((String)propValue.value);
                        break;
                     case 4:
                        vmData.namespaceCapabilityMetadata = propValue.value;
                     }
                  }

                  HashMap var27 = result;
                  return var27;
               }

               result = new HashMap();
            } catch (Throwable var24) {
               var4 = var24;
               throw var24;
            } finally {
               if (point != null) {
                  if (var4 != null) {
                     try {
                        point.close();
                     } catch (Throwable var23) {
                        var4.addSuppressed(var23);
                     }
                  } else {
                     point.close();
                  }
               }

            }

            return result;
         } catch (Exception var26) {
            _logger.error("Failed to retrieve VM properties: ", var26);
            return new HashMap();
         }
      } else {
         return new HashMap();
      }
   }

   private static Set getVmsFromVsanObjects(ManagedObjectReference clusterRef, List vsanObjectIdentities) {
      Set vmRefs = new HashSet();
      if (vsanObjectIdentities != null) {
         Iterator var3 = vsanObjectIdentities.iterator();

         while(var3.hasNext()) {
            VsanObjectIdentity identity = (VsanObjectIdentity)var3.next();
            if (identity.vm != null) {
               ManagedObjectReference vmRef = identity.vm;
               VmodlHelper.assignServerGuid(vmRef, clusterRef.getServerGuid());
               vmRefs.add(vmRef);
            }
         }
      }

      return vmRefs;
   }

   public static Map getVsanUuidToStoragePolicyName(VsanObjectIdentityAndHealth vsanObjectIdentityAndHealth, VsanObjectInformation[] vsanObjectInformations, Map storagePolicies) {
      Map result = new HashMap();
      if (vsanObjectIdentityAndHealth != null && !ArrayUtils.isEmpty(vsanObjectIdentityAndHealth.identities)) {
         Map objInfoByUuid = getObjectInfoByVsanUuid(vsanObjectInformations);
         VsanObjectIdentity[] var5 = vsanObjectIdentityAndHealth.identities;
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            VsanObjectIdentity identity = var5[var7];
            if (StringUtils.isEmpty(identity.uuid)) {
               _logger.warn(String.format("Invalid UUID returned for type %s, additional description: %s.", identity.type, identity.description));
            } else {
               VsanObjectInformation objectInformation = (VsanObjectInformation)objInfoByUuid.get(identity.uuid);
               String storagePolicy;
               if (objectInformation != null) {
                  storagePolicy = storagePolicies.containsKey(objectInformation.spbmProfileUuid) ? (String)storagePolicies.get(objectInformation.spbmProfileUuid) : objectInformation.spbmProfileUuid;
               } else {
                  storagePolicy = storagePolicies.containsKey(identity.spbmProfileUuid) ? (String)storagePolicies.get(identity.spbmProfileUuid) : identity.spbmProfileUuid;
               }

               result.put(identity.uuid, storagePolicy);
            }
         }

         return result;
      } else {
         _logger.error("No data returned for object health and identities.");
         return result;
      }
   }

   private static Map getObjectInfoByVsanUuid(VsanObjectInformation[] vsanObjectInformations) {
      Map objInfoByVsanUuid = new HashMap();
      VsanObjectInformation[] var2 = vsanObjectInformations;
      int var3 = vsanObjectInformations.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         VsanObjectInformation info = var2[var4];
         objInfoByVsanUuid.put(info.vsanObjectUuid, info);
      }

      return objInfoByVsanUuid;
   }

   private static String getVmHomeVsanUuid(String vmFilePath) {
      if (vmFilePath == null) {
         return null;
      } else {
         int startIndex = vmFilePath.indexOf(93);
         int endIndex = vmFilePath.indexOf(47);
         return startIndex >= 0 && endIndex > startIndex ? vmFilePath.substring(startIndex + 1, endIndex).trim() : null;
      }
   }
}
