package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.encryption.KeyProviderId;
import com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo;
import com.vmware.vim.binding.vim.vsan.cluster.ConfigInfo.HostDefaultInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainsConfigSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanWitnessSpec;
import com.vmware.vim.vsan.binding.vim.vsan.DataEncryptionConfig;
import com.vmware.vim.vsan.binding.vim.vsan.DataInTransitEncryptionConfig;
import com.vmware.vim.vsan.binding.vim.vsan.RdmaConfig;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.advancedoptions.AdvancedOptionsInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.SpaceEfficiencyConfig;
import com.vmware.vsan.client.services.config.model.ClusterMode;
import com.vmware.vsan.client.services.diskmanagement.claiming.HostDisksClaimer;
import com.vmware.vsan.client.services.health.model.HistoricalHealthConfig;
import com.vmware.vsphere.client.vsan.spec.VsanFaultDomainSpec;
import com.vmware.vsphere.client.vsan.spec.VsanSemiAutoDiskMappingsSpec;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class VsanConfigSpec {
   public boolean isEnabled;
   public ClusterMode clusterMode;
   public SpaceEfficiencyConfig spaceEfficiencyConfig;
   public boolean allowReducedRedundancy;
   public boolean enableDataAtRestEncryption;
   public boolean eraseDisksBeforeUse;
   public String kmipClusterId;
   public VsanStretchedClusterConfig stretchedClusterConfig;
   public boolean autoClaimDisks;
   public boolean enableRdma;
   public boolean enableDataInTransitEncryption;
   public Integer rekeyInterval;
   public boolean hasSharedWitnessHost = false;
   public boolean isServerOrClientCluster = false;
   public AdvancedOptionsInfo advancedOptions;
   public HistoricalHealthConfig historicalHealthConfig;
   @ProxygenSerializer.ElementType(VsanSemiAutoDiskMappingsSpec.class)
   public List diskMappings;
   @ProxygenSerializer.ElementType(VsanFaultDomainSpec.class)
   public List faultDomainSpecs;

   public ReconfigSpec getReconfigSpec(ManagedObjectReference clusterRef, boolean hasEncryptionPermissions, boolean hasRekeyPermission, HostDisksClaimer hostDisksClaimer) {
      ConfigInfo vsanClusterConfig = this.getVsanConfigInfo();
      DataEncryptionConfig encryptionConfig = null;
      if (VsanCapabilityUtils.isDataAtRestEncryptionSupportedOnVc(clusterRef) && hasEncryptionPermissions) {
         encryptionConfig = this.getEncryptionSpec();
      }

      DataInTransitEncryptionConfig dataInTransitEncryptionConfig = null;
      if (VsanCapabilityUtils.isDataInTransitEncryptionSupportedOnVc(clusterRef) && hasRekeyPermission) {
         dataInTransitEncryptionConfig = this.getDataInTransitEncryption();
      }

      ReconfigSpec reconfigSpec = this.getBasicReconfigSpec(hostDisksClaimer);
      reconfigSpec.vsanClusterConfig = vsanClusterConfig;
      reconfigSpec.dataEfficiencyConfig = SpaceEfficiencyConfig.toVmodl(this.spaceEfficiencyConfig, clusterRef);
      reconfigSpec.dataEncryptionConfig = encryptionConfig;
      reconfigSpec.allowReducedRedundancy = this.allowReducedRedundancy;
      reconfigSpec.dataInTransitEncryptionConfig = dataInTransitEncryptionConfig;
      reconfigSpec.extendedConfig = AdvancedOptionsInfo.toVmodl(this.advancedOptions, clusterRef);
      if (VsanCapabilityUtils.isRdmaSupported(clusterRef)) {
         reconfigSpec.rdmaConfig = new RdmaConfig();
         reconfigSpec.rdmaConfig.rdmaEnabled = this.enableRdma;
      }

      return reconfigSpec;
   }

   public ReconfigSpec getComputeOnlyReconfigSpec() {
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.modify = true;
      ConfigInfo vsanClusterConfig = new ConfigInfo();
      vsanClusterConfig.enabled = false;
      reconfigSpec.vsanClusterConfig = vsanClusterConfig;
      reconfigSpec.mode = ClusterMode.COMPUTE.getKey();
      return reconfigSpec;
   }

   public ReconfigSpec getBasicReconfigSpec(HostDisksClaimer hostDisksClaimer) {
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.diskMappingSpec = hostDisksClaimer.toVsanDiskMappingsConfigSpecVmodl(this.autoClaimDisks, this.diskMappings);
      reconfigSpec.faultDomainsSpec = this.getFdSpec();
      reconfigSpec.modify = true;
      return reconfigSpec;
   }

   private ConfigInfo getVsanConfigInfo() {
      ConfigInfo vsanClusterConfig = new ConfigInfo();
      vsanClusterConfig.enabled = true;
      vsanClusterConfig.defaultConfig = new HostDefaultInfo();
      vsanClusterConfig.defaultConfig.autoClaimStorage = this.autoClaimDisks;
      return vsanClusterConfig;
   }

   private DataEncryptionConfig getEncryptionSpec() {
      DataEncryptionConfig encryptionConfig = new DataEncryptionConfig();
      encryptionConfig.encryptionEnabled = this.enableDataAtRestEncryption;
      if (encryptionConfig.encryptionEnabled) {
         if (!StringUtils.isEmpty(this.kmipClusterId)) {
            encryptionConfig.kmsProviderId = new KeyProviderId();
            encryptionConfig.kmsProviderId.setId(this.kmipClusterId);
         }

         encryptionConfig.eraseDisksBeforeUse = this.eraseDisksBeforeUse;
      }

      return encryptionConfig;
   }

   private DataInTransitEncryptionConfig getDataInTransitEncryption() {
      DataInTransitEncryptionConfig dataInTransitEncryptionConfig = new DataInTransitEncryptionConfig();
      dataInTransitEncryptionConfig.enabled = this.enableDataInTransitEncryption;
      if (dataInTransitEncryptionConfig.enabled) {
         dataInTransitEncryptionConfig.rekeyInterval = this.rekeyInterval;
      }

      return dataInTransitEncryptionConfig;
   }

   private VsanFaultDomainsConfigSpec getFdSpec() {
      VsanFaultDomainsConfigSpec fdConfigSpec = null;
      if (this.stretchedClusterConfig != null) {
         fdConfigSpec = new VsanFaultDomainsConfigSpec();
         List fdSpecs = new ArrayList();
         fdSpecs.add(this.createFaultDomainSpec(this.stretchedClusterConfig.preferredSiteName, this.stretchedClusterConfig.preferredSiteHosts));
         fdSpecs.add(this.createFaultDomainSpec(this.stretchedClusterConfig.secondarySiteName, this.stretchedClusterConfig.secondarySiteHosts));
         fdConfigSpec.faultDomains = (com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec[])fdSpecs.toArray(new com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec[fdSpecs.size()]);
         fdConfigSpec.witness = this.getVsanWitnessSpec(this.stretchedClusterConfig);
      }

      if (!CollectionUtils.isEmpty(this.faultDomainSpecs)) {
         fdConfigSpec = new VsanFaultDomainsConfigSpec();
         Map fdToHostsMap = new HashMap();
         List fdSpecs = new ArrayList();

         Iterator var4;
         VsanFaultDomainSpec spec;
         for(var4 = this.faultDomainSpecs.iterator(); var4.hasNext(); ((List)fdToHostsMap.get(spec.faultDomain)).add(spec.hostRef)) {
            spec = (VsanFaultDomainSpec)var4.next();
            if (!fdToHostsMap.containsKey(spec.faultDomain)) {
               fdToHostsMap.put(spec.faultDomain, new ArrayList());
            }
         }

         var4 = fdToHostsMap.keySet().iterator();

         while(var4.hasNext()) {
            String fdName = (String)var4.next();
            List hosts = (List)fdToHostsMap.get(fdName);
            fdSpecs.add(this.createFaultDomainSpec(fdName, hosts));
         }

         fdConfigSpec.faultDomains = (com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec[])fdSpecs.toArray(new com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec[fdSpecs.size()]);
      }

      return fdConfigSpec;
   }

   private VsanWitnessSpec getVsanWitnessSpec(VsanStretchedClusterConfig stretchedClusterConfig) {
      VsanWitnessSpec result = new VsanWitnessSpec();
      result.host = stretchedClusterConfig.witnessHost;
      result.preferredFaultDomainName = stretchedClusterConfig.preferredSiteName;
      result.diskMapping = stretchedClusterConfig.witnessHostDiskMapping;
      return result;
   }

   private com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec createFaultDomainSpec(String fdName, List hosts) {
      com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec faultDomainSpec = new com.vmware.vim.vsan.binding.vim.cluster.VsanFaultDomainSpec();
      faultDomainSpec.name = fdName;
      faultDomainSpec.hosts = (ManagedObjectReference[])hosts.toArray(new ManagedObjectReference[hosts.size()]);
      return faultDomainSpec;
   }

   public String toString() {
      return "VsanConfigSpec(isEnabled=" + this.isEnabled + ", clusterMode=" + this.clusterMode + ", spaceEfficiencyConfig=" + this.spaceEfficiencyConfig + ", allowReducedRedundancy=" + this.allowReducedRedundancy + ", enableDataAtRestEncryption=" + this.enableDataAtRestEncryption + ", eraseDisksBeforeUse=" + this.eraseDisksBeforeUse + ", kmipClusterId=" + this.kmipClusterId + ", stretchedClusterConfig=" + this.stretchedClusterConfig + ", autoClaimDisks=" + this.autoClaimDisks + ", enableRdma=" + this.enableRdma + ", enableDataInTransitEncryption=" + this.enableDataInTransitEncryption + ", rekeyInterval=" + this.rekeyInterval + ", hasSharedWitnessHost=" + this.hasSharedWitnessHost + ", isServerOrClientCluster=" + this.isServerOrClientCluster + ", advancedOptions=" + this.advancedOptions + ", historicalHealthConfig=" + this.historicalHealthConfig + ", diskMappings=" + this.diskMappings + ", faultDomainSpecs=" + this.faultDomainSpecs + ")";
   }
}
