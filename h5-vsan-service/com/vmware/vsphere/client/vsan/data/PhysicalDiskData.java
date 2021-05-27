package com.vmware.vsphere.client.vsan.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementUtil;
import com.vmware.vsan.client.services.diskmanagement.DiskStatus;
import com.vmware.vsan.client.services.diskmanagement.PmemDiskData;
import com.vmware.vsan.client.services.diskmanagement.StorageCapacity;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsUtil;
import com.vmware.vsan.client.util.PhysicalDiskJsonParser;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class PhysicalDiskData {
   public ManagedObjectReference hostRef;
   public ManagedObjectReference clusterRef;
   public String diskName;
   public String uuid;
   public long capacity;
   public String[] operationalState;
   public Boolean ineligible;
   public String diskIssue;
   public Boolean isSsd;
   public ClaimOption claimOption;
   public String vsanDiskGroupUuid;
   public String[] physicalLocation;
   public String usedCapacity;
   public String reservedCapacity;
   public DiskStatus diskStatus;
   public List virtualDiskUuids = new ArrayList();

   public static PhysicalDiskData fromVsanDisk(VsanDiskData diskData, JsonNode json, ManagedObjectReference diskHostRef, ManagedObjectReference clusterRef) {
      PhysicalDiskData physicalDiskData = fromVsanDiskData(diskData, diskHostRef, clusterRef);
      physicalDiskData.claimOption = diskData.isCacheDisk ? ClaimOption.ClaimForCache : ClaimOption.ClaimForStorage;
      physicalDiskData.vsanDiskGroupUuid = diskData.vsanUuid;
      physicalDiskData.usedCapacity = String.valueOf(PhysicalDiskJsonParser.getUsedCapacity(json, diskData.vsanUuid));
      physicalDiskData.reservedCapacity = String.valueOf(PhysicalDiskJsonParser.getReservedCapacity(json, diskData.vsanUuid));
      physicalDiskData.virtualDiskUuids = VirtualObjectsUtil.getObjectUuidsOnVsan(json, diskData.vsanUuid);
      return physicalDiskData;
   }

   public static PhysicalDiskData fromVsanDirectDisk(VsanDiskData diskData, StorageCapacity diskCapacity, List objectUuids, ManagedObjectReference diskHostRef, ManagedObjectReference clusterRef) {
      PhysicalDiskData physicalDiskData = fromVsanDiskData(diskData, diskHostRef, clusterRef);
      physicalDiskData.claimOption = ClaimOption.VMFS;
      physicalDiskData.usedCapacity = Long.toString(diskCapacity.used);
      return physicalDiskData;
   }

   public static PhysicalDiskData fromPmemStorage(PmemDiskData pmemStorage, ManagedObjectReference diskHostRef, ManagedObjectReference clusterRef) {
      PhysicalDiskData physicalDiskData = new PhysicalDiskData();
      physicalDiskData.hostRef = diskHostRef;
      physicalDiskData.clusterRef = clusterRef;
      physicalDiskData.uuid = pmemStorage.uuid;
      physicalDiskData.diskName = pmemStorage.name;
      physicalDiskData.capacity = pmemStorage.capacity.total;
      physicalDiskData.usedCapacity = Long.toString(pmemStorage.capacity.used);
      physicalDiskData.diskStatus = pmemStorage.diskStatus;
      physicalDiskData.claimOption = ClaimOption.PMEM;
      physicalDiskData.virtualDiskUuids = pmemStorage.objectUuids;
      return physicalDiskData;
   }

   private static PhysicalDiskData fromVsanDiskData(VsanDiskData diskData, ManagedObjectReference diskHostRef, ManagedObjectReference clusterRef) {
      ScsiDisk disk = diskData.disk;
      PhysicalDiskData physicalDiskData = new PhysicalDiskData();
      physicalDiskData.hostRef = diskHostRef;
      physicalDiskData.clusterRef = clusterRef;
      physicalDiskData.capacity = BaseUtils.lbaToBytes(disk.capacity);
      physicalDiskData.operationalState = disk.operationalState;
      physicalDiskData.ineligible = diskData.ineligible;
      if (physicalDiskData.ineligible) {
         if (!StringUtils.isEmpty(diskData.stateReason)) {
            physicalDiskData.diskIssue = diskData.stateReason;
         }
      } else if (!ArrayUtils.isEmpty(diskData.issues)) {
         physicalDiskData.diskIssue = diskData.issues[0];
      }

      physicalDiskData.isSsd = disk.ssd;
      physicalDiskData.diskName = DiskManagementUtil.getDiskName(disk);
      physicalDiskData.uuid = disk.uuid;
      physicalDiskData.physicalLocation = disk.physicalLocation;
      physicalDiskData.diskStatus = diskData.diskStatus;
      return physicalDiskData;
   }
}
