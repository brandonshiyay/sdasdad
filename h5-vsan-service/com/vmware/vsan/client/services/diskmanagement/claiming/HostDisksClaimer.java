package com.vmware.vsan.client.services.diskmanagement.claiming;

import com.google.common.collect.ImmutableList;
import com.vmware.vim.binding.vim.vsan.host.DiskMapping;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanDiskMappingsConfigSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanHostDiskMapping;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMappingCreationSpec;
import com.vmware.vsphere.client.vsan.spec.VsanSemiAutoDiskMappingsSpec;
import com.vmware.vsphere.client.vsan.spec.VsanSemiAutoDiskSpec;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
public class HostDisksClaimer {
   private VsanDatastoreClaimer vsanDatastoreClaimer = new VsanDatastoreClaimer();
   private List datastoreClaimers = ImmutableList.of(new VsanDatastoreClaimer(), new VmfsDatastoreClaimer(), new PmemDatastoreClaimer());

   public VsanDiskMappingsConfigSpec toVsanDiskMappingsConfigSpecVmodl(boolean autoClaimDisks, List hostsDiskMappings) {
      return !autoClaimDisks && !CollectionUtils.isEmpty(hostsDiskMappings) ? this.claimForEachDatastore(hostsDiskMappings).toVsanDiskMappingsConfigSpecVmodl() : null;
   }

   public List toDiskMappingCreationSpecVmodls(VsanSemiAutoDiskMappingsSpec hostDiskMappings) {
      return hostDiskMappings == null ? null : this.claimForEachDatastore(hostDiskMappings).toDiskMappingCreationSpecVmodls();
   }

   public DiskMappingCreationSpec toDiskMappingCreationSpecVmodl(ManagedObjectReference hostRef, DiskMapping diskMapping) {
      VsanHostDiskMapping vsanDiskMapping = this.vsanDatastoreClaimer.toVsanHostDiskMappingVmodl(hostRef, diskMapping);
      ClaimedDisksPerDatastore claimedDisksPerDatastore = new ClaimedDisksPerDatastore();
      claimedDisksPerDatastore.vsanDatastoreDiskMappings.add(vsanDiskMapping);
      return (DiskMappingCreationSpec)claimedDisksPerDatastore.toDiskMappingCreationSpecVmodls().stream().findFirst().orElse((Object)null);
   }

   private ClaimedDisksPerDatastore claimForEachDatastore(VsanSemiAutoDiskMappingsSpec hostDiskMapping) {
      return this.claimForEachDatastore(Collections.singletonList(hostDiskMapping));
   }

   private ClaimedDisksPerDatastore claimForEachDatastore(List hostsDiskMappings) {
      ClaimedDisksPerDatastore claimedDisksPerDatastore = new ClaimedDisksPerDatastore();
      hostsDiskMappings.forEach((hostDiskMapping) -> {
         this.claimHostDisks(hostDiskMapping, claimedDisksPerDatastore);
      });
      return claimedDisksPerDatastore;
   }

   private void claimHostDisks(VsanSemiAutoDiskMappingsSpec diskMappingSpec, ClaimedDisksPerDatastore claimedDisksPerDatastore) {
      Map claimOptionToDisks = this.groupDisksByClaimOption(diskMappingSpec.disks);
      this.datastoreClaimers.stream().filter((claimer) -> {
         return claimer.canClaim(claimOptionToDisks);
      }).forEach((claimer) -> {
         claimer.claim(diskMappingSpec.hostRef, claimOptionToDisks, claimedDisksPerDatastore);
      });
   }

   private Map groupDisksByClaimOption(VsanSemiAutoDiskSpec[] disks) {
      return (Map)Stream.of(disks).collect(Collectors.groupingBy(VsanSemiAutoDiskSpec::getClaimOption));
   }
}
