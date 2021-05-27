package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityInfo;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class EntityRefData {
   public String entityRefId;
   public String metricName;
   public PerformanceObjectType performanceObjectType;
   public String objectName;
   public ManagedObjectReference managedObjectRef;
   public String managedObjectName;
   public String vsanUuid;
   public boolean isEntityMissing = false;

   public EntityRefData() {
   }

   public EntityRefData(VsanPerfEntityInfo entityInfo, ManagedObjectReference clusterRef) {
      this.entityRefId = entityInfo.entityRefId;
      this.objectName = entityInfo.entityName;
      this.performanceObjectType = this.getPerformanceObjectType(entityInfo.entityRefType);
      if (StringUtils.isEmpty(entityInfo.entityRelatedMoRef)) {
         if (this.isClusterData(this.performanceObjectType)) {
            this.managedObjectRef = clusterRef;
         } else {
            this.isEntityMissing = true;
         }
      } else {
         this.managedObjectRef = BaseUtils.generateMor(entityInfo.entityRelatedMoRef, clusterRef.getServerGuid());
      }

      if (!StringUtils.isEmpty(this.entityRefId)) {
         String[] parts = this.entityRefId.split(":");
         if (parts != null && parts.length >= 2) {
            this.metricName = parts[0];
            this.vsanUuid = parts[1];
         }
      }
   }

   private PerformanceObjectType getPerformanceObjectType(String entityRefType) {
      PerformanceObjectType result = null;
      byte var4 = -1;
      switch(entityRefType.hashCode()) {
      case -1917420641:
         if (entityRefType.equals("cluster-ioinsight")) {
            var4 = 18;
         }
         break;
      case -1775894832:
         if (entityRefType.equals("capacity-disk")) {
            var4 = 1;
         }
         break;
      case -1229779238:
         if (entityRefType.equals("cluster-domcompmgr")) {
            var4 = 13;
         }
         break;
      case -1114158933:
         if (entityRefType.equals("vsan-pnic-net")) {
            var4 = 7;
         }
         break;
      case -982244677:
         if (entityRefType.equals("vsan-host-net")) {
            var4 = 8;
         }
         break;
      case -712982753:
         if (entityRefType.equals("virtual-disk")) {
            var4 = 4;
         }
         break;
      case -624594652:
         if (entityRefType.equals("cluster-domowner")) {
            var4 = 14;
         }
         break;
      case -17571576:
         if (entityRefType.equals("cache-disk")) {
            var4 = 2;
         }
         break;
      case 94783762:
         if (entityRefType.equals("cmmds")) {
            var4 = 15;
         }
         break;
      case 112500252:
         if (entityRefType.equals("vscsi")) {
            var4 = 3;
         }
         break;
      case 594137398:
         if (entityRefType.equals("host-domowner")) {
            var4 = 10;
         }
         break;
      case 752768485:
         if (entityRefType.equals("vsan-vnic-net")) {
            var4 = 6;
         }
         break;
      case 884532648:
         if (entityRefType.equals("host-domclient")) {
            var4 = 9;
         }
         break;
      case 1032473077:
         if (entityRefType.equals("clom-disk-stats")) {
            var4 = 16;
         }
         break;
      case 1439608399:
         if (entityRefType.equals("disk-group")) {
            var4 = 0;
         }
         break;
      case 1503534541:
         if (entityRefType.equals("host-ioinsight")) {
            var4 = 19;
         }
         break;
      case 1592855429:
         if (entityRefType.equals("virtual-machine")) {
            var4 = 5;
         }
         break;
      case 1684192704:
         if (entityRefType.equals("clom-host-stats")) {
            var4 = 17;
         }
         break;
      case 1740616300:
         if (entityRefType.equals("host-domcompmgr")) {
            var4 = 11;
         }
         break;
      case 1758544762:
         if (entityRefType.equals("cluster-domclient")) {
            var4 = 12;
         }
      }

      switch(var4) {
      case 0:
         result = PerformanceObjectType.diskGroup;
         break;
      case 1:
         result = PerformanceObjectType.capacityDisk;
         break;
      case 2:
         result = PerformanceObjectType.cacheDisk;
         break;
      case 3:
         result = PerformanceObjectType.vscsi;
         break;
      case 4:
         result = PerformanceObjectType.virtualDisk;
         break;
      case 5:
         result = PerformanceObjectType.vm;
         break;
      case 6:
         result = PerformanceObjectType.hostVnic;
         break;
      case 7:
         result = PerformanceObjectType.hostPnic;
         break;
      case 8:
         result = PerformanceObjectType.hostNet;
         break;
      case 9:
         result = PerformanceObjectType.hostVmConsumption;
         break;
      case 10:
      case 11:
         result = PerformanceObjectType.hostBackend;
         break;
      case 12:
         result = PerformanceObjectType.clusterVmConsumption;
         break;
      case 13:
         result = PerformanceObjectType.clusterBackend;
         break;
      case 14:
         result = PerformanceObjectType.clusterDomOwner;
         break;
      case 15:
         result = PerformanceObjectType.cmmds;
         break;
      case 16:
         result = PerformanceObjectType.clomDiskStats;
         break;
      case 17:
         result = PerformanceObjectType.clomHostStats;
         break;
      case 18:
         result = PerformanceObjectType.clusterIoInsight;
         break;
      case 19:
         result = PerformanceObjectType.hostIoInsight;
      }

      return result;
   }

   private boolean isClusterData(PerformanceObjectType objectType) {
      if (objectType == null) {
         return false;
      } else {
         return objectType.equals(PerformanceObjectType.clusterBackend) || objectType.equals(PerformanceObjectType.clusterDomOwner) || objectType.equals(PerformanceObjectType.clusterVmConsumption) || objectType.equals(PerformanceObjectType.clusterIoInsight);
      }
   }
}
