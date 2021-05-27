package com.vmware.vsan.client.services.physicaldisks;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiLun.State;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.diskmanagement.DiskStatus;
import com.vmware.vsan.client.services.diskmanagement.DiskStatusKey;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectModel;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.data.HostPhysicalMappingData;
import com.vmware.vsphere.client.vsan.data.PhysicalDiskData;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TsModel
public class PhysicalDisksHierarchicalData {
   private static final Logger logger = LoggerFactory.getLogger(PhysicalDisksHierarchicalData.class);
   private static final long FLAG_NOT_AVAILABLE = -1L;
   public String name;
   public String iconId;
   public ManagedObjectReference hostRef;
   public String faultDomain;
   public String vsanUuid;
   public Boolean isSsd;
   public long capacity;
   public long usedCapacity;
   public Long reservedCapacity;
   public PhysicalDisksHierarchicalData.DeviceState state;
   public Boolean ineligible;
   public String[] issues;
   public ClaimOption claimOption;
   public DiskStatus diskStatus;
   public long componentsNumber;
   public String[] physicalLocation;
   public List children = new ArrayList();
   private List virtualObjectUuids = new ArrayList();
   public List virtualObjects = new ArrayList();
   public Object[] deviceAdaptersData;

   public PhysicalDisksHierarchicalData() {
   }

   public PhysicalDisksHierarchicalData(HostPhysicalMappingData item) {
      this.name = item.name;
      this.iconId = item.primaryIconId;
      this.hostRef = item.hostRef;
      this.faultDomain = item.faultDomain;
      this.capacity = 0L;
      this.usedCapacity = 0L;
      this.reservedCapacity = 0L;
      this.componentsNumber = 0L;
      this.deviceAdaptersData = item.storageAdapterDevices;
      Map groupsByUuid = new HashMap();
      if (CollectionUtils.isNotEmpty(item.physicalDisks)) {
         Iterator var3 = item.physicalDisks.iterator();

         while(var3.hasNext()) {
            PhysicalDiskData physicalDisk = (PhysicalDiskData)var3.next();
            if (ClaimOption.isClaimedForVsan(physicalDisk.claimOption)) {
               this.addVsanDiskToDiskGroup(groupsByUuid, physicalDisk);
            } else {
               this.addManagedByVsanStorageToDiskGroup(groupsByUuid, physicalDisk, physicalDisk.claimOption.name());
            }
         }
      }

      this.addChildren(groupsByUuid.values());
   }

   private void addVsanDiskToDiskGroup(Map groupsByUuid, PhysicalDiskData physicalDisk) {
      PhysicalDisksHierarchicalData diskItem = mapToVsanDiskData(physicalDisk);
      PhysicalDisksHierarchicalData groupItem = (PhysicalDisksHierarchicalData)groupsByUuid.get(physicalDisk.vsanDiskGroupUuid);
      if (groupItem == null) {
         groupItem = createDiskGroupDataItem(physicalDisk);
         groupsByUuid.put(groupItem.vsanUuid, groupItem);
      }

      groupItem.addChild(diskItem, diskItem.claimOption == ClaimOption.ClaimForStorage);
   }

   private void addManagedByVsanStorageToDiskGroup(Map groupsByUuid, PhysicalDiskData physicalDisk, String diskGroupUuid) {
      PhysicalDisksHierarchicalData groupItem = (PhysicalDisksHierarchicalData)groupsByUuid.get(diskGroupUuid);
      if (groupItem == null) {
         groupItem = new PhysicalDisksHierarchicalData();
         groupsByUuid.put(diskGroupUuid, groupItem);
      }

      groupItem.addChild(mapToManagedByVsanStorageData(physicalDisk), true);
   }

   private static PhysicalDisksHierarchicalData mapToVsanDiskData(PhysicalDiskData physicalDisk) {
      PhysicalDisksHierarchicalData diskItem = mapToDiskData(physicalDisk);
      diskItem.reservedCapacity = parseCapacity(physicalDisk.reservedCapacity);
      diskItem.state = PhysicalDisksHierarchicalData.DeviceState.fromScsiState(physicalDisk.operationalState);
      if (physicalDisk.virtualDiskUuids != null) {
         diskItem.componentsNumber = (long)physicalDisk.virtualDiskUuids.size();
         diskItem.physicalLocation = physicalDisk.physicalLocation;
         diskItem.virtualObjectUuids = physicalDisk.virtualDiskUuids;
      }

      return diskItem;
   }

   private static PhysicalDisksHierarchicalData mapToManagedByVsanStorageData(PhysicalDiskData physicalDisk) {
      PhysicalDisksHierarchicalData diskItem = mapToDiskData(physicalDisk);
      boolean isMounted = (Boolean)physicalDisk.diskStatus.additionalStatuses.get(DiskStatusKey.IS_MOUNTED);
      diskItem.state = isMounted ? PhysicalDisksHierarchicalData.DeviceState.OK : PhysicalDisksHierarchicalData.DeviceState.OFF;
      if (physicalDisk.virtualDiskUuids != null) {
         diskItem.componentsNumber = (long)physicalDisk.virtualDiskUuids.size();
         diskItem.virtualObjectUuids = physicalDisk.virtualDiskUuids;
      }

      return diskItem;
   }

   private static PhysicalDisksHierarchicalData mapToDiskData(PhysicalDiskData physicalDisk) {
      PhysicalDisksHierarchicalData diskItem = createGroupingDataItem(physicalDisk.diskName, physicalDisk.uuid);
      diskItem.isSsd = physicalDisk.isSsd;
      diskItem.capacity = physicalDisk.capacity;
      diskItem.usedCapacity = parseCapacity(physicalDisk.usedCapacity);
      diskItem.claimOption = physicalDisk.claimOption;
      diskItem.diskStatus = physicalDisk.diskStatus;
      diskItem.ineligible = physicalDisk.ineligible;
      if (physicalDisk.diskIssue != null) {
         diskItem.issues = new String[]{physicalDisk.diskIssue};
      }

      return diskItem;
   }

   private static PhysicalDisksHierarchicalData createDiskGroupDataItem(PhysicalDiskData diskData) {
      PhysicalDisksHierarchicalData diskGroupItem = createGroupingDataItem(Utils.getLocalizedString("vsan.evacuationStatus.formattedDiskGroupName", diskData.vsanDiskGroupUuid), diskData.vsanDiskGroupUuid);
      return diskGroupItem;
   }

   private static PhysicalDisksHierarchicalData createGroupingDataItem(String name, String uuid) {
      PhysicalDisksHierarchicalData groupingDataItem = new PhysicalDisksHierarchicalData();
      groupingDataItem.name = name;
      groupingDataItem.vsanUuid = uuid;
      return groupingDataItem;
   }

   private void addChild(PhysicalDisksHierarchicalData child, boolean accumulateCapacity) {
      this.children.add(child);
      this.componentsNumber += child.componentsNumber;
      this.virtualObjectUuids.addAll(child.virtualObjectUuids);
      if (accumulateCapacity) {
         this.capacity += child.capacity;
         this.usedCapacity += child.usedCapacity;
         if (child.reservedCapacity != null) {
            if (this.reservedCapacity == null) {
               this.reservedCapacity = 0L;
            }

            this.reservedCapacity = this.reservedCapacity + child.reservedCapacity;
         }
      }

   }

   private void addChildren(Collection children) {
      Iterator var2 = children.iterator();

      while(var2.hasNext()) {
         PhysicalDisksHierarchicalData child = (PhysicalDisksHierarchicalData)var2.next();
         this.addChild(child, true);
      }

   }

   public List getVirtualObjectUuids() {
      return this.virtualObjectUuids;
   }

   public void setVirtualObjectsData(List virtualObjects) {
      if (!CollectionUtils.isEmpty(virtualObjects)) {
         PhysicalDisksHierarchicalData.VirtualObjectModelCollector hostVirtualObjectsCollector = new PhysicalDisksHierarchicalData.VirtualObjectModelCollector();
         Iterator var3 = this.children.iterator();

         while(true) {
            PhysicalDisksHierarchicalData groupData;
            do {
               if (!var3.hasNext()) {
                  this.virtualObjects = hostVirtualObjectsCollector.toList();
                  return;
               }

               groupData = (PhysicalDisksHierarchicalData)var3.next();
            } while(CollectionUtils.isEmpty(groupData.virtualObjectUuids) && this.isVsanDiskGroup(groupData));

            PhysicalDisksHierarchicalData.VirtualObjectModelCollector groupVirtualObjectsCollector = new PhysicalDisksHierarchicalData.VirtualObjectModelCollector();
            Iterator var6 = groupData.children.iterator();

            while(var6.hasNext()) {
               PhysicalDisksHierarchicalData diskData = (PhysicalDisksHierarchicalData)var6.next();
               Set diskObjectUuids = new HashSet(diskData.virtualObjectUuids);
               Iterator var9 = virtualObjects.iterator();

               while(var9.hasNext()) {
                  VirtualObjectModel virtualObject = (VirtualObjectModel)var9.next();
                  VirtualObjectModel clone;
                  if (this.isVsanDirectObject(virtualObject, diskData.vsanUuid, diskObjectUuids)) {
                     this.incrementVirtualObjectsCount(virtualObject, groupData, diskData);
                     clone = virtualObject.cloneWithoutChildren();
                  } else {
                     clone = cloneVirtualObject(virtualObject, diskObjectUuids);
                  }

                  if (clone != null) {
                     diskData.virtualObjects.add(clone);
                     groupVirtualObjectsCollector.addVirtualObject(clone);
                     hostVirtualObjectsCollector.addVirtualObject(clone);
                  }
               }
            }

            groupData.virtualObjects = groupVirtualObjectsCollector.toList();
         }
      }
   }

   private boolean isVsanDiskGroup(PhysicalDisksHierarchicalData groupData) {
      return groupData.children.stream().allMatch((disk) -> {
         return ClaimOption.isClaimedForVsan(disk.claimOption);
      });
   }

   private boolean isVsanDirectObject(VirtualObjectModel vo, String diskUuid, Set knownObjectUuids) {
      boolean isNotMappedObject = !knownObjectUuids.contains(vo.uid);
      boolean isMatchingByDiskUuid = diskUuid.equalsIgnoreCase(vo.diskUuid);
      return isNotMappedObject && isMatchingByDiskUuid;
   }

   private void incrementVirtualObjectsCount(VirtualObjectModel virtualObject, PhysicalDisksHierarchicalData groupData, PhysicalDisksHierarchicalData diskData) {
      this.virtualObjectUuids.add(virtualObject.uid);
      ++this.componentsNumber;
      groupData.virtualObjectUuids.add(virtualObject.uid);
      ++groupData.componentsNumber;
      diskData.virtualObjectUuids.add(virtualObject.uid);
      ++diskData.componentsNumber;
   }

   private static VirtualObjectModel cloneVirtualObject(VirtualObjectModel virtualObject, Collection diskObjectUuids) {
      VirtualObjectModel clone = virtualObject.cloneWithoutChildren();
      if (ArrayUtils.isNotEmpty(virtualObject.children)) {
         List children = new ArrayList(virtualObject.children.length);
         VirtualObjectModel[] var4 = virtualObject.children;
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            VirtualObjectModel child = var4[var6];
            if (diskObjectUuids.contains(child.uid)) {
               children.add(child);
            }
         }

         clone.children = (VirtualObjectModel[])children.toArray(new VirtualObjectModel[children.size()]);
      }

      return (!StringUtils.isNotEmpty(clone.uid) || !diskObjectUuids.contains(clone.uid)) && !ArrayUtils.isNotEmpty(clone.children) ? null : clone;
   }

   private static long parseCapacity(String value) {
      try {
         return Long.parseLong(value);
      } catch (Exception var2) {
         logger.warn("Cannot parse capacity to long. Probably the disk is absent: " + value);
         return 0L;
      }
   }

   private static class VirtualObjectModelCollector {
      private Map virtualObjects;

      private VirtualObjectModelCollector() {
         this.virtualObjects = new LinkedHashMap();
      }

      public void addVirtualObject(VirtualObjectModel virtualObject) {
         Integer virtualObjectKey = this.getKey(virtualObject);
         if (this.virtualObjects.containsKey(virtualObjectKey)) {
            VirtualObjectModel vom = (VirtualObjectModel)this.virtualObjects.get(virtualObjectKey);
            vom.mergeChildren(virtualObject);
         } else {
            this.virtualObjects.put(virtualObjectKey, virtualObject.cloneWithChildren());
         }

      }

      public List toList() {
         return new ArrayList(this.virtualObjects.values());
      }

      private int getKey(VirtualObjectModel virtualObject) {
         int result = 7;
         int firstComponent = virtualObject.vmRef == null ? 1 : virtualObject.vmRef.hashCode();
         int result = 31 * result + firstComponent;
         String secondComponent = virtualObject.uid == null ? virtualObject.name : virtualObject.uid;
         return 31 * result + (secondComponent == null ? 1 : secondComponent.hashCode());
      }

      // $FF: synthetic method
      VirtualObjectModelCollector(Object x0) {
         this();
      }
   }

   @TsModel
   public static enum DeviceState {
      OK,
      OFF,
      LOST,
      ERROR,
      UNKNOWN;

      public static PhysicalDisksHierarchicalData.DeviceState fromScsiState(String[] stateKeys) {
         Set states = new HashSet();
         String[] var2 = stateKeys;
         int var3 = stateKeys.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String key = var2[var4];
            states.add(State.valueOf(key));
         }

         if (states.contains(State.ok)) {
            return OK;
         } else if (states.contains(State.off)) {
            return OFF;
         } else if (states.contains(State.lostCommunication)) {
            return LOST;
         } else {
            return states.contains(State.error) ? ERROR : UNKNOWN;
         }
      }
   }
}
