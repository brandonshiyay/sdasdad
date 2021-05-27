package com.vmware.vsan.client.services.resyncing.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanComponentSyncState;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanObjectSyncState;
import com.vmware.vsan.client.services.common.data.VmData;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectModelFactory;
import com.vmware.vsphere.client.vsan.base.data.IscsiLun;
import com.vmware.vsphere.client.vsan.base.data.IscsiTarget;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class ResyncComponent {
   public String name;
   public String iconId;
   public ManagedObjectReference moRef;
   public String storageProfile;
   public String hostName;
   public long bytesToResync;
   public long etaToResync;
   public ResyncComponent.ResyncReasonCode reason;
   public SortedSet children;
   public boolean isQueued;
   public String uuid;
   private ResyncComponent parent;
   private VmData vmData;

   public ResyncComponent() {
      this.isQueued = false;
      this.bytesToResync = 0L;
      this.etaToResync = -1L;
      this.children = new TreeSet(new ResyncComponent.ResyncComponentComparator());
   }

   public ResyncComponent(VmData vmData) {
      this();
      if (vmData != null) {
         this.name = vmData.name;
         this.iconId = vmData.primaryIconId;
         this.moRef = vmData.vmRef;
         this.vmData = vmData;
      }

   }

   public ResyncComponent(VsanObject iscsiData, VsanObjectSyncState syncData, String storagePolicyName, Map hostUuidToHostNames) {
      this();
      if (iscsiData != null) {
         this.uuid = iscsiData.vsanObjectUuid;
         if (iscsiData instanceof IscsiTarget) {
            this.name = ((IscsiTarget)iscsiData).alias;
            this.iconId = "iscsi-target-icon";
         } else if (iscsiData instanceof IscsiLun) {
            IscsiLun lun = (IscsiLun)iscsiData;
            String alias;
            if (!StringUtils.isEmpty(lun.alias)) {
               alias = lun.alias;
            } else {
               alias = "-";
            }

            this.name = Utils.getLocalizedString("vsan.virtualObjects.iscsiLun", alias, Integer.toString(lun.lunId));
            this.iconId = "iscsi-lun-icon";
         }
      }

      this.updateHealthData(storagePolicyName);
      if (syncData != null) {
         this.addComponents(syncData, hostUuidToHostNames);
      }

   }

   public ResyncComponent(String name, String hostName, long bytesToResync, long etaToResync, ResyncComponent.ResyncReasonCode reason) {
      this();
      this.name = name;
      this.hostName = hostName;
      this.bytesToResync = bytesToResync;
      this.etaToResync = etaToResync;
      this.reason = reason;
   }

   public ResyncComponent addChildObject(String name, String iconId, VsanObjectIdentity objectIdentity, VsanObjectSyncState syncData, String storagePolicyName, Map hostUuidToHostNames) {
      ResyncComponent child = new ResyncComponent();
      child.name = name;
      if (iconId != null) {
         child.iconId = iconId;
      }

      child.uuid = objectIdentity.uuid;
      if (this.vmData != null) {
         child.parent = this;
         child.processVmObjects(objectIdentity, storagePolicyName, this.vmData);
      }

      child.updateHealthData(storagePolicyName);
      if (syncData != null) {
         child.addComponents(syncData, hostUuidToHostNames);
      }

      this.children.add(child);
      return this;
   }

   public ResyncComponent addChildObject(String name, VsanObjectIdentity objectIdentity, VsanObjectSyncState syncData, String storagePolicyName, Map hostUuidToHostNames) {
      return this.addChildObject(name, (String)null, objectIdentity, syncData, storagePolicyName, hostUuidToHostNames);
   }

   private void processVmObjects(VsanObjectIdentity objectIdentity, String storagePolicyName, VmData vmData) {
      VsanObjectType objectIdentityType = VsanObjectType.parse(objectIdentity.type);
      this.name = VirtualObjectModelFactory.updateCommonVmObjectName(this.name, objectIdentityType);
      switch(objectIdentityType) {
      case vdisk:
         if (vmData.uuidToVirtualDiskMap.containsKey(objectIdentity.uuid)) {
            VirtualDisk virtualDisk = (VirtualDisk)vmData.uuidToVirtualDiskMap.get(objectIdentity.uuid);
            if (virtualDisk.deviceInfo != null) {
               this.name = virtualDisk.deviceInfo.label;
               this.iconId = "disk-icon";
            }
         } else if (vmData.uuidToDiskSnapshotMap.containsKey(objectIdentity.uuid)) {
            this.name = vmData.getSnapshotName(objectIdentity.uuid);
            this.iconId = "disk-icon";
         }
         break;
      case namespace:
         this.iconId = "vsphere-icon-folder";
         this.parent.updateHealthData(storagePolicyName);
         break;
      case hbrPersist:
         this.iconId = "folder";
      }

   }

   private void addComponents(VsanObjectSyncState syncData, Map hostUuidToHostNames) {
      VsanComponentSyncState[] var3 = syncData.components;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VsanComponentSyncState vmResyncComponent = var3[var5];
         ResyncComponent component = new ResyncComponent(vmResyncComponent.uuid, (String)hostUuidToHostNames.get(vmResyncComponent.hostUuid), vmResyncComponent.bytesToSync, vmResyncComponent.recoveryETA != null ? vmResyncComponent.recoveryETA : -1L, this.getResyncReason(vmResyncComponent.reasons));
         this.children.add(component);
      }

   }

   private void updateHealthData(String policyName) {
      if (policyName != null) {
         this.storageProfile = policyName;
      }

   }

   private ResyncComponent.ResyncReasonCode getResyncReason(String[] reasons) {
      if (ArrayUtils.isEmpty(reasons)) {
         return ResyncComponent.ResyncReasonCode.stale;
      } else {
         EnumSet resonsSet = EnumSet.noneOf(ResyncComponent.ResyncReasonCode.class);
         String[] var3 = reasons;
         int var4 = reasons.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String reason = var3[var5];
            resonsSet.add(ResyncComponent.ResyncReasonCode.valueOf(reason));
         }

         return (ResyncComponent.ResyncReasonCode)resonsSet.iterator().next();
      }
   }

   public String toString() {
      return "ResyncComponent(name=" + this.name + ", iconId=" + this.iconId + ", moRef=" + this.moRef + ", storageProfile=" + this.storageProfile + ", hostName=" + this.hostName + ", bytesToResync=" + this.bytesToResync + ", etaToResync=" + this.etaToResync + ", reason=" + this.reason + ", children=" + this.children + ", isQueued=" + this.isQueued + ", uuid=" + this.uuid + ", parent=" + this.parent + ", vmData=" + this.vmData + ")";
   }

   public static class ResyncComponentComparator implements Comparator {
      public int compare(ResyncComponent o1, ResyncComponent o2) {
         if (Utils.getLocalizedString("vsan.resyncing.components.other").equals(o1.name)) {
            return 1;
         } else if (Utils.getLocalizedString("vsan.resyncing.components.iscsi").equals(o1.name)) {
            return !Utils.getLocalizedString("vsan.resyncing.components.other").equals(o2.name) ? 1 : -1;
         } else if (Utils.getLocalizedString("vsan.resyncing.components.other").equals(o2.name)) {
            return -1;
         } else if (Utils.getLocalizedString("vsan.resyncing.components.iscsi").equals(o2.name)) {
            return !Utils.getLocalizedString("vsan.resyncing.components.other").equals(o1.name) ? -1 : 1;
         } else {
            int compareByName = o1.name.compareTo(o2.name);
            if (compareByName != 0) {
               return compareByName;
            } else if (o1.uuid == null) {
               return 1;
            } else {
               return o2.uuid == null ? -1 : o1.uuid.compareTo(o2.uuid);
            }
         }
      }
   }

   @TsModel
   public static enum ResyncStatusCode {
      active,
      queued,
      suspended;
   }

   @TsModel
   public static enum ResyncReasonCode {
      evacuate,
      dying_evacuate,
      rebalance,
      repair,
      reconfigure,
      stale,
      merge_concat,
      object_format_change,
      VsanSyncReason_Unknown;
   }
}
