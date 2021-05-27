package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfEntityMetricCSV;
import com.vmware.vim.vsan.binding.vim.cluster.VsanPerfMetricSeriesCSV;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class PerfTopContributorsEntity {
   public InventoryNode entity;
   public List metricsSeries;
   public String entityRefId;
   public ManagedObjectReference parent;
   public boolean isVmOnVsanDatastore;

   public static PerfTopContributorsEntity parsePerfEntityMetricCSV(VsanPerfEntityMetricCSV metric, DataServiceResponse vmProperties, Map mappings, PerfTopContributorsEntityType type, boolean isVmOnVsanDatastore) {
      PerfTopContributorsEntity topContributorsEntity = new PerfTopContributorsEntity();
      topContributorsEntity.entityRefId = metric.entityRefId;
      topContributorsEntity.metricsSeries = new ArrayList();
      topContributorsEntity.isVmOnVsanDatastore = isVmOnVsanDatastore;
      switch(type) {
      case VIRTUAL_MACHINE:
         if (vmProperties == null || vmProperties.getPropertyValues().length <= 0) {
            return null;
         }

         topContributorsEntity.entity = (InventoryNode)vmProperties.getMap().entrySet().stream().filter((entry) -> {
            return filterVms(metric.entityRefId, entry);
         }).map(PerfTopContributorsEntity::createVmInventoryNode).findFirst().orElse((Object)null);
         if (topContributorsEntity.entity == null) {
            return null;
         }
         break;
      case DISK_GROUP:
         updateDiskGroupProperties(topContributorsEntity, mappings);
      }

      if (ArrayUtils.isEmpty(metric.value)) {
         return topContributorsEntity;
      } else {
         VsanPerfMetricSeriesCSV[] var6 = metric.value;
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            VsanPerfMetricSeriesCSV series = var6[var8];
            PerfGraphMetricsData metricsData = new PerfGraphMetricsData();
            metricsData.key = series.metricId.label;
            metricsData.values = new ArrayList();
            metricsData.values.add(Double.parseDouble(series.values));
            topContributorsEntity.metricsSeries.add(metricsData);
         }

         return topContributorsEntity;
      }
   }

   private static void updateDiskGroupProperties(PerfTopContributorsEntity topContributorsEntity, Map mappings) {
      topContributorsEntity.entity = createDiskGroupInventoryNode(topContributorsEntity.entityRefId);
      if (mappings != null) {
         topContributorsEntity.parent = (ManagedObjectReference)mappings.entrySet().stream().filter((hostToDiskMappingEntry) -> {
            return hostContainsDiskGroup(topContributorsEntity.entity.name, (DiskMapInfoEx[])hostToDiskMappingEntry.getValue());
         }).map((hostToDiskMappingEntry) -> {
            return (ManagedObjectReference)hostToDiskMappingEntry.getKey();
         }).findFirst().orElse((Object)null);
      }

   }

   private static InventoryNode createVmInventoryNode(Entry entry) {
      return new InventoryNode((ManagedObjectReference)entry.getKey(), (String)((Map)entry.getValue()).get("name"), (String)((Map)entry.getValue()).get("primaryIconId"));
   }

   private static InventoryNode createDiskGroupInventoryNode(String entityRefId) {
      InventoryNode diskGroup = new InventoryNode();
      diskGroup.name = QueryUtil.extractNodeUuid(entityRefId);
      diskGroup.primaryIconId = "disk-group-icon";
      return diskGroup;
   }

   private static boolean hostContainsDiskGroup(String diskGroupUuid, DiskMapInfoEx[] mappings) {
      return ArrayUtils.isEmpty(mappings) ? false : Arrays.stream(mappings).anyMatch((diskMappings) -> {
         return diskMappings.mapping.ssd.vsanDiskInfo.vsanUuid.equals(diskGroupUuid);
      });
   }

   private static boolean filterVms(String entityRefId, Entry entry) {
      return entityRefId.indexOf((String)((Map)entry.getValue()).get("config.instanceUuid")) > -1;
   }
}
