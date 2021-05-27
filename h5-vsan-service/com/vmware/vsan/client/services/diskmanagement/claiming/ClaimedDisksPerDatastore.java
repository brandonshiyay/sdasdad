package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.vmware.vim.vsan.binding.vim.cluster.VsanDiskMappingsConfigSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping.VsanDiskGroupCreationType;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMappingCreationSpec;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMappingCreationSpec.DiskMappingCreationType;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimedDisksPerDatastore {
   public List vsanDatastoreDiskMappings = new ArrayList();
   public List vmfsDatastoreDiskMappings = new ArrayList();
   public List pmemDatastoreDiskMappings = new ArrayList();

   public VsanDiskMappingsConfigSpec toVsanDiskMappingsConfigSpecVmodl() {
      VsanDiskMappingsConfigSpec spec = new VsanDiskMappingsConfigSpec();
      spec.hostDiskMappings = this.getAllVsanHostDiskMappings();
      return spec;
   }

   public List toDiskMappingCreationSpecVmodls() {
      return (List)Arrays.stream(this.getAllVsanHostDiskMappings()).map(this::toDiskMappingCreationSpecVmodl).collect(Collectors.toList());
   }

   private DiskMappingCreationSpec toDiskMappingCreationSpecVmodl(VsanHostDiskMapping diskMapping) {
      DiskMappingCreationSpec diskMappingCreationSpec = new DiskMappingCreationSpec();
      diskMappingCreationSpec.host = diskMapping.host;
      diskMappingCreationSpec.cacheDisks = diskMapping.cacheDisks;
      diskMappingCreationSpec.capacityDisks = diskMapping.capacityDisks;
      diskMappingCreationSpec.creationType = this.getDiskMappingCreationType(diskMapping.type).toString();
      return diskMappingCreationSpec;
   }

   private VsanHostDiskMapping[] getAllVsanHostDiskMappings() {
      List diskMappings = new ArrayList(this.vsanDatastoreDiskMappings);
      diskMappings.addAll(this.vmfsDatastoreDiskMappings);
      diskMappings.addAll(this.pmemDatastoreDiskMappings);
      return (VsanHostDiskMapping[])diskMappings.toArray(new VsanHostDiskMapping[0]);
   }

   private DiskMappingCreationType getDiskMappingCreationType(String vsanDiskGroupCreationType) {
      VsanDiskGroupCreationType type = (VsanDiskGroupCreationType)EnumUtils.fromString(VsanDiskGroupCreationType.class, vsanDiskGroupCreationType);
      switch(type) {
      case allflash:
         return DiskMappingCreationType.allFlash;
      case hybrid:
         return DiskMappingCreationType.hybrid;
      case vsandirect:
         return DiskMappingCreationType.vsandirect;
      case pmem:
         return DiskMappingCreationType.pmem;
      default:
         return DiskMappingCreationType.DiskMappingCreationType_Unknown;
      }
   }
}
