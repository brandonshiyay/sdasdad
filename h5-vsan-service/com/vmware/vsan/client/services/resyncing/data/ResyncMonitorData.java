package com.vmware.vsan.client.services.resyncing.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanObjectSyncState;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanSyncingObjectQueryResult;
import com.vmware.vsan.client.services.common.data.VmData;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceShare;
import com.vmware.vsphere.client.vsan.base.data.IscsiLun;
import com.vmware.vsphere.client.vsan.base.data.IscsiTarget;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.collections4.CollectionUtils;

@TsModel
public class ResyncMonitorData {
   public long etaToResync;
   public long activeETA;
   public long queuedETA;
   public long suspendedETA;
   public long bytesToResync;
   public long activeBytesToResync;
   public long queuedBytesToResync;
   public long suspendedBytesToResync;
   public long componentsToSync;
   public long activeComponentsToResync;
   public long queuedComponentsToResync;
   public long suspendedComponentsToResync;
   public DelayTimerData delayTimerData;
   public RepairTimerData repairTimerData;
   public boolean isResyncThrottlingSupported;
   public boolean isVsanClusterPartitioned;
   public boolean isResyncFilterApiSupported;
   public int resyncThrottlingValue;
   public SortedSet components;
   private Map hostUuidToHostNames;
   private Map componentsSyncData;

   public ResyncMonitorData() {
      this.componentsSyncData = new HashMap();
   }

   public ResyncMonitorData(VsanSyncingObjectQueryResult syncingObjectsData, Map hostUuidToHostNames) {
      this();
      this.etaToResync = syncingObjectsData.totalRecoveryETA;
      if (syncingObjectsData.syncingObjectRecoveryDetails != null) {
         if (syncingObjectsData.syncingObjectRecoveryDetails.getBytesToSyncForActiveObjects() != null) {
            this.activeComponentsToResync = syncingObjectsData.syncingObjectRecoveryDetails.getActiveObjectsToSync();
            this.activeETA = syncingObjectsData.syncingObjectRecoveryDetails.getActivelySyncingObjectRecoveryETA();
            this.activeBytesToResync = syncingObjectsData.syncingObjectRecoveryDetails.getBytesToSyncForActiveObjects();
         }

         if (syncingObjectsData.syncingObjectRecoveryDetails.getBytesToSyncForQueuedObjects() != null) {
            this.queuedComponentsToResync = syncingObjectsData.syncingObjectRecoveryDetails.getQueuedObjectsToSync();
            this.queuedETA = syncingObjectsData.syncingObjectRecoveryDetails.getQueuedForSyncObjectRecoveryETA();
            this.queuedBytesToResync = syncingObjectsData.syncingObjectRecoveryDetails.getBytesToSyncForQueuedObjects();
         }

         if (syncingObjectsData.syncingObjectRecoveryDetails.getBytesToSyncForSuspendedObjects() != null) {
            this.suspendedComponentsToResync = syncingObjectsData.syncingObjectRecoveryDetails.getSuspendedObjectsToSync();
            this.suspendedETA = syncingObjectsData.syncingObjectRecoveryDetails.getSuspendedObjectRecoveryETA();
            this.suspendedBytesToResync = syncingObjectsData.syncingObjectRecoveryDetails.getBytesToSyncForSuspendedObjects();
         }
      }

      this.componentsToSync = syncingObjectsData.totalObjectsToSync;
      this.bytesToResync = syncingObjectsData.totalBytesToSync;
      this.hostUuidToHostNames = hostUuidToHostNames;
      if (syncingObjectsData.objects != null) {
         VsanObjectSyncState[] var3 = syncingObjectsData.objects;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanObjectSyncState vsanObject = var3[var5];
            this.componentsSyncData.put(vsanObject.uuid, vsanObject);
         }
      }

      if (this.componentsSyncData.size() > 0) {
         this.components = new TreeSet(new ResyncComponent.ResyncComponentComparator());
      }

   }

   public Set getVsanObjectUuids() {
      return this.componentsSyncData.keySet();
   }

   public ResyncMonitorData uniteResyncingObjects(ResyncMonitorData resyncData) {
      this.etaToResync = Math.max(this.etaToResync, resyncData.etaToResync);
      this.bytesToResync += resyncData.bytesToResync;
      this.componentsToSync += resyncData.componentsToSync;
      this.componentsSyncData.putAll(resyncData.componentsSyncData);
      if (this.components == null && this.componentsSyncData.size() > 0) {
         this.components = new TreeSet(new ResyncComponent.ResyncComponentComparator());
      }

      return this;
   }

   public ResyncMonitorData processVmObjects(List vmObjectIdentities, Map vmDataMap, Map uuidToPolicyName) {
      Map vmResyncDataMap = new HashMap();

      VsanObjectIdentity objectIdentity;
      ResyncComponent vmResyncData;
      for(Iterator var5 = vmObjectIdentities.iterator(); var5.hasNext(); vmResyncData.addChildObject(objectIdentity.description, objectIdentity, (VsanObjectSyncState)this.componentsSyncData.get(objectIdentity.uuid), (String)uuidToPolicyName.get(objectIdentity.uuid), this.hostUuidToHostNames)) {
         objectIdentity = (VsanObjectIdentity)var5.next();
         VmData vmData = (VmData)vmDataMap.get(objectIdentity.vm);
         vmResyncData = (ResyncComponent)vmResyncDataMap.get(objectIdentity.vm);
         if (vmResyncData == null) {
            vmResyncData = new ResyncComponent(vmData);
            vmResyncDataMap.put(objectIdentity.vm, vmResyncData);
         }
      }

      Collection resyncComponents = vmResyncDataMap.values();
      Iterator var10 = resyncComponents.iterator();

      while(var10.hasNext()) {
         ResyncComponent vmResyncData = (ResyncComponent)var10.next();
         this.updateTotalBytesAndEtaToSync(vmResyncData);
      }

      this.components.addAll(resyncComponents);
      return this;
   }

   public ResyncMonitorData processVrObjects(List vmObjectIdentities, Map allVrWrapperDataMap, Map uuidToPolicyName) {
      Map resultResyncComponentsByUid = new HashMap();
      Iterator var5 = vmObjectIdentities.iterator();

      while(var5.hasNext()) {
         VsanObjectIdentity objectIdentity = (VsanObjectIdentity)var5.next();
         VsanObjectType identityType = VsanObjectType.parse(objectIdentity.type);
         String vrWrapperUid = VsanObjectType.hbrCfg.equals(identityType) ? objectIdentity.uuid : objectIdentity.vmNsObjectUuid;
         ResyncComponent vrWrapper = (ResyncComponent)allVrWrapperDataMap.get(vrWrapperUid);
         if (vrWrapper == null) {
            vrWrapperUid = null;
            vrWrapper = (ResyncComponent)allVrWrapperDataMap.get(vrWrapperUid);
         }

         this.addVrChildToVrWrapper(objectIdentity, vrWrapper, uuidToPolicyName);
         resultResyncComponentsByUid.put(vrWrapperUid, vrWrapper);
      }

      Collection resyncComponents = resultResyncComponentsByUid.values();
      Iterator var11 = resyncComponents.iterator();

      while(var11.hasNext()) {
         ResyncComponent vrResyncData = (ResyncComponent)var11.next();
         this.updateTotalBytesAndEtaToSync(vrResyncData);
      }

      this.components.addAll(resyncComponents);
      return this;
   }

   private void addVrChildToVrWrapper(VsanObjectIdentity objectIdentity, ResyncComponent vrWrapper, Map uuidTiPolicyName) {
      String name = null;
      String iconId = null;
      if (VsanObjectType.hbrCfg.name().equals(objectIdentity.type)) {
         name = Utils.getLocalizedString("vsan.resyncing.components.hbrCfg");
         iconId = "folder";
      } else {
         name = Utils.getLocalizedString("vsan.resyncing.components.hbrDisk");
         iconId = "disk-icon";
      }

      vrWrapper.addChildObject(name, iconId, objectIdentity, (VsanObjectSyncState)this.componentsSyncData.get(objectIdentity.uuid), (String)uuidTiPolicyName.get(objectIdentity.uuid), this.hostUuidToHostNames);
   }

   public ResyncMonitorData processOtherObjects(List vmObjectIdentities, List orphanedSyncObjects, Map uuidToPolicyName) {
      if (CollectionUtils.isEmpty(vmObjectIdentities) && CollectionUtils.isEmpty(orphanedSyncObjects)) {
         return this;
      } else {
         ResyncComponent othersComponent = new ResyncComponent();
         othersComponent.name = Utils.getLocalizedString("vsan.resyncing.components.other");
         Iterator var5;
         if (vmObjectIdentities != null) {
            var5 = vmObjectIdentities.iterator();

            while(var5.hasNext()) {
               VsanObjectIdentity objectIdentity = (VsanObjectIdentity)var5.next();
               othersComponent.addChildObject(objectIdentity.description, objectIdentity, (VsanObjectSyncState)this.componentsSyncData.get(objectIdentity.uuid), (String)uuidToPolicyName.get(objectIdentity.uuid), this.hostUuidToHostNames);
            }
         }

         if (orphanedSyncObjects != null) {
            var5 = orphanedSyncObjects.iterator();

            while(var5.hasNext()) {
               String orphanedSyncObject = (String)var5.next();
               VsanObjectIdentity objIdentity = new VsanObjectIdentity();
               objIdentity.setUuid(orphanedSyncObject);
               objIdentity.setType(VsanObjectType.other.toString());
               othersComponent.addChildObject(orphanedSyncObject, objIdentity, (VsanObjectSyncState)this.componentsSyncData.get(orphanedSyncObject), (String)uuidToPolicyName.get(orphanedSyncObject), this.hostUuidToHostNames);
            }
         }

         this.updateTotalBytesAndEtaToSync(othersComponent);
         this.components.add(othersComponent);
         return this;
      }
   }

   public ResyncMonitorData processFileShares(List identitiesList, Map uuidToPolicyName, List shares) {
      Map identities = identitiesToMap(identitiesList);
      Iterator var5 = shares.iterator();

      while(var5.hasNext()) {
         VsanFileServiceShare share = (VsanFileServiceShare)var5.next();
         ResyncComponent shareComponent = new ResyncComponent();
         shareComponent.name = share.config.name;
         this.components.add(shareComponent);
         shareComponent.iconId = "vsphere-icon-folder";
         Iterator var8 = share.objectUuids.iterator();

         while(var8.hasNext()) {
            String objectUuid = (String)var8.next();
            VsanObjectIdentity identity = (VsanObjectIdentity)identities.get(objectUuid);
            shareComponent.addChildObject(identity.description, identity, (VsanObjectSyncState)this.componentsSyncData.get(objectUuid), (String)uuidToPolicyName.get(objectUuid), this.hostUuidToHostNames);
         }

         this.updateTotalBytesAndEtaToSync(shareComponent);
      }

      return this;
   }

   public ResyncMonitorData processIscsiObjects(List vmObjectIdentities, Map uuidToPolicyName, Map iscsiObjects) {
      ResyncComponent iscsiComponent = new ResyncComponent();
      iscsiComponent.name = Utils.getLocalizedString("vsan.resyncing.components.iscsi");
      List iscsiLuns = new ArrayList();
      Iterator var6 = vmObjectIdentities.iterator();

      while(true) {
         while(var6.hasNext()) {
            VsanObjectIdentity objectIdentity = (VsanObjectIdentity)var6.next();
            if (iscsiObjects != null && iscsiObjects.containsKey(objectIdentity.uuid)) {
               VsanObject iscsiTargetObject = (VsanObject)iscsiObjects.get(objectIdentity.uuid);
               if (iscsiTargetObject instanceof IscsiTarget) {
                  ResyncComponent iscsiTargetComponent = new ResyncComponent(iscsiTargetObject, (VsanObjectSyncState)this.componentsSyncData.get(objectIdentity.uuid), (String)uuidToPolicyName.get(objectIdentity.uuid), this.hostUuidToHostNames);
                  iscsiTargetComponent.uuid = objectIdentity.uuid;
                  iscsiComponent.children.add(iscsiTargetComponent);
               } else if (iscsiTargetObject instanceof IscsiLun) {
                  iscsiLuns.add((IscsiLun)iscsiTargetObject);
               }
            } else {
               iscsiComponent.addChildObject(objectIdentity.description, objectIdentity, (VsanObjectSyncState)this.componentsSyncData.get(objectIdentity.uuid), (String)uuidToPolicyName.get(objectIdentity.uuid), this.hostUuidToHostNames);
            }
         }

         var6 = iscsiLuns.iterator();

         while(true) {
            Iterator var10;
            ResyncComponent iscsiTargetComponent;
            IscsiLun iscsiLun;
            String targetAlias;
            boolean parentTargetFound;
            do {
               if (!var6.hasNext()) {
                  this.updateTotalBytesAndEtaToSync(iscsiComponent);
                  this.components.add(iscsiComponent);
                  return this;
               }

               iscsiLun = (IscsiLun)var6.next();
               targetAlias = iscsiLun.targetAlias;
               parentTargetFound = false;
               var10 = iscsiComponent.children.iterator();

               while(var10.hasNext()) {
                  ResyncComponent iscsiTargetComponent = (ResyncComponent)var10.next();
                  if (iscsiTargetComponent.name.equals(targetAlias)) {
                     parentTargetFound = true;
                     iscsiTargetComponent = new ResyncComponent(iscsiLun, (VsanObjectSyncState)this.componentsSyncData.get(iscsiLun.vsanObjectUuid), (String)uuidToPolicyName.get(iscsiLun.vsanObjectUuid), this.hostUuidToHostNames);
                     iscsiTargetComponent.children.add(iscsiTargetComponent);
                  }
               }
            } while(parentTargetFound);

            var10 = iscsiObjects.values().iterator();

            while(var10.hasNext()) {
               VsanObject vsanObject = (VsanObject)var10.next();
               if (vsanObject instanceof IscsiTarget && ((IscsiTarget)vsanObject).alias.equals(targetAlias)) {
                  iscsiTargetComponent = new ResyncComponent(vsanObject, (VsanObjectSyncState)this.componentsSyncData.get(vsanObject.vsanObjectUuid), (String)uuidToPolicyName.get(vsanObject.vsanObjectUuid), this.hostUuidToHostNames);
                  iscsiComponent.children.add(iscsiTargetComponent);
                  ResyncComponent iscsiLunComponent = new ResyncComponent(iscsiLun, (VsanObjectSyncState)this.componentsSyncData.get(iscsiLun.vsanObjectUuid), (String)uuidToPolicyName.get(iscsiLun.vsanObjectUuid), this.hostUuidToHostNames);
                  iscsiTargetComponent.children.add(iscsiLunComponent);
               }
            }
         }
      }
   }

   private void updateTotalBytesAndEtaToSync(ResyncComponent resyncComponent) {
      ResyncComponent childComponent;
      for(Iterator var2 = resyncComponent.children.iterator(); var2.hasNext(); resyncComponent.etaToResync = Math.max(childComponent.etaToResync, resyncComponent.etaToResync)) {
         childComponent = (ResyncComponent)var2.next();
         if (childComponent.children.size() > 0) {
            this.updateTotalBytesAndEtaToSync(childComponent);
         }

         resyncComponent.bytesToResync += childComponent.bytesToResync;
      }

   }

   private static Map identitiesToMap(Collection identities) {
      Map identityMap = new HashMap();
      Iterator var2 = identities.iterator();

      while(var2.hasNext()) {
         VsanObjectIdentity identity = (VsanObjectIdentity)var2.next();
         identityMap.put(identity.uuid, identity);
      }

      return identityMap;
   }

   public String toString() {
      return "ResyncMonitorData(etaToResync=" + this.etaToResync + ", activeETA=" + this.activeETA + ", queuedETA=" + this.queuedETA + ", suspendedETA=" + this.suspendedETA + ", bytesToResync=" + this.bytesToResync + ", activeBytesToResync=" + this.activeBytesToResync + ", queuedBytesToResync=" + this.queuedBytesToResync + ", suspendedBytesToResync=" + this.suspendedBytesToResync + ", componentsToSync=" + this.componentsToSync + ", activeComponentsToResync=" + this.activeComponentsToResync + ", queuedComponentsToResync=" + this.queuedComponentsToResync + ", suspendedComponentsToResync=" + this.suspendedComponentsToResync + ", delayTimerData=" + this.delayTimerData + ", repairTimerData=" + this.repairTimerData + ", isResyncThrottlingSupported=" + this.isResyncThrottlingSupported + ", isVsanClusterPartitioned=" + this.isVsanClusterPartitioned + ", isResyncFilterApiSupported=" + this.isResyncFilterApiSupported + ", resyncThrottlingValue=" + this.resyncThrottlingValue + ", components=" + this.components + ", hostUuidToHostNames=" + this.hostUuidToHostNames + ", componentsSyncData=" + this.componentsSyncData + ")";
   }
}
