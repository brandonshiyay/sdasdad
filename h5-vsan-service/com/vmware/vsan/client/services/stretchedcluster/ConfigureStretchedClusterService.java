package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ComputeResource;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.diskGroups.data.VsanDiskMapping;
import com.vmware.vsan.client.services.stretchedcluster.model.DomainOrHostData;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterConfig;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterMutationProvider;
import com.vmware.vsphere.client.vsan.stretched.VsanWitnessConfig;
import com.vmware.vsphere.client.vsan.stretched.WitnessHostValidationResult;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigureStretchedClusterService {
   private static final Log logger = LogFactory.getLog(ConfigureStretchedClusterService.class);
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private ObjectReferenceService refService;
   @Autowired
   private VsanStretchedClusterMutationProvider stretchedClusterMutationProvider;
   @Autowired
   private WitnessHostValidationService witnessHostValidationService;
   @Autowired
   private VsanClient vsanClient;

   @TsService("validateNewWitnessHost")
   public WitnessHostValidationResult getNewWitnessHostValidationError(ManagedObjectReference clusterRef, ManagedObjectReference witnessHost) {
      return this.witnessHostValidationService.validateWitnessHost(new ManagedObjectReference[]{clusterRef}, witnessHost, true);
   }

   @TsService("configureStretchedClusterTask")
   public ManagedObjectReference configureStretchedCluster(ManagedObjectReference clusterRef, String preferredName, DomainOrHostData[] preferredDomains, String secondaryName, DomainOrHostData[] secondaryDomains, ManagedObjectReference witnessHost, VsanDiskMapping witnessHostDiskMapping) throws Exception {
      VsanStretchedClusterConfig spec = new VsanStretchedClusterConfig();
      spec.isFaultDomainConfigurationChanged = this.isDomainConfigChanged(preferredName, preferredDomains) || this.isDomainConfigChanged(secondaryName, secondaryDomains);
      spec.preferredSiteName = preferredName;
      spec.preferredSiteHosts = new ArrayList();
      DomainOrHostData[] var9 = preferredDomains;
      int var10 = preferredDomains.length;

      int var11;
      DomainOrHostData domain;
      DomainOrHostData[] var13;
      int var14;
      int var15;
      DomainOrHostData host;
      for(var11 = 0; var11 < var10; ++var11) {
         domain = var9[var11];
         if (domain.isHost) {
            spec.preferredSiteHosts.add((ManagedObjectReference)this.refService.getReference(domain.uid));
         } else {
            var13 = domain.children;
            var14 = var13.length;

            for(var15 = 0; var15 < var14; ++var15) {
               host = var13[var15];
               spec.preferredSiteHosts.add((ManagedObjectReference)this.refService.getReference(host.uid));
            }
         }
      }

      spec.secondarySiteName = secondaryName;
      spec.secondarySiteHosts = new ArrayList();
      var9 = secondaryDomains;
      var10 = secondaryDomains.length;

      for(var11 = 0; var11 < var10; ++var11) {
         domain = var9[var11];
         if (domain.isHost) {
            spec.secondarySiteHosts.add((ManagedObjectReference)this.refService.getReference(domain.uid));
         } else {
            var13 = domain.children;
            var14 = var13.length;

            for(var15 = 0; var15 < var14; ++var15) {
               host = var13[var15];
               spec.secondarySiteHosts.add((ManagedObjectReference)this.refService.getReference(host.uid));
            }
         }
      }

      spec.witnessHost = this.getWitnessHostRef(witnessHost);
      if (witnessHostDiskMapping != null) {
         spec.witnessHostDiskMapping = witnessHostDiskMapping.toVmodl();
      }

      return this.stretchedClusterMutationProvider.configureStretchedCluster(clusterRef, spec);
   }

   @TsService("changeWitnessHostTask")
   public ManagedObjectReference changeWitnessHost(ManagedObjectReference clusterRef, String preferredName, ManagedObjectReference witnessHost, VsanDiskMapping witnessHostDiskMapping) throws Exception {
      VsanWitnessConfig spec = new VsanWitnessConfig();
      spec.host = this.getWitnessHostRef(witnessHost);
      spec.preferredFaultDomain = preferredName;
      if (witnessHostDiskMapping != null) {
         spec.diskMapping = witnessHostDiskMapping.toVmodl();
      }

      return this.stretchedClusterMutationProvider.setWitnessHost(clusterRef, spec);
   }

   private ManagedObjectReference getWitnessHostRef(ManagedObjectReference hostOrComputeResource) throws Exception {
      if (this.vmodlHelper.isOfType(hostOrComputeResource, ComputeResource.class)) {
         hostOrComputeResource = (ManagedObjectReference)QueryUtil.getProperty(hostOrComputeResource, "host", (Object)null);
      }

      return hostOrComputeResource;
   }

   private boolean isDomainConfigChanged(String name, DomainOrHostData[] domainsAndHosts) {
      return domainsAndHosts.length != 1 || domainsAndHosts[0].isHost || !domainsAndHosts[0].uid.equals(name);
   }

   @TsService
   public boolean getStretchClusterSupported(ManagedObjectReference clusterRef) {
      return VsanCapabilityUtils.isStretchedClusterSupportedOnCluster(clusterRef, this.vsanClient);
   }
}
