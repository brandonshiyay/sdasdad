package com.vmware.vsan.client.services.virtualobjects.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vmware.vim.binding.vim.KeyValue;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.FileBackingInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTarget;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vim.vsan.binding.vim.vsan.FileShare;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class VirtualObjectModelFactory {
   private static final Log logger = LogFactory.getLog(VirtualObjectModelFactory.class);

   public List buildVms(VsanObjectIdentityAndHealth objectIdentityAndHealth, Set vmRefs, VsanObjectInformation[] vsanObjectInformations, Map dsProperties, Multimap snapshots, Map policies, boolean isObjectHealthV2Supported) {
      Multimap objInfosByUuid = this.getVirtualObjectModelByUuid(objectIdentityAndHealth, vsanObjectInformations, policies);
      Multimap allIdentitiesByUuid = mapAllObjIdentByUuid(objectIdentityAndHealth.identities);
      Multimap vmObjectsByVmRef = buildVmObjects(objInfosByUuid, allIdentitiesByUuid, dsProperties, snapshots);
      List vmModels = this.buildVmModels(vmRefs, vmObjectsByVmRef, dsProperties, isObjectHealthV2Supported);
      return vmModels;
   }

   private static Multimap buildVmObjects(Multimap objInfoMap, Multimap identitiesByUuid, Map dsProperties, Multimap snapshots) {
      Multimap vmObjects = HashMultimap.create();
      Iterator var5 = objInfoMap.values().iterator();

      while(true) {
         while(true) {
            VirtualObjectModel vmObjectModel;
            ManagedObjectReference vmRef;
            HashSet fcdVMs;
            Iterator var14;
            do {
               label66:
               do {
                  if (!var5.hasNext()) {
                     return vmObjects;
                  }

                  vmObjectModel = (VirtualObjectModel)var5.next();
                  vmRef = vmObjectModel.vmRef;
                  fcdVMs = new HashSet();
                  vmObjectModel.name = updateCommonVmObjectName(vmObjectModel.name, vmObjectModel.type.vmodlType);
                  switch(vmObjectModel.type.vmodlType) {
                  case namespace:
                     vmObjectModel.iconId = "folder";
                     break;
                  case improvedVirtualDisk:
                     vmObjectModel.iconId = "disk-icon";
                     var14 = identitiesByUuid.get(vmObjectModel.uid).iterator();

                     while(true) {
                        VsanObjectType identityType;
                        VsanObjectIdentity vsanObjIdentity;
                        do {
                           do {
                              if (!var14.hasNext()) {
                                 continue label66;
                              }

                              vsanObjIdentity = (VsanObjectIdentity)var14.next();
                              identityType = VsanObjectType.parse(vsanObjIdentity.type);
                           } while(vsanObjIdentity.vm == null);
                        } while(VsanObjectType.vdisk != identityType && VsanObjectType.namespace != identityType);

                        fcdVMs.add(vsanObjIdentity.vm);
                     }
                  case extension:
                  case attachedCnsVolBlock:
                     vmObjectModel.iconId = "cns-volume";
                     break;
                  case vdisk:
                     VirtualDevice[] devices = dsProperties.containsKey(vmRef) ? (VirtualDevice[])((VirtualDevice[])((Map)dsProperties.get(vmRef)).get("config.hardware.device")) : new VirtualDevice[0];
                     Collection snapshotConfig = snapshots.containsKey(vmRef) ? snapshots.get(vmRef) : Collections.emptyList();
                     vmObjectModel.name = getDiskLabel(vmObjectModel.uid, vmObjectModel.name, devices, (Collection)snapshotConfig);
                     vmObjectModel.iconId = "disk-icon";
                     break;
                  case hbrPersist:
                     vmObjectModel.iconId = "folder";
                  }
               } while(vmObjectModel.name == null);
            } while(vmRef == null);

            if (vmObjectModel.healthState == null) {
               vmObjectModel.healthState = VsanObjectHealthState.INACCESSIBLE;
            }

            if (!CollectionUtils.isEmpty(fcdVMs)) {
               var14 = fcdVMs.iterator();

               while(var14.hasNext()) {
                  ManagedObjectReference fcdVmRef = (ManagedObjectReference)var14.next();
                  vmObjects.put(fcdVmRef, vmObjectModel);
               }
            } else {
               vmObjects.put(vmObjectModel.vmRef, vmObjectModel);
            }
         }
      }
   }

   public static String updateCommonVmObjectName(String name, VsanObjectType type) {
      switch(type) {
      case namespace:
         name = Utils.getLocalizedString("vsan.virtualObjects.vmHome");
      case improvedVirtualDisk:
      case extension:
      case attachedCnsVolBlock:
      case vdisk:
      default:
         break;
      case hbrPersist:
         name = Utils.getLocalizedString("vsan.virtualObjects.hbrPersist");
         break;
      case vmswap:
         name = Utils.getLocalizedString("vsan.virtualObjects.vmSwap");
         break;
      case vmem:
         name = Utils.getLocalizedString("vsan.virtualObjects.vmMemory");
      }

      return name;
   }

   private List buildVmModels(Set vmRefs, Multimap vmObjectsByVmRef, Map dsProperties, boolean isObjectHealthV2Supported) {
      List vmModels = new ArrayList();
      Iterator var6 = vmRefs.iterator();

      while(var6.hasNext()) {
         ManagedObjectReference vmRef = (ManagedObjectReference)var6.next();
         Map vmProperties = (Map)dsProperties.get(vmRef);
         VirtualObjectModel vmModel = new VirtualObjectModel();
         vmModel.vmRef = vmRef;
         vmModel.type = new VirtualObjectType(DisplayObjectType.VM);
         if (vmProperties != null && !vmProperties.isEmpty()) {
            vmModel.iconId = vmProperties.get("primaryIconId") + "";
            vmModel.name = vmProperties.get("name") + "";
         } else {
            vmModel.iconId = "vsphere-icon-vm";
            vmModel.name = vmRef.getValue();
         }

         vmModel.children = (VirtualObjectModel[])vmObjectsByVmRef.get(vmRef).toArray(new VirtualObjectModel[0]);
         if (isPersistenceServiceSupported(vmModel)) {
            VirtualObjectModel[] var10 = vmModel.children;
            int var11 = var10.length;

            for(int var12 = 0; var12 < var11; ++var12) {
               VirtualObjectModel child = var10[var12];
               if (child.type.vmodlType == VsanObjectType.extension) {
                  vmModel.type.displayType = DisplayObjectType.EXTENSION_APP;
                  vmModel.type.extendedTypeName = child.type.extendedTypeName;
                  vmModel.type.extendedTypeId = child.type.extendedTypeId;
                  vmModel.applicationInstanceId = child.applicationInstanceId;
                  break;
               }
            }
         }

         Arrays.sort(vmModel.children, VirtualObjectModel.COMPARATOR);
         this.aggregateWrapperObjectDetails(vmModel, isObjectHealthV2Supported);
         vmModels.add(vmModel);
      }

      Collections.sort(vmModels, VirtualObjectModel.COMPARATOR);
      return vmModels;
   }

   private void aggregateWrapperObjectDetails(VirtualObjectModel wrapperModel, boolean isObjectHealthV2Supported) {
      if (isObjectHealthV2Supported) {
         wrapperModel.compositeHealth = VirtualObjectsUtil.aggregateWrapperObjectHealthV2(wrapperModel);
      } else {
         wrapperModel.healthState = VirtualObjectsUtil.aggregateWrapperObjectHealthV1(wrapperModel);
      }

      wrapperModel.storagePolicy = VirtualObjectsUtil.getCommonStoragePolicy(wrapperModel);
   }

   public List buildVrObjects(VsanObjectIdentityAndHealth identities, VsanObjectInformation[] vsanObjectInformations, Map storagePolicies, boolean isObjectHealthV2Supported) {
      if (ArrayUtils.isEmpty(identities.identities)) {
         return Collections.EMPTY_LIST;
      } else {
         Map objInfoByUuid = getObjectInfoByVsanUuid(vsanObjectInformations);
         Map objectHealthByUuid = VirtualObjectsUtil.getVsanObjectsHealthMap(identities.health);
         Map replicatedWrappersByCfgId = new HashMap();
         MultiValuedMap hbrDisksByCfgId = new HashSetValuedHashMap();
         VsanObjectIdentity[] var9 = identities.identities;
         int var10 = var9.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            VsanObjectIdentity identity = var9[var11];
            switch(VsanObjectType.parse(identity.type)) {
            case hbrCfg:
               VirtualObjectModel replicationWrapper = buildReplicationWrapper(Utils.getLocalizedString("vsan.virtualObjects.hbr.wrapper", identity.description));
               replicatedWrappersByCfgId.put(identity.uuid, replicationWrapper);
               replicationWrapper.children = new VirtualObjectModel[]{buildReplicationCfg(identity, (VsanObjectInformation)objInfoByUuid.get(identity.uuid), (VirtualObjectHealthModel)objectHealthByUuid.get(identity.uuid), storagePolicies)};
               break;
            case hbrDisk:
               hbrDisksByCfgId.put(identity.vmNsObjectUuid, buildReplicationDisk(identity, (VsanObjectInformation)objInfoByUuid.get(identity.uuid), (VirtualObjectHealthModel)objectHealthByUuid.get(identity.uuid), storagePolicies));
            }
         }

         Iterator var14 = replicatedWrappersByCfgId.keySet().iterator();

         while(var14.hasNext()) {
            String replicaCfgId = (String)var14.next();
            VirtualObjectModel wrapper = (VirtualObjectModel)replicatedWrappersByCfgId.get(replicaCfgId);
            List children = new ArrayList(Arrays.asList(wrapper.children));
            Collection hbrDisks = hbrDisksByCfgId.remove(replicaCfgId);
            if (hbrDisks != null) {
               children.addAll(hbrDisks);
               wrapper.children = (VirtualObjectModel[])children.toArray(new VirtualObjectModel[0]);
            }
         }

         List result = new ArrayList(replicatedWrappersByCfgId.values());
         Collections.sort(result, VirtualObjectModel.COMPARATOR);
         if (hbrDisksByCfgId.size() > 0) {
            logger.warn("There are Replica Disks that are not matched to any Replication Config and will be put under generic category");
            VirtualObjectModel defaultWrapper = buildReplicationWrapper(Utils.getLocalizedString("vsan.virtualObjects.hbr.wrapper.generic"));
            defaultWrapper.children = (VirtualObjectModel[])hbrDisksByCfgId.values().toArray(new VirtualObjectModel[0]);
            result.add(defaultWrapper);
         }

         result.stream().forEach((wrapperx) -> {
            this.aggregateWrapperObjectDetails(wrapperx, isObjectHealthV2Supported);
         });
         return result;
      }
   }

   public List buildOthers(Set allVsanUuids, VsanObjectIdentityAndHealth objectIdentityAndHealth, VsanObjectInformation[] vsanObjectInformations, Map policies) {
      Multimap objInfoByUuid = this.getVirtualObjectModelByUuid(objectIdentityAndHealth, vsanObjectInformations, policies);
      List otherObjects = new ArrayList();
      Iterator var7 = allVsanUuids.iterator();

      while(var7.hasNext()) {
         String vsanUuid = (String)var7.next();
         Iterator var9 = objInfoByUuid.get(vsanUuid).iterator();

         while(var9.hasNext()) {
            VirtualObjectModel model = (VirtualObjectModel)var9.next();
            if (model != null && model.isOtherType()) {
               otherObjects.add(model);
            }
         }
      }

      Collections.sort(otherObjects, VirtualObjectModel.COMPARATOR);
      return otherObjects;
   }

   public List buildExtensionObjectsWithoutVm(VsanObjectIdentityAndHealth objectIdentityAndHealth, Map policies) {
      return (List)this.getVirtualObjectModelByUuid(objectIdentityAndHealth, new VsanObjectInformation[0], policies).values().stream().filter((object) -> {
         return object.type.vmodlType == VsanObjectType.extension;
      }).filter((object) -> {
         return object.vmRef == null;
      }).map((object) -> {
         object.iconId = "extension";
         return object;
      }).collect(Collectors.toList());
   }

   public List buildIscsiTargets(VsanIscsiTarget[] vsanIscsiTargets, VsanIscsiLUN[] vsanIscsiLUNs, Map policies) {
      List result = new ArrayList();
      VsanIscsiTarget[] var5 = vsanIscsiTargets;
      int var6 = vsanIscsiTargets.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         VsanIscsiTarget iscsiTarget = var5[var7];
         VirtualObjectModel iscsiModel = fromVsanObjectInformation((VsanObjectIdentity)null, iscsiTarget.objectInformation, (VirtualObjectHealthModel)null, policies);
         iscsiModel.name = iscsiTarget.alias;
         iscsiModel.iconId = "iscsi-target-icon";
         iscsiModel.type = new VirtualObjectType(VsanObjectType.iscsiTarget);
         result.add(iscsiModel);
         if (vsanIscsiLUNs != null) {
            List lunModels = new ArrayList();
            VsanIscsiLUN[] var11 = vsanIscsiLUNs;
            int var12 = vsanIscsiLUNs.length;

            for(int var13 = 0; var13 < var12; ++var13) {
               VsanIscsiLUN lun = var11[var13];
               if (lun.targetAlias.equals(iscsiTarget.alias)) {
                  VirtualObjectModel lunModel = fromVsanObjectInformation((VsanObjectIdentity)null, lun.objectInformation, (VirtualObjectHealthModel)null, policies);
                  lunModel.name = Utils.getLocalizedString("vsan.virtualObjects.iscsiLun", lun.alias != null ? lun.alias : "", Integer.toString(lun.lunId != null ? lun.lunId : 0)).trim();
                  lunModel.iconId = "iscsi-lun-icon";
                  lunModel.type = new VirtualObjectType(VsanObjectType.iscsiLun);
                  lunModels.add(lunModel);
               }
            }

            iscsiModel.children = (VirtualObjectModel[])lunModels.toArray(new VirtualObjectModel[lunModels.size()]);
            Arrays.sort(iscsiModel.children, VirtualObjectModel.COMPARATOR);
         }
      }

      Collections.sort(result, VirtualObjectModel.COMPARATOR);
      return result;
   }

   public List buildFcds(VsanObjectIdentityAndHealth objectIdentityAndHealth, VsanObjectInformation[] vsanObjectInformations, Map policies) {
      List allFcds = new ArrayList();
      Multimap vsanObjectInfosByUuid = this.getVirtualObjectModelByUuid(objectIdentityAndHealth, vsanObjectInformations, policies);
      Iterator var6 = vsanObjectInfosByUuid.values().iterator();

      while(true) {
         VirtualObjectModel model;
         label24:
         while(true) {
            do {
               if (!var6.hasNext()) {
                  Collections.sort(allFcds, VirtualObjectModel.COMPARATOR);
                  return allFcds;
               }

               model = (VirtualObjectModel)var6.next();
            } while(model.vmRef != null);

            switch(model.type.vmodlType) {
            case improvedVirtualDisk:
               model.iconId = "disk-icon";
               break label24;
            case detachedCnsVolBlock:
               model.iconId = "cns-volume";
               break label24;
            }
         }

         allFcds.add(model);
      }
   }

   public List buildFileShares(List fileShares, VsanObjectIdentityAndHealth objectIdentityAndHealth, VsanObjectInformation[] vsanObjectInformations, Map policies, boolean isObjectHealthV2Supported) {
      if (CollectionUtils.isEmpty(fileShares)) {
         return Collections.EMPTY_LIST;
      } else {
         List result = new ArrayList(fileShares.size());
         Multimap objInfoMap = this.getVirtualObjectModelByUuid(objectIdentityAndHealth, vsanObjectInformations, policies);
         Iterator var8 = fileShares.iterator();

         while(true) {
            FileShare fileShare;
            VirtualObjectModel share;
            do {
               if (!var8.hasNext()) {
                  return result;
               }

               fileShare = (FileShare)var8.next();
               share = new VirtualObjectModel();
               share.name = fileShare.config.name;
               share.iconId = "vsphere-icon-folder";
               share.type = new VirtualObjectType(VsanObjectType.fileShare);
               result.add(share);
            } while(ArrayUtils.isEmpty(fileShare.runtime.vsanObjectUuids));

            List children = new ArrayList(fileShare.runtime.vsanObjectUuids.length);
            String[] var12 = fileShare.runtime.vsanObjectUuids;
            int var13 = var12.length;

            for(int var14 = 0; var14 < var13; ++var14) {
               String childUuid = var12[var14];

               VirtualObjectModel shareObject;
               for(Iterator var16 = objInfoMap.get(childUuid).iterator(); var16.hasNext(); children.add(shareObject)) {
                  shareObject = (VirtualObjectModel)var16.next();
                  if (shareObject.type.vmodlType == VsanObjectType.cnsVolFile) {
                     share.name = shareObject.name;
                     share.iconId = "cns-file-volume";
                     share.type = new VirtualObjectType(VsanObjectType.cnsVolFile);
                     shareObject.type.displayType = DisplayObjectType.FILE_VOLUME_OBJECT;
                     shareObject.name = fileShare.config.name;
                  } else {
                     shareObject.type = new VirtualObjectType(VsanObjectType.fileShare);
                     shareObject.type.displayType = DisplayObjectType.FILE_SHARE_OBJECT;
                  }
               }
            }

            share.children = (VirtualObjectModel[])children.toArray(new VirtualObjectModel[0]);
            this.aggregateWrapperObjectDetails(share, isObjectHealthV2Supported);
         }
      }
   }

   private static Multimap mapAllObjIdentByUuid(VsanObjectIdentity[] identities) {
      Multimap objIdentityByVsanUuid = ArrayListMultimap.create();
      if (ArrayUtils.isEmpty(identities)) {
         return objIdentityByVsanUuid;
      } else {
         VsanObjectIdentity[] var2 = identities;
         int var3 = identities.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanObjectIdentity identity = var2[var4];
            objIdentityByVsanUuid.put(identity.uuid, identity);
         }

         return objIdentityByVsanUuid;
      }
   }

   private Multimap getVirtualObjectModelByUuid(VsanObjectIdentityAndHealth identitiesAndHealth, VsanObjectInformation[] vsanObjectInformations, Map policies) {
      if (ArrayUtils.isEmpty(identitiesAndHealth.identities)) {
         return HashMultimap.create();
      } else {
         Map vmByVsanUuid = getVmsByVsanUuid(identitiesAndHealth.identities);
         Map objInfoByUuid = getObjectInfoByVsanUuid(vsanObjectInformations);
         Map objectHealthByUuid = VirtualObjectsUtil.getVsanObjectsHealthMap(identitiesAndHealth.health);
         Multimap result = HashMultimap.create();
         VsanObjectIdentity[] var8 = identitiesAndHealth.identities;
         int var9 = var8.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            VsanObjectIdentity identity = var8[var10];
            VirtualObjectModel objData = fromVsanObjectInformation(identity, (VsanObjectInformation)objInfoByUuid.get(identity.uuid), (VirtualObjectHealthModel)objectHealthByUuid.get(identity.uuid), policies);
            if (identity.vm == null) {
               identity.vm = (ManagedObjectReference)vmByVsanUuid.get(identity.uuid);
            }

            result.put(identity.uuid, objData);
         }

         return result;
      }
   }

   private static Map getVmsByVsanUuid(VsanObjectIdentity[] identities) {
      Map vmByVsanUuid = new HashMap();
      VsanObjectIdentity[] var2 = identities;
      int var3 = identities.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         VsanObjectIdentity identity = var2[var4];
         if (identity.vm != null) {
            vmByVsanUuid.put(identity.uuid, identity.vm);
         }
      }

      return vmByVsanUuid;
   }

   private static Map getObjectInfoByVsanUuid(VsanObjectInformation[] vsanObjectInformations) {
      if (ArrayUtils.isEmpty(vsanObjectInformations)) {
         return Collections.EMPTY_MAP;
      } else {
         Map objInfoByVsanUuid = new HashMap();
         VsanObjectInformation[] var2 = vsanObjectInformations;
         int var3 = vsanObjectInformations.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanObjectInformation info = var2[var4];
            objInfoByVsanUuid.put(info.vsanObjectUuid, info);
         }

         return objInfoByVsanUuid;
      }
   }

   private static VirtualObjectModel buildReplicationWrapper(String name) {
      VirtualObjectModel result = new VirtualObjectModel();
      result.iconId = "vr-replication-wrapper";
      result.type = new VirtualObjectType(DisplayObjectType.REPLICATION);
      result.name = name;
      return result;
   }

   private static VirtualObjectModel buildReplicationCfg(VsanObjectIdentity identity, VsanObjectInformation objectInformation, VirtualObjectHealthModel objectHealth, Map policies) {
      VirtualObjectModel result = fromVsanObjectInformation(identity, objectInformation, objectHealth, policies);
      result.name = Utils.getLocalizedString("vsan.virtualObjects.hbrCfg");
      result.iconId = "folder";
      result.type = new VirtualObjectType(DisplayObjectType.FOLDER);
      return result;
   }

   private static VirtualObjectModel buildReplicationDisk(VsanObjectIdentity identity, VsanObjectInformation objectInformation, VirtualObjectHealthModel objectHealth, Map policies) {
      VirtualObjectModel result = fromVsanObjectInformation(identity, objectInformation, objectHealth, policies);
      result.name = Utils.getLocalizedString("vsan.virtualObjects.hbrDisk", extractFileName(identity.description));
      result.iconId = "disk-icon";
      result.type = new VirtualObjectType(VsanObjectType.hbrDisk);
      return result;
   }

   private static String extractFileName(String fullPath) {
      return fullPath.substring(fullPath.lastIndexOf("/") + 1);
   }

   private static VirtualObjectModel fromVsanObjectInformation(VsanObjectIdentity identity, VsanObjectInformation objectInformation, VirtualObjectHealthModel objectHealth, Map policies) {
      VirtualObjectModel result = new VirtualObjectModel();
      if (identity != null) {
         result.uid = identity.uuid;
         result.name = identity.description;
         result.vmRef = identity.vm;
         result.type = new VirtualObjectType(identity.type);
         if (ArrayUtils.isNotEmpty(identity.metadatas)) {
            KeyValue[] var5 = identity.metadatas;
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               KeyValue metadata = var5[var7];
               String var9 = metadata.key;
               byte var10 = -1;
               switch(var9.hashCode()) {
               case -1962487731:
                  if (var9.equals("appplatform.vmware.com/extension-desc")) {
                     var10 = 1;
                  }
                  break;
               case -1509433269:
                  if (var9.equals("vsanDirectDiskUuid")) {
                     var10 = 3;
                  }
                  break;
               case 699545381:
                  if (var9.equals("appplatform.vmware.com/instance-id")) {
                     var10 = 2;
                  }
                  break;
               case 1557732823:
                  if (var9.equals("appplatform.vmware.com/extension-id")) {
                     var10 = 0;
                  }
               }

               switch(var10) {
               case 0:
                  result.type.extendedTypeId = metadata.value;
                  break;
               case 1:
                  result.type.extendedTypeName = metadata.value;
                  break;
               case 2:
                  result.applicationInstanceId = metadata.value;
                  break;
               case 3:
                  result.diskUuid = metadata.value;
                  break;
               default:
                  logger.warn("Unknonw property: " + metadata.key);
               }
            }
         }
      } else {
         result.uid = result.name = objectInformation.vsanObjectUuid;
      }

      if (objectInformation != null) {
         result.storagePolicy = policies.containsKey(objectInformation.spbmProfileUuid) ? (String)policies.get(objectInformation.spbmProfileUuid) : objectInformation.spbmProfileUuid;
         result.healthState = VsanObjectHealthState.fromString(objectInformation.vsanHealth);
      } else {
         result.storagePolicy = policies.containsKey(identity.spbmProfileUuid) ? (String)policies.get(identity.spbmProfileUuid) : identity.spbmProfileUuid;
         if (objectHealth != null) {
            result.healthState = objectHealth.health;
            result.compositeHealth = objectHealth.compositeHealth;
         } else {
            result.healthState = VsanObjectHealthState.UNKNOWN;
         }
      }

      return result;
   }

   private static String getDiskLabel(String uuid, String objectName, VirtualDevice[] devices, Collection configSnapshots) {
      VirtualDisk disk = VirtualObjectsUtil.findDisk(devices, uuid);
      if (disk != null) {
         return disk.deviceInfo.label;
      } else {
         Iterator var5 = configSnapshots.iterator();

         do {
            if (!var5.hasNext()) {
               return objectName;
            }

            ConfigInfo configSnapshot = (ConfigInfo)var5.next();
            disk = VirtualObjectsUtil.findDisk(configSnapshot.hardware.device, uuid);
         } while(disk == null);

         String path = ((FileBackingInfo)disk.backing).fileName;
         int lastSeparator = path.lastIndexOf(47);
         if (lastSeparator != -1) {
            path = path.substring(lastSeparator + 1);
         }

         return Utils.getLocalizedString("vsan.virtualObjects.vmSnapshot", disk.deviceInfo.label, path);
      }
   }

   private static boolean isPersistenceServiceSupported(VirtualObjectModel model) {
      return model != null && model.vmRef != null && StringUtils.isNotEmpty(model.vmRef.getServerGuid()) && VsanCapabilityUtils.isPersistenceServiceSupportedOnVc(model.vmRef);
   }

   public static class Metadata {
      public static final String EXTENSION_ID = "appplatform.vmware.com/extension-id";
      public static final String INSTANCE_ID = "appplatform.vmware.com/instance-id";
      public static final String EXTENSION_NAME = "appplatform.vmware.com/extension-desc";
      public static final String DISK_UUID = "vsanDirectDiskUuid";
   }
}
