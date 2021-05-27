package com.vmware.vsan.client.services.hci;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.EVCMode;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.NumericRange;
import com.vmware.vim.binding.vim.SDDCBase;
import com.vmware.vim.binding.vim.ClusterComputeResource.DVSSetting;
import com.vmware.vim.binding.vim.ClusterComputeResource.DvsProfile;
import com.vmware.vim.binding.vim.ClusterComputeResource.HCIConfigInfo;
import com.vmware.vim.binding.vim.ClusterComputeResource.HCIConfigSpec;
import com.vmware.vim.binding.vim.ClusterComputeResource.HostConfigurationInput;
import com.vmware.vim.binding.vim.ClusterComputeResource.ValidationResultBase;
import com.vmware.vim.binding.vim.ClusterComputeResource.DVSSetting.DVPortgroupToServiceMapping;
import com.vmware.vim.binding.vim.cluster.EVCManager;
import com.vmware.vim.binding.vim.dvs.DistributedVirtualPortgroup;
import com.vmware.vim.binding.vim.dvs.HostMember;
import com.vmware.vim.binding.vim.dvs.VmwareDistributedVirtualSwitch;
import com.vmware.vim.binding.vim.dvs.VmwareDistributedVirtualSwitch.PvlanSpec;
import com.vmware.vim.binding.vim.dvs.VmwareDistributedVirtualSwitch.TrunkVlanSpec;
import com.vmware.vim.binding.vim.dvs.VmwareDistributedVirtualSwitch.VlanIdSpec;
import com.vmware.vim.binding.vim.dvs.VmwareDistributedVirtualSwitch.VlanSpec;
import com.vmware.vim.binding.vim.fault.InvalidState;
import com.vmware.vim.binding.vim.host.PhysicalNic;
import com.vmware.vim.binding.vim.host.CpuPackage.Vendor;
import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vmodl.LocalizableMessage;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vise.data.query.PropertyConstraint;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.CeipService;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.services.common.TaskService;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.config.model.ClusterMode;
import com.vmware.vsan.client.services.config.model.VsanClusterType;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.services.diskmanagement.claiming.HostDisksClaimer;
import com.vmware.vsan.client.services.encryption.EncryptionPropertyProvider;
import com.vmware.vsan.client.services.hci.model.BasicClusterConfigData;
import com.vmware.vsan.client.services.hci.model.ClusterConfigData;
import com.vmware.vsan.client.services.hci.model.ConfigureWizardData;
import com.vmware.vsan.client.services.hci.model.DrsAutoLevel;
import com.vmware.vsan.client.services.hci.model.DvsData;
import com.vmware.vsan.client.services.hci.model.EvcModeConfigData;
import com.vmware.vsan.client.services.hci.model.EvcModeData;
import com.vmware.vsan.client.services.hci.model.ExistingDvpgData;
import com.vmware.vsan.client.services.hci.model.ExistingDvsData;
import com.vmware.vsan.client.services.hci.model.HciWorkflowState;
import com.vmware.vsan.client.services.hci.model.HostAdapter;
import com.vmware.vsan.client.services.hci.model.HostInCluster;
import com.vmware.vsan.client.services.hci.model.Service;
import com.vmware.vsan.client.services.hci.model.VlanData;
import com.vmware.vsan.client.services.hci.model.VlanType;
import com.vmware.vsan.client.services.vum.VumBaselineRecommendationService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.data.VsanConfigSpec;
import com.vmware.vsphere.client.vsan.health.VsanTestData;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HciClusterService {
   private static final String VSAN_HIDE_CEIP_PAGE_IN_HCI_WOFKFLOW = "config.vsan.hide_ceip_page_in_hci_wofkflow";
   private static final String HOST_NICS_PROPERTY = "config.network.pnic";
   private static final String HOST_IS_WITNESS_PROPERTY = "isWitnessHost";
   private static final String IS_METADATA_WITNESS_HOST_PROPERTY = "isMetadataWitnessHost";
   private static final String SUPPORTED_EVC_MODE_PROPERTY = "supportedEvcMode";
   private static final String HOST_MAX_EVC_MODE_KEY_PROPERTY = "summary.maxEVCModeKey";
   private static final String HCI_CONFIG_PROPERTY = "hciConfig";
   private static final String HA_PROPERTY = "configurationEx.dasConfig.enabled";
   private static final String HA_FAILOVER_LEVEL_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.failoverLevel";
   private static final String HA_HOST_MONITORING_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.hostMonitoring";
   private static final String HA_ADMISSION_CONTROL_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.admissionControlEnabled";
   private static final String HA_VM_MONITORING_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.vmMonitoring";
   private static final String DRS_PROPERTY = "configurationEx.drsConfig.enabled";
   private static final String DRS_AUTOMATION_LEVEL_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].drsConfig.defaultVmBehavior";
   private static final String DRS_MIGRATION_THRESHOLD_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].drsConfig.vmotionRate";
   private static final String VSAN_PROPERTY = "configurationEx.vsanConfigInfo.enabled";
   private static final String DVPG_VLAN_PROPERTY = "config.defaultPortConfig.vlan";
   private static final String VERSION_PROPERTY = "config.productInfo.version";
   private static final String DVS_HOST_PROPERTY = "config.host";
   private static final String NIOC_VERSION_PROPERTY = "lacpVersionColumnLabelDerived";
   private static final String LACP_VERSION_PROPERTY = "niocVersionColumnLabel";
   private static final String DVS_PORTGROUP_RELATION = "portgroup";
   private static final String DVPG_UPLINK_PROPERTY = "config.uplink";
   private static final String PMAN_PROPERTY = "lifecycleManaged";
   private static final int LARGE_SCALE_CLUSTER_SUPPORT_THRESHOLD = 32;
   private static final String[] BASIC_CLUSTER_CONFIG_PROPERTIES = new String[]{"host._length", "hciConfig", "configurationEx.drsConfig.enabled", "configurationEx.dasConfig.enabled", "lifecycleManaged", "configurationEx.vsanConfigInfo.enabled"};
   private static final String[] CLUSTER_CONFIG_PROPERTIES = new String[]{"configurationEx[@type='ClusterConfigInfoEx'].dasConfig.admissionControlEnabled", "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.failoverLevel", "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.hostMonitoring", "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.vmMonitoring", "configurationEx[@type='ClusterConfigInfoEx'].drsConfig.defaultVmBehavior", "configurationEx[@type='ClusterConfigInfoEx'].drsConfig.vmotionRate"};
   private static final String[] EXISTING_DVS_PROPERTIES = new String[]{"name", "config.productInfo.version", "lacpVersionColumnLabelDerived", "niocVersionColumnLabel", "config.host"};
   private static final String OBJECT_NAME_SEPARATOR = " ";
   private static final String GENERAL_ENABLED = "enabled";
   private static final String VM_MONITORING_DISABLED = "vmMonitoringDisabled";
   private static final String DEFAULT_VLAN = "0";
   private static final int MAX_DVS = 3;
   private static final Log logger = LogFactory.getLog(HciClusterService.class);
   private static final VsanProfiler _profiler = new VsanProfiler(HciClusterService.class);
   @Autowired
   private VcClient vcClient;
   @Autowired
   private ObjectReferenceService refService;
   @Autowired
   private TaskService taskService;
   @Autowired
   private PermissionService permissionService;
   @Autowired
   private EncryptionPropertyProvider encryptionPropertyProvider;
   @Autowired
   private CeipService ceipService;
   @Autowired
   private VumBaselineRecommendationService baselineRecommendationService;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private HostDisksClaimer hostDisksClaimer;
   @Autowired
   private VsanConfigService vsanConfigService;
   @Autowired
   private CsdService csdService;

   @TsService
   public ConfigureWizardData getConfigureWizardData(ManagedObjectReference clusterRef) {
      BasicClusterConfigData basicClusterConfigData = this.getBasicClusterConfigData(clusterRef);
      return basicClusterConfigData.hciWorkflowState == HciWorkflowState.IN_PROGRESS ? this.getHciConfigureData(basicClusterConfigData, clusterRef) : this.getHciExtendData(basicClusterConfigData, clusterRef);
   }

   private ConfigureWizardData getHciConfigureData(BasicClusterConfigData basicClusterConfigData, ManagedObjectReference clusterRef) {
      ConfigureWizardData wizardData = new ConfigureWizardData();
      wizardData.isExtend = false;
      wizardData.openYesNoDialog = false;
      wizardData.openWarningDialog = false;
      boolean hasNetPermissions = this.hasHostConfigurePermissions(clusterRef);
      wizardData.optOutOfNetworking = !hasNetPermissions;
      wizardData.optOutOfNetworkingDisabled = !hasNetPermissions;
      wizardData.enableFaultDomainForSingleSiteCluster = false;
      wizardData.showDvsPage = true;
      wizardData.showVmotionTrafficPage = basicClusterConfigData.drsEnabled;
      wizardData.showVsanTrafficPage = basicClusterConfigData.vsanEnabled;
      wizardData.showAdvancedOptionsPage = true;
      wizardData.showClaimDisksPage = basicClusterConfigData.vsanEnabled;
      wizardData.selectedVsanClusterType = basicClusterConfigData.vsanEnabled ? VsanClusterType.SINGLE_SITE_CLUSTER : VsanClusterType.NO_VSAN;
      wizardData.showFaultDomainsPageComponent = false;
      wizardData.showSingleSiteFaultDomainsPage = false;
      wizardData.showWitnessHostPageComponent = false;
      wizardData.showClaimDisksWitnessHostPage = false;
      wizardData.isSupportInsightStepHidden = this.isHideSupportInsightStepConfigured(clusterRef);
      wizardData.ceipEnabled = this.ceipService.getCeipServiceEnabled(clusterRef);
      if (basicClusterConfigData.hosts > 32) {
         wizardData.largeScaleClusterSupport = true;
      } else {
         wizardData.largeScaleClusterSupport = this.getLargeScaleClusterSupport(clusterRef);
      }

      return wizardData;
   }

   private ConfigureWizardData getHciExtendData(BasicClusterConfigData basicClusterConfigData, ManagedObjectReference clusterRef) {
      ConfigureWizardData wizardData = new ConfigureWizardData();
      wizardData.isExtend = true;
      wizardData.optOutOfNetworking = true;
      HCIConfigInfo hciConfig = null;

      try {
         hciConfig = (HCIConfigInfo)QueryUtil.getProperty(clusterRef, "hciConfig");
      } catch (Exception var6) {
         logger.error("Unable to extract HCI config for cluster: " + clusterRef);
         throw new VsanUiLocalizableException("vsan.hci.createWorkflow.extend.hciConfig.error", var6);
      }

      this.processDvsSettings(wizardData, hciConfig);
      if (!this.validateNetworks(wizardData, clusterRef, hciConfig, basicClusterConfigData.vsanEnabled)) {
         return wizardData;
      } else {
         wizardData.showDvsPage = false;
         wizardData.showAdvancedOptionsPage = false;
         wizardData.showClaimDisksPage = basicClusterConfigData.vsanEnabled;
         if (basicClusterConfigData.vsanEnabled) {
            boolean isStretchedCluster = this.isStretchedCluster(clusterRef);
            if (isStretchedCluster) {
               wizardData.selectedVsanClusterType = VsanClusterType.STRETCHED_CLUSTER;
               wizardData.showFaultDomainsPageComponent = true;
            } else {
               wizardData.selectedVsanClusterType = VsanClusterType.SINGLE_SITE_CLUSTER;
               wizardData.showSingleSiteFaultDomainsPage = this.isFaultDomainCreationAllowed(clusterRef);
            }
         } else {
            wizardData.selectedVsanClusterType = VsanClusterType.NO_VSAN;
            wizardData.showFaultDomainsPageComponent = false;
            wizardData.showSingleSiteFaultDomainsPage = false;
            if (wizardData.optOutOfNetworking) {
               wizardData.openYesNoDialog = true;
               wizardData.dialogText = Utils.getLocalizedString("vsan.hci.dialog.configureHostsConfirmation.title");
            }
         }

         wizardData.showWitnessHostPageComponent = false;
         wizardData.showClaimDisksWitnessHostPage = false;
         return wizardData;
      }
   }

   private boolean isFaultDomainCreationAllowed(ManagedObjectReference clusterRef) {
      VsanConfigSpec vsanConfigSpec = this.vsanConfigService.getVsanConfigSpec(clusterRef);
      return vsanConfigSpec == null || vsanConfigSpec.advancedOptions == null || vsanConfigSpec.advancedOptions.capacityReservationConfig == null || !vsanConfigSpec.advancedOptions.capacityReservationConfig.vsanOperationReservation.isEnforced();
   }

   @TsService
   public ClusterConfigData getClusterConfigData(ManagedObjectReference clusterRef) {
      String[] properties = (String[])ArrayUtils.addAll(BASIC_CLUSTER_CONFIG_PROPERTIES, CLUSTER_CONFIG_PROPERTIES);
      DataServiceResponse response = null;

      try {
         response = QueryUtil.getProperties(clusterRef, properties);
      } catch (Exception var9) {
         throw new VsanUiLocalizableException("vsan.common.cluster.configuration.error", var9);
      }

      ClusterConfigData configData = new ClusterConfigData();
      configData.vsanConfigSpec = this.vsanConfigService.getVsanConfigSpec(clusterRef);
      boolean isComputeOnlyCluster = configData.vsanConfigSpec != null && ClusterMode.COMPUTE.equals(configData.vsanConfigSpec.clusterMode);

      try {
         configData.basicConfig = this.populateBasicClusterConfigData(response, isComputeOnlyCluster);
      } catch (Exception var8) {
         throw new VsanUiLocalizableException("vsan.hci.basicClusterData.error", var8);
      }

      configData.enableAdmissionControl = (Boolean)response.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.admissionControlEnabled");
      configData.hostFTT = (Integer)response.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.failoverLevel");
      String vmMonitorStr = (String)response.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.vmMonitoring");
      configData.enableVmMonitoring = !"vmMonitoringDisabled".equals(vmMonitorStr);
      String hostMonitorStr = (String)response.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].dasConfig.hostMonitoring");
      configData.enableHostMonitoring = "enabled".equals(hostMonitorStr);
      configData.automationLevel = DrsAutoLevel.fromString(response.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].drsConfig.defaultVmBehavior").toString());
      configData.migrationThreshold = (Integer)response.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].drsConfig.vmotionRate");
      if (configData.basicConfig.vsanEnabled && VsanCapabilityUtils.isVumBaselineRecommendationSupportedOnVc(clusterRef)) {
         configData.vumBaselineRecommendationType = this.baselineRecommendationService.getClusterVumBaselineRecommendation(clusterRef);
      }

      return configData;
   }

   public BasicClusterConfigData getBasicClusterConfigData(ManagedObjectReference clusterRef) {
      try {
         DataServiceResponse response = QueryUtil.getProperties(clusterRef, BASIC_CLUSTER_CONFIG_PROPERTIES);
         boolean isComputeOnlyCluster = this.csdService.isComputeOnlyCluster(clusterRef);
         return this.populateBasicClusterConfigData(response, isComputeOnlyCluster);
      } catch (Exception var4) {
         throw new VsanUiLocalizableException("vsan.hci.basicClusterData.error", var4);
      }
   }

   private BasicClusterConfigData populateBasicClusterConfigData(DataServiceResponse response, boolean isComputeOnlyCluster) throws Exception {
      BasicClusterConfigData basicConfig = new BasicClusterConfigData();
      basicConfig.isComputeOnlyCluster = isComputeOnlyCluster;
      if (response != null && response.getPropertyValues() != null) {
         PropertyValue[] propertyValues = response.getPropertyValues();
         HCIConfigInfo hciConfigInfo = null;
         PropertyValue[] var6 = propertyValues;
         int var7 = propertyValues.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            PropertyValue propValue = var6[var8];
            if (propValue.propertyName.equals("host._length")) {
               basicConfig.hosts = (Integer)propValue.value;
            } else if (propValue.propertyName.equals("configurationEx.drsConfig.enabled")) {
               basicConfig.drsEnabled = (Boolean)propValue.value;
            } else if (propValue.propertyName.equals("configurationEx.dasConfig.enabled")) {
               basicConfig.haEnabled = (Boolean)propValue.value;
            } else if (propValue.propertyName.equals("configurationEx.vsanConfigInfo.enabled")) {
               basicConfig.vsanEnabled = (Boolean)propValue.value;
            } else if (propValue.propertyName.equals("lifecycleManaged")) {
               basicConfig.pmanEnabled = BooleanUtils.isTrue((Boolean)propValue.value);
            } else if (propValue.propertyName.equals("hciConfig")) {
               hciConfigInfo = (HCIConfigInfo)propValue.value;
            }
         }

         if (hciConfigInfo != null) {
            basicConfig.notConfiguredHosts = this.getNotConfiguredHostsCount(basicConfig.hosts, hciConfigInfo);
            basicConfig.hciWorkflowState = HciWorkflowState.fromString(hciConfigInfo.workflowState);
            if (basicConfig.hciWorkflowState == HciWorkflowState.DONE && basicConfig.notConfiguredHosts > 0) {
               basicConfig.dvsDataByService = this.getDvsInfoData(hciConfigInfo);
            }
         } else {
            basicConfig.notConfiguredHosts = 0;
            basicConfig.hciWorkflowState = HciWorkflowState.NOT_IN_HCI_WORKFLOW;
         }

         return basicConfig;
      } else {
         return basicConfig;
      }
   }

   private int getNotConfiguredHostsCount(int hosts, HCIConfigInfo hciConfigInfo) {
      ManagedObjectReference[] configuredHosts = hciConfigInfo.configuredHosts;
      return configuredHosts == null ? hosts : hosts - configuredHosts.length;
   }

   @TsService
   public List getClusterHosts(ManagedObjectReference clusterRef) throws Exception {
      List result = new ArrayList();
      PropertyValue[] hostNameValues = QueryUtil.getPropertyForRelatedObjects(clusterRef, "host", ClusterComputeResource.class.getSimpleName(), "name").getPropertyValues();
      PropertyValue[] var4 = hostNameValues;
      int var5 = hostNameValues.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         PropertyValue nameValue = var4[var6];
         result.add(HostInCluster.create((ManagedObjectReference)nameValue.resourceObject, this.refService.getUid(nameValue.resourceObject), (String)nameValue.value));
      }

      return result;
   }

   @TsService
   public List getNotConfiguredClusterHosts(ManagedObjectReference clusterRef) throws Exception {
      List result = new ArrayList();
      ManagedObjectReference[] notConfiguredHosts = this.getNotConfiguredHosts(clusterRef);
      PropertyValue[] hostNameValues = QueryUtil.getProperties(notConfiguredHosts, new String[]{"name"}).getPropertyValues();
      PropertyValue[] var5 = hostNameValues;
      int var6 = hostNameValues.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         PropertyValue nameValue = var5[var7];
         result.add(HostInCluster.create((ManagedObjectReference)nameValue.resourceObject, this.refService.getUid(nameValue.resourceObject), (String)nameValue.value));
      }

      Collections.sort(result, new Comparator() {
         public int compare(HostInCluster host1, HostInCluster host2) {
            if (host1 != null && !StringUtils.isEmpty(host1.name) || host2 != null && !StringUtils.isEmpty(host2.name)) {
               if (host1 != null && !StringUtils.isEmpty(host1.name)) {
                  return host2 != null && !StringUtils.isEmpty(host2.name) ? host1.name.compareTo(host2.name) : -1;
               } else {
                  return 1;
               }
            } else {
               return 0;
            }
         }
      });
      return result;
   }

   private ManagedObjectReference[] getNotConfiguredHosts(ManagedObjectReference clusterRef) throws Exception {
      DataServiceResponse response = QueryUtil.getProperties(clusterRef, new String[]{"host", "hciConfig"});
      ManagedObjectReference[] hosts = (ManagedObjectReference[])response.getProperty(clusterRef, "host");
      HCIConfigInfo hciConfigInfo = (HCIConfigInfo)response.getProperty(clusterRef, "hciConfig");
      ManagedObjectReference[] configuredHosts = hciConfigInfo == null ? null : hciConfigInfo.configuredHosts;
      ManagedObjectReference[] notConfiguredHosts = new ManagedObjectReference[0];
      if (hosts != null) {
         if (configuredHosts == null) {
            notConfiguredHosts = hosts;
         } else if (hosts.length == configuredHosts.length) {
            notConfiguredHosts = new ManagedObjectReference[0];
         } else {
            List configuredHostsIds = new ArrayList();
            ManagedObjectReference[] var8 = configuredHosts;
            int var9 = configuredHosts.length;

            int var10;
            for(var10 = 0; var10 < var9; ++var10) {
               ManagedObjectReference host = var8[var10];
               configuredHostsIds.add(host.getValue());
            }

            List hostsList = new ArrayList();
            ManagedObjectReference[] var14 = hosts;
            var10 = hosts.length;

            for(int var15 = 0; var15 < var10; ++var15) {
               ManagedObjectReference host = var14[var15];
               if (!configuredHostsIds.contains(host.getValue())) {
                  hostsList.add(host);
               }
            }

            notConfiguredHosts = (ManagedObjectReference[])hostsList.toArray(new ManagedObjectReference[0]);
         }
      }

      return notConfiguredHosts;
   }

   @TsService
   public EvcModeConfigData getEvcModeConfigData(ManagedObjectReference clusterRef) throws Exception {
      EvcModeConfigData data = new EvcModeConfigData();
      PropertyValue[] hostProps = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"summary.maxEVCModeKey"}).getPropertyValues();
      EVCMode[] evcModes = (EVCMode[])QueryUtil.getProperty(clusterRef, "supportedEvcMode", (Object)null);
      if (!ArrayUtils.isEmpty(evcModes) && !ArrayUtils.isEmpty(hostProps)) {
         List supportedAmdEvcMode = new ArrayList();
         List supportedIntelEvcMode = new ArrayList();
         EVCMode[] var7 = evcModes;
         int var8 = evcModes.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            EVCMode evcMode = var7[var9];
            EvcModeData modeData = new EvcModeData();
            modeData.id = evcMode.key;
            modeData.label = evcMode.label;
            if (Vendor.amd.name().equals(evcMode.vendor)) {
               supportedAmdEvcMode.add(modeData);
            } else if (Vendor.intel.name().equals(evcMode.vendor)) {
               supportedIntelEvcMode.add(modeData);
            } else {
               logger.warn("Unsupported vendor: " + evcMode.vendor);
            }
         }

         List intelSupportedIndex = new ArrayList();
         List amdSupportedIndex = new ArrayList();
         PropertyValue[] var17 = hostProps;
         int var19 = hostProps.length;

         for(int var20 = 0; var20 < var19; ++var20) {
            PropertyValue propValue = var17[var20];
            String hostEvcMode = (String)propValue.value;
            if (StringUtils.isEmpty(hostEvcMode)) {
               break;
            }

            int i;
            if (hostEvcMode.contains(Vendor.amd.name())) {
               for(i = 0; i < supportedAmdEvcMode.size(); ++i) {
                  if (hostEvcMode.equals(((EvcModeData)supportedAmdEvcMode.get(i)).id)) {
                     amdSupportedIndex.add(i);
                     break;
                  }
               }
            }

            if (hostEvcMode.contains(Vendor.intel.name())) {
               for(i = 0; i < supportedIntelEvcMode.size(); ++i) {
                  if (hostEvcMode.equals(((EvcModeData)supportedIntelEvcMode.get(i)).id)) {
                     intelSupportedIndex.add(i);
                     break;
                  }
               }
            }
         }

         if ((intelSupportedIndex.size() == 0 || amdSupportedIndex.size() == 0) && (intelSupportedIndex.size() != 0 || amdSupportedIndex.size() != 0)) {
            Integer intelSupportedLength;
            if (intelSupportedIndex.size() != 0) {
               intelSupportedLength = (Integer)Collections.min(intelSupportedIndex) + 1;
               data.supportedIntelEvcMode = supportedIntelEvcMode.subList(0, intelSupportedLength);
            } else {
               intelSupportedLength = (Integer)Collections.min(amdSupportedIndex) + 1;
               data.supportedAmdEvcMode = supportedAmdEvcMode.subList(0, intelSupportedLength);
            }

            return data;
         } else {
            data.unsupportedEvcStatus = true;
            return data;
         }
      } else {
         return data;
      }
   }

   @TsService
   public Object getEvcModeValidationResult(ManagedObjectReference clusterRef, String evcModeKey) throws Exception {
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      Object var8;
      try {
         ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
         EVCManager evcManager = (EVCManager)vcConnection.createStub(EVCManager.class, cluster.evcManager());
         ManagedObjectReference task = evcManager.checkConfigureEvc(evcModeKey, (String)null);
         task.setServerGuid(clusterRef.getServerGuid());
         var8 = this.taskService.getResult(task);
      } catch (Throwable var17) {
         var4 = var17;
         throw var17;
      } finally {
         if (vcConnection != null) {
            if (var4 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var16) {
                  var4.addSuppressed(var16);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      return var8;
   }

   @TsService("configureHciClusterTask")
   public ManagedObjectReference configureCluster(ManagedObjectReference clusterRef, ClusterConfigData clusterConfigData) throws Exception {
      boolean vsanEnabled = clusterConfigData.basicConfig.vsanEnabled;
      HciWorkflowState state = clusterConfigData.basicConfig.hciWorkflowState;
      VcConnection vcConnection = this.getVcConnection(clusterRef, vsanEnabled);

      try {
         VsanProfiler.Point point = _profiler.point("ClusterComputeResource.reconfigureEx");
         Throwable var7 = null;

         try {
            Object var8;
            try {
               switch(state) {
               case IN_PROGRESS:
                  var8 = this.createWorkflow(vcConnection, clusterRef, clusterConfigData, this.getClusterHosts(clusterRef));
                  return (ManagedObjectReference)var8;
               case DONE:
                  var8 = this.extendWorkflow(vcConnection, clusterRef, clusterConfigData, this.getNotConfiguredClusterHosts(clusterRef));
                  return (ManagedObjectReference)var8;
               default:
                  throw new IllegalStateException("Illegal state of the HCI cluster: " + state);
               }
            } catch (Throwable var26) {
               var8 = var26;
               var7 = var26;
               throw var26;
            }
         } finally {
            if (point != null) {
               if (var7 != null) {
                  try {
                     point.close();
                  } catch (Throwable var25) {
                     var7.addSuppressed(var25);
                  }
               } else {
                  point.close();
               }
            }

         }
      } finally {
         if (clusterConfigData.basicConfig.vsanEnabled) {
            vcConnection.close();
         }

      }
   }

   @TsService
   public ManagedObjectReference simpleClusterExtend(ManagedObjectReference clusterRef) throws Exception {
      ClusterConfigData clusterConfigData = this.getClusterConfigData(clusterRef);
      VcConnection vcConnection = this.getVcConnection(clusterRef, clusterConfigData.basicConfig.vsanEnabled);
      return this.extendWorkflow(vcConnection, clusterRef, clusterConfigData, this.getNotConfiguredClusterHosts(clusterRef));
   }

   @TsService
   public void abandonHciWorkflowCluster(ManagedObjectReference clusterRef) throws Exception {
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
      ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
      if (cluster.getHciConfig() != null && HciWorkflowState.IN_PROGRESS == HciWorkflowState.fromString(cluster.getHciConfig().workflowState)) {
         cluster.AbandonHciWorkflow();
      }

   }

   private VcConnection getVcConnection(ManagedObjectReference clusterRef, boolean vsan) {
      return vsan ? this.vcClient.getVsanVmodlVersionConnection(clusterRef.getServerGuid()) : this.vcClient.getConnection(clusterRef.getServerGuid());
   }

   private ManagedObjectReference createWorkflow(VcConnection vcConnection, ManagedObjectReference clusterRef, ClusterConfigData clusterConfigData, List hosts) throws Exception {
      ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
      boolean hasEncryptionPermissions = this.encryptionPropertyProvider.getEncryptionPermissions(clusterRef);
      boolean hasRekeyPermissions = this.encryptionPropertyProvider.getReKeyPermissions(clusterRef);
      HCIConfigSpec hciConfigSpec = clusterConfigData.getHciConfigSpec(clusterRef, hasEncryptionPermissions, hasRekeyPermissions, this.hostDisksClaimer);
      HostConfigurationInput[] hostConfigurationInputs = clusterConfigData.getHostConfigurationInputs(hosts);
      ManagedObjectReference taskRef = cluster.configureHCI(hciConfigSpec, hostConfigurationInputs);
      return VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
   }

   private ManagedObjectReference extendWorkflow(VcConnection vcConnection, ManagedObjectReference clusterRef, ClusterConfigData clusterConfigData, List hosts) throws Exception {
      ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
      HostConfigurationInput[] hostConfigurationInputs = clusterConfigData.getHostConfigurationInputs(hosts);
      SDDCBase reconfigSpec = null;
      if (clusterConfigData.basicConfig.vsanEnabled) {
         if (clusterConfigData.vsanConfigSpec.stretchedClusterConfig != null && clusterConfigData.vsanConfigSpec.stretchedClusterConfig.witnessHost == null) {
            clusterConfigData.vsanConfigSpec.stretchedClusterConfig.witnessHost = this.getStretchedClusterWitnessHost(clusterRef);
         }

         reconfigSpec = clusterConfigData.vsanConfigSpec.getBasicReconfigSpec(this.hostDisksClaimer);
      }

      ManagedObjectReference taskRef = cluster.extendHCI(hostConfigurationInputs, reconfigSpec);
      taskRef.setServerGuid(clusterRef.getServerGuid());
      return taskRef;
   }

   private ManagedObjectReference getStretchedClusterWitnessHost(ManagedObjectReference clusterRef) {
      try {
         DataServiceResponse propertyResponse = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "allVsanHosts", ClusterComputeResource.class.getSimpleName(), new String[]{"isWitnessHost", "isMetadataWitnessHost"});
         Iterator var3 = propertyResponse.getResourceObjects().iterator();

         ManagedObjectReference hostRef;
         boolean isWitnessHost;
         boolean isMetadataWitnessHost;
         do {
            if (!var3.hasNext()) {
               return null;
            }

            Object obj = var3.next();
            hostRef = (ManagedObjectReference)obj;
            isWitnessHost = (Boolean)propertyResponse.getProperty(hostRef, "isWitnessHost");
            isMetadataWitnessHost = (Boolean)propertyResponse.getProperty(hostRef, "isMetadataWitnessHost");
         } while(!isWitnessHost || isMetadataWitnessHost);

         return hostRef;
      } catch (Exception var8) {
         logger.error("Unable to determine witness host for the cluster.");
         throw new VsanUiLocalizableException("vsan.hci.createWorkflow.witnessHost.error", var8);
      }
   }

   @TsService
   public List getPhysicalAdapters(ManagedObjectReference clusterRef) throws Exception {
      DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", ClusterComputeResource.class.getSimpleName(), new String[]{"config.network.pnic"});
      PropertyValue[] propertyValues = response.getPropertyValues();
      int nicCount = this.getMaxCommonNicCount(propertyValues);
      List names = this.getFirstNDeviceNames(propertyValues, nicCount);
      Collections.sort(names);
      return this.generateNHostAdapters(names, nicCount);
   }

   private int getMaxCommonNicCount(PropertyValue[] propertyValues) {
      int nicCount = Integer.MAX_VALUE;
      if (propertyValues.length != 0) {
         PropertyValue[] var3 = propertyValues;
         int var4 = propertyValues.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            PropertyValue propertyValue = var3[var5];
            PhysicalNic[] nics = (PhysicalNic[])((PhysicalNic[])propertyValue.value);
            nicCount = Math.min(nicCount, nics.length);
         }
      } else {
         nicCount = 0;
      }

      return nicCount;
   }

   private List getFirstNDeviceNames(PropertyValue[] hostPropertyValues, int number) {
      List result = new ArrayList();
      Map pnicNamesToHostCount = new HashMap();
      PropertyValue[] var5 = hostPropertyValues;
      int var6 = hostPropertyValues.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         PropertyValue propertyValue = var5[var7];
         PhysicalNic[] physicalNics = (PhysicalNic[])((PhysicalNic[])propertyValue.value);
         PhysicalNic[] var10 = physicalNics;
         int var11 = physicalNics.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            PhysicalNic physicalNic = var10[var12];
            String pnicName = physicalNic.device;
            Integer hostCount = 1;
            if (pnicNamesToHostCount.containsKey(pnicName)) {
               hostCount = (Integer)pnicNamesToHostCount.get(pnicName) + 1;
            }

            pnicNamesToHostCount.put(pnicName, hostCount);
         }
      }

      Iterator var16 = pnicNamesToHostCount.entrySet().iterator();

      while(var16.hasNext()) {
         Entry pnicNameToHostCount = (Entry)var16.next();
         if ((Integer)pnicNameToHostCount.getValue() == hostPropertyValues.length) {
            result.add(pnicNameToHostCount.getKey());
         }

         if (result.size() == number) {
            break;
         }
      }

      return result;
   }

   private List generateNHostAdapters(List names, int number) {
      List result = new ArrayList(number);
      if (names.size() != number) {
         logger.warn("Inconsistent physical adapter naming across the hosts is found. Only suitable physical adapters are shown.");
      }

      for(int i = 0; i < names.size(); ++i) {
         result.add(HostAdapter.create(Utils.getLocalizedString("vsan.hci.configureCluster.longAdapterNamePattern", String.valueOf(i), (String)names.get(i)), (String)names.get(i)));
      }

      return result;
   }

   @TsService
   public List getUniqueNewDvsNames(ManagedObjectReference clusterRef) throws Exception {
      List dvsNames = this.getExistingDvsNames(clusterRef);
      List result = new ArrayList();

      for(int i = 0; i < 3; ++i) {
         String newName = BaseUtils.getIndexedString(dvsNames, Utils.getLocalizedString("vsan.hci.configureCluster.dvs.defaultName"), " ");
         dvsNames.add(newName);
         result.add(newName);
      }

      return result;
   }

   @TsService
   public List getExistingDvsNames(ManagedObjectReference clusterRef) throws Exception {
      PropertyConstraint id = QueryUtil.createPropertyConstraint(VmwareDistributedVirtualSwitch.class.getSimpleName(), "serverGuid", com.vmware.vise.data.query.Comparator.EQUALS, clusterRef.getServerGuid());
      String[] properties = new String[]{"name"};
      ResultSet resultSet = QueryUtil.getData(QueryUtil.buildQuerySpec((Constraint)id, properties));
      DataServiceResponse response = QueryUtil.getDataServiceResponse(resultSet, properties);
      ArrayList dvsNames = new ArrayList();
      PropertyValue[] var7 = response.getPropertyValues();
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         PropertyValue propertyValue = var7[var9];
         dvsNames.add((String)propertyValue.value);
      }

      return dvsNames;
   }

   private void processDvsSettings(ConfigureWizardData wizardData, HCIConfigInfo hciConfig) {
      if (hciConfig != null && hciConfig.dvsSetting != null) {
         wizardData.optOutOfNetworking = false;
         DVSSetting[] var3 = hciConfig.dvsSetting;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            DVSSetting dvsSetting = var3[var5];
            DVPortgroupToServiceMapping[] mappings = dvsSetting.dvPortgroupSetting;
            if (mappings != null) {
               DVPortgroupToServiceMapping[] var8 = mappings;
               int var9 = mappings.length;

               for(int var10 = 0; var10 < var9; ++var10) {
                  DVPortgroupToServiceMapping mapping = var8[var10];

                  try {
                     Service service = Service.fromString(mapping.service);
                     switch(service) {
                     case VMOTION:
                        wizardData.showVmotionTrafficPage = true;
                        break;
                     case VSAN:
                        wizardData.showVsanTrafficPage = true;
                     }
                  } catch (Exception var13) {
                     logger.error("Unable to process DVS setting's mapping: " + mapping, var13);
                  }
               }
            }
         }

      }
   }

   private boolean validateNetworks(ConfigureWizardData wizardData, ManagedObjectReference clusterRef, HCIConfigInfo hciConfig, boolean vsanEnabled) {
      if (wizardData.optOutOfNetworking) {
         return true;
      } else {
         try {
            boolean hasExtendNetworkingPermissions = this.hasExtendNetworkingPermissions(clusterRef, hciConfig);
            if (!hasExtendNetworkingPermissions) {
               wizardData.openWarningDialog = true;
               wizardData.dialogText = Utils.getLocalizedString("vsan.hci.dialog.configureHostsWarning.title");
               return false;
            }
         } catch (Exception var8) {
            logger.error("Unable to validate network permissions, assume allowed", var8);
         }

         String[] networkValidationMessages = null;

         try {
            networkValidationMessages = this.getNetworkValidationMessages(clusterRef, (DvsProfile[])null, this.getNotConfiguredHosts(clusterRef), vsanEnabled);
         } catch (Exception var7) {
            logger.error("Unable to validate HCI wizard network configuration.", var7);
         }

         if (ArrayUtils.isNotEmpty(networkValidationMessages)) {
            wizardData.openWarningDialog = true;
            wizardData.dialogText = Utils.getLocalizedString("vsan.hci.dialog.configureHostsWarning.networkConfigurationError.title");
            wizardData.warningDialogContent = networkValidationMessages;
            return false;
         } else {
            return true;
         }
      }
   }

   @TsService
   public boolean hasNetworkingModifyPermissions(ManagedObjectReference[] dvSwitches, ManagedObjectReference[] dvPortgroups) throws Exception {
      boolean hasDvsCreatePermission = true;
      boolean hasDvpgCreatePermission = true;
      if (ArrayUtils.isNotEmpty(dvSwitches)) {
         hasDvsCreatePermission = this.permissionService.havePermissions(dvSwitches, new String[]{"DVSwitch.HostOp"});
      }

      if (ArrayUtils.isNotEmpty(dvPortgroups)) {
         hasDvpgCreatePermission = this.permissionService.havePermissions(dvPortgroups, new String[]{"Network.Assign"});
      }

      return hasDvsCreatePermission && hasDvpgCreatePermission;
   }

   @TsService
   public boolean hasNetworkingCreatePermissions(ManagedObjectReference clusterRef, boolean checkDvsCreatePermission, boolean checkDvpgCreatePermission) throws Exception {
      ManagedObjectReference datacenter = (ManagedObjectReference)QueryUtil.getProperty(clusterRef, "dc", (Object)null);
      List permissionsToCheck = new ArrayList();
      if (checkDvsCreatePermission) {
         permissionsToCheck.add("DVSwitch.Create");
      }

      if (checkDvpgCreatePermission) {
         permissionsToCheck.add("DVPortgroup.Create");
      }

      return this.permissionService.hasPermissions(datacenter, (String[])permissionsToCheck.toArray(new String[0]));
   }

   private boolean getLargeScaleClusterSupport(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(clusterRef);
      return configInfoEx.extendedConfig.largeScaleClusterSupport;
   }

   private boolean hasHostConfigurePermissions(ManagedObjectReference clusterRef) {
      try {
         ManagedObjectReference[] hosts = (ManagedObjectReference[])QueryUtil.getProperty(clusterRef, "host", (Object)null);
         if (ArrayUtils.isEmpty(hosts)) {
            return true;
         } else {
            ManagedObjectReference[] var3 = hosts;
            int var4 = hosts.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               ManagedObjectReference host = var3[var5];
               if (!this.permissionService.hasPermissions(host, new String[]{"Host.Config.Network"})) {
                  return false;
               }
            }

            return true;
         }
      } catch (Exception var7) {
         logger.error("Unable to determine hosts' configure permissions. Default to true");
         return true;
      }
   }

   private boolean hasExtendNetworkingPermissions(ManagedObjectReference clusterRef, HCIConfigInfo hciConfig) throws Exception {
      boolean hasDvsModify = true;
      if (hciConfig.dvsSetting != null) {
         DVSSetting[] var4 = hciConfig.dvsSetting;
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            DVSSetting dvsSetting = var4[var6];
            if (!this.permissionService.hasPermissions(dvsSetting.dvSwitch, new String[]{"DVSwitch.Modify"})) {
               hasDvsModify = false;
               break;
            }
         }
      }

      List dvPortgroups = this.getDvPortgroups(hciConfig);
      boolean hasNetworkAssign = true;
      Iterator var14 = dvPortgroups.iterator();

      while(var14.hasNext()) {
         ManagedObjectReference dvPortgroup = (ManagedObjectReference)var14.next();
         if (!this.permissionService.hasPermissions(dvPortgroup, new String[]{"Network.Assign"})) {
            hasNetworkAssign = false;
            break;
         }
      }

      ManagedObjectReference[] notConfiguredHosts = this.getNotConfiguredHosts(clusterRef);
      boolean hasHostNetworkConfig = true;
      ManagedObjectReference[] var8 = notConfiguredHosts;
      int var9 = notConfiguredHosts.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         ManagedObjectReference host = var8[var10];
         if (!this.permissionService.hasPermissions(host, new String[]{"Host.Config.Network"})) {
            hasHostNetworkConfig = false;
            break;
         }
      }

      return hasDvsModify && hasNetworkAssign && hasHostNetworkConfig;
   }

   private boolean isStretchedCluster(ManagedObjectReference clusterRef) {
      ManagedObjectReference witnessHost = this.getStretchedClusterWitnessHost(clusterRef);
      return witnessHost != null;
   }

   private Map getDvsInfoData(HCIConfigInfo hciConfigInfo) throws Exception {
      Map dvsDataByService = new HashMap();
      Map dvpgMorByService = new HashMap();
      Map dvsMorByService = new HashMap();
      if (hciConfigInfo == null) {
         return dvsDataByService;
      } else {
         DVSSetting[] dvsSettings = hciConfigInfo.getDvsSetting();
         if (ArrayUtils.isEmpty(dvsSettings)) {
            return dvsDataByService;
         } else {
            DVSSetting[] var6 = dvsSettings;
            int var7 = dvsSettings.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               DVSSetting dvsSetting = var6[var8];
               DVPortgroupToServiceMapping[] dvPortgroupSetting = dvsSetting.dvPortgroupSetting;
               if (ArrayUtils.isNotEmpty(dvPortgroupSetting)) {
                  DVPortgroupToServiceMapping[] var11 = dvPortgroupSetting;
                  int var12 = dvPortgroupSetting.length;

                  for(int var13 = 0; var13 < var12; ++var13) {
                     DVPortgroupToServiceMapping dvpgSetting = var11[var13];
                     if (dvpgSetting != null && (Service.fromString(dvpgSetting.service) == Service.VSAN || Service.fromString(dvpgSetting.service) == Service.VMOTION)) {
                        dvsMorByService.put(Service.fromString(dvpgSetting.service), dvsSetting.dvSwitch);
                        dvpgMorByService.put(Service.fromString(dvpgSetting.service), dvpgSetting.dvPortgroup);
                     }
                  }
               }
            }

            DataServiceResponse responseForDvpg;
            if (CollectionUtils.isNotEmpty(dvsMorByService.values())) {
               responseForDvpg = QueryUtil.getProperties((ManagedObjectReference[])dvsMorByService.values().toArray(new ManagedObjectReference[0]), new String[]{"name"});
               this.setDvsDataByService(dvsDataByService, dvsMorByService, responseForDvpg);
            }

            if (CollectionUtils.isNotEmpty(dvpgMorByService.values())) {
               responseForDvpg = QueryUtil.getProperties((ManagedObjectReference[])dvpgMorByService.values().toArray(new ManagedObjectReference[0]), new String[]{"config.defaultPortConfig.vlan"});
               this.setDvsDataByService(dvsDataByService, dvpgMorByService, responseForDvpg);
            }

            return dvsDataByService;
         }
      }
   }

   @TsService
   public String[] validateNetworkSpecification(ManagedObjectReference clusterRef, ClusterConfigData clusterConfigData) throws Exception {
      return this.getNetworkValidationMessages(clusterRef, clusterConfigData.getDvsProfiles(), (ManagedObjectReference[])null, clusterConfigData.basicConfig.vsanEnabled);
   }

   @TsService
   public List getExistingPgNames(ManagedObjectReference clusterRef) throws Exception {
      PropertyConstraint id = QueryUtil.createPropertyConstraint(DistributedVirtualPortgroup.class.getSimpleName(), "serverGuid", com.vmware.vise.data.query.Comparator.EQUALS, clusterRef.getServerGuid());
      String[] properties = new String[]{"name"};
      ResultSet resultSet = QueryUtil.getData(QueryUtil.buildQuerySpec((Constraint)id, properties));
      DataServiceResponse response = QueryUtil.getDataServiceResponse(resultSet, properties);
      ArrayList pgNames = new ArrayList();
      PropertyValue[] var7 = response.getPropertyValues();
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         PropertyValue propertyValue = var7[var9];
         pgNames.add((String)propertyValue.value);
      }

      return pgNames;
   }

   @TsService
   public List getExistingDvs(ManagedObjectReference clusterRef, String selectedDvsName) throws Exception {
      List result = new ArrayList();
      List hostsInCluster = new ArrayList();
      PropertyValue[] hostValues = QueryUtil.getPropertyForRelatedObjects(clusterRef, "host", ClusterComputeResource.class.getSimpleName(), "config.product.version").getPropertyValues();
      if (hostValues == null) {
         return result;
      } else {
         int lowestHostVersion = Integer.MAX_VALUE;
         PropertyValue[] var7 = hostValues;
         int var8 = hostValues.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            PropertyValue hostValue = var7[var9];
            hostsInCluster.add((ManagedObjectReference)hostValue.resourceObject);
            int currentHostVersion = Integer.parseInt(((String)hostValue.value).replaceAll("\\.", ""));
            if (currentHostVersion < lowestHostVersion) {
               lowestHostVersion = currentHostVersion;
            }
         }

         Map responseForDvs = this.queryDvsProperties(clusterRef);
         Iterator var13 = responseForDvs.entrySet().iterator();

         while(var13.hasNext()) {
            Entry dvsEntry = (Entry)var13.next();
            Map dvsProperties = (Map)dvsEntry.getValue();
            if (!this.isDvsVersionIncompatible((String)dvsProperties.get("config.productInfo.version"), lowestHostVersion) && !this.isDvsConnectedToHostInCluster(hostsInCluster, (HostMember[])((HostMember[])dvsProperties.get("config.host")))) {
               ExistingDvsData existingDvsData = new ExistingDvsData();
               existingDvsData.dvsRef = (ManagedObjectReference)dvsEntry.getKey();
               existingDvsData.name = (String)dvsProperties.get("name");
               existingDvsData.version = (String)dvsProperties.get("config.productInfo.version");
               existingDvsData.niocVersion = (String)dvsProperties.get("lacpVersionColumnLabelDerived");
               existingDvsData.lacpVersion = (String)dvsProperties.get("niocVersionColumnLabel");
               if (existingDvsData.name.equals(selectedDvsName)) {
                  existingDvsData.isSelected = true;
                  result.add(0, existingDvsData);
               } else {
                  result.add(existingDvsData);
               }
            }
         }

         return result;
      }
   }

   @TsService
   public List getExistingDvpg(ManagedObjectReference dvsRef, String selectedDvpgName) throws Exception {
      List result = new ArrayList();
      Map dvpgResponse = QueryUtil.getPropertiesForRelatedObjects(dvsRef, "portgroup", DistributedVirtualPortgroup.class.getSimpleName(), new String[]{"name", "config.uplink"}).getMap();
      if (MapUtils.isEmpty(dvpgResponse)) {
         return result;
      } else {
         Iterator var5 = dvpgResponse.entrySet().iterator();

         while(var5.hasNext()) {
            Entry dvpgEntry = (Entry)var5.next();
            Map dvpgProperties = (Map)dvpgEntry.getValue();
            if (!(Boolean)dvpgProperties.get("config.uplink")) {
               ExistingDvpgData existingDvpgData = new ExistingDvpgData();
               existingDvpgData.dvpgRef = (ManagedObjectReference)dvpgEntry.getKey();
               existingDvpgData.name = (String)dvpgProperties.get("name");
               if (existingDvpgData.name.equals(selectedDvpgName)) {
                  existingDvpgData.isSelected = true;
                  result.add(0, existingDvpgData);
               } else {
                  result.add(existingDvpgData);
               }
            }
         }

         return result;
      }
   }

   @TsService
   public VlanData getDvpgVlan(ManagedObjectReference dvpgRef) throws Exception {
      VlanSpec dvpgVlanSpec = (VlanSpec)QueryUtil.getProperty(dvpgRef, "config.defaultPortConfig.vlan", (Object)null);
      return dvpgVlanSpec == null ? null : this.getVlanData(dvpgVlanSpec);
   }

   private String[] getNetworkValidationMessages(ManagedObjectReference clusterRef, DvsProfile[] dvsProfiles, ManagedObjectReference[] notConfiguredHosts, boolean vsanEnabled) throws Exception {
      VcConnection vcConnection = this.getVcConnection(clusterRef, vsanEnabled);
      ArrayList messages = new ArrayList();

      try {
         VsanProfiler.Point point = _profiler.point("ClusterComputeResource.validateHCIConfiguration");
         Throwable var8 = null;

         ValidationResultBase[] var12;
         try {
            ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
            HCIConfigSpec hciConfigSpec = new HCIConfigSpec();
            hciConfigSpec.dvsProf = dvsProfiles;
            ValidationResultBase[] validationResultBase = cluster.validateHCIConfiguration(hciConfigSpec, notConfiguredHosts);
            if (!ArrayUtils.isEmpty(validationResultBase)) {
               var12 = validationResultBase;
               int var13 = validationResultBase.length;

               for(int var14 = 0; var14 < var13; ++var14) {
                  ValidationResultBase validationResult = var12[var14];
                  if (ArrayUtils.isEmpty(validationResult.info)) {
                     logger.warn("Unexpected ValidationResultBase value retrieved: validationResult.info array is empty.");
                  } else {
                     String message = "";
                     LocalizableMessage[] var17 = validationResult.info;
                     int var18 = var17.length;

                     for(int var19 = 0; var19 < var18; ++var19) {
                        LocalizableMessage infoMessage = var17[var19];
                        message = message.concat(infoMessage.getMessage());
                     }

                     messages.add(message);
                  }
               }

               return CollectionUtils.isNotEmpty(messages) ? (String[])messages.toArray(new String[0]) : null;
            }

            var12 = null;
         } catch (Throwable var30) {
            var8 = var30;
            throw var30;
         } finally {
            if (point != null) {
               if (var8 != null) {
                  try {
                     point.close();
                  } catch (Throwable var29) {
                     var8.addSuppressed(var29);
                  }
               } else {
                  point.close();
               }
            }

         }

         return var12;
      } catch (InvalidState var32) {
         return new String[]{Utils.getLocalizedString("vsan.hci.configureCluster.dvsVerification.clusterNotInHCI")};
      }
   }

   private void setDvsDataByService(Map dvsDataByService, Map morByService, DataServiceResponse response) {
      PropertyValue[] var4 = response.getPropertyValues();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         PropertyValue propertyValue = var4[var6];
         if ("name".equals(propertyValue.propertyName)) {
            String switchName = (String)propertyValue.value;
            Iterator var14 = morByService.entrySet().iterator();

            while(var14.hasNext()) {
               Entry entry = (Entry)var14.next();
               if (((ManagedObjectReference)entry.getValue()).equals(propertyValue.resourceObject)) {
                  if (dvsDataByService.get(entry.getKey()) == null) {
                     dvsDataByService.put(entry.getKey(), new DvsData());
                  }

                  ((DvsData)dvsDataByService.get(entry.getKey())).dvsName = switchName;
               }
            }
         } else if ("config.defaultPortConfig.vlan".equals(propertyValue.propertyName)) {
            VlanSpec dvpgVlanSpec = (VlanSpec)propertyValue.value;
            VlanData vlanData = this.getVlanData(dvpgVlanSpec);
            Iterator var10 = morByService.entrySet().iterator();

            while(var10.hasNext()) {
               Entry entry = (Entry)var10.next();
               if (((ManagedObjectReference)entry.getValue()).equals(propertyValue.resourceObject)) {
                  if (dvsDataByService.get(entry.getKey()) == null) {
                     dvsDataByService.put(entry.getKey(), new DvsData());
                  }

                  ((DvsData)dvsDataByService.get(entry.getKey())).vlan = vlanData.vlan;
                  ((DvsData)dvsDataByService.get(entry.getKey())).vlanType = vlanData.vlanType;
               }
            }
         }
      }

   }

   private VlanData getVlanData(VlanSpec dvpgVlanSpec) {
      VlanData vlanData = new VlanData();
      vlanData.vlan = "0";
      vlanData.vlanType = VlanType.NONE;
      if (dvpgVlanSpec != null) {
         if (dvpgVlanSpec instanceof VlanIdSpec) {
            vlanData.vlan = String.valueOf(((VlanIdSpec)dvpgVlanSpec).vlanId);
            vlanData.vlanType = VlanType.VLAN_ID;
         } else if (dvpgVlanSpec instanceof TrunkVlanSpec) {
            NumericRange[] trunkRanges = ((TrunkVlanSpec)dvpgVlanSpec).vlanId;
            vlanData.vlan = NumberUtils.parseNumericRange(trunkRanges);
            vlanData.vlanType = VlanType.VLAN_TRUNK;
         } else if (dvpgVlanSpec instanceof PvlanSpec) {
            vlanData.vlan = String.valueOf(((PvlanSpec)dvpgVlanSpec).pvlanId);
            vlanData.vlanType = VlanType.PVLAN;
         }
      }

      return vlanData;
   }

   private List getDvPortgroups(HCIConfigInfo hciConfig) {
      List result = new ArrayList();
      if (hciConfig.dvsSetting != null) {
         DVSSetting[] var3 = hciConfig.dvsSetting;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            DVSSetting dvsSetting = var3[var5];
            if (dvsSetting.dvPortgroupSetting != null) {
               DVPortgroupToServiceMapping[] var7 = dvsSetting.dvPortgroupSetting;
               int var8 = var7.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  DVPortgroupToServiceMapping mapping = var7[var9];
                  result.add(mapping.dvPortgroup);
               }
            }
         }
      }

      return result;
   }

   @TsService
   public VsanTestData getHealthGroupData(ManagedObjectReference clusterRef, String perspective, String group) throws Exception {
      VsanClusterHealthSummary healthSummary = this.getHealthSummary(clusterRef, perspective);
      Set allMoRefs = new HashSet();
      VsanClusterHealthGroup[] var6;
      int var7;
      int var8;
      VsanClusterHealthGroup currentGroup;
      if (healthSummary != null && healthSummary.groups != null) {
         var6 = healthSummary.groups;
         var7 = var6.length;

         for(var8 = 0; var8 < var7; ++var8) {
            currentGroup = var6[var8];
            VsanHealthUtil.addToTestMoRefs(currentGroup, allMoRefs, clusterRef.getServerGuid());
         }
      }

      var6 = healthSummary.groups;
      var7 = var6.length;

      for(var8 = 0; var8 < var7; ++var8) {
         currentGroup = var6[var8];
         if (currentGroup.groupName.equals(group)) {
            return new VsanTestData(currentGroup, healthSummary.timestamp, VsanHealthUtil.getNamesForMoRefs(allMoRefs), false);
         }
      }

      return null;
   }

   @TsService
   public void hideSupportInsightStep(ManagedObjectReference clusterRef) {
      try {
         VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         try {
            OptionManager optionManager = (OptionManager)vcConnection.createStub(OptionManager.class, vcConnection.getContent().setting);
            OptionValue newOption = new OptionValue("config.vsan.hide_ceip_page_in_hci_wofkflow", Boolean.TRUE.toString());
            optionManager.updateValues(new OptionValue[]{newOption});
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (vcConnection != null) {
               if (var3 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }

      } catch (Exception var16) {
         logger.error("Failed to save config.vsan.hide_ceip_page_in_hci_wofkflow", var16);
         throw new VsanUiLocalizableException("vsan.hci.dialog.hideSupportInsight.error", var16);
      }
   }

   private VsanClusterHealthSummary getHealthSummary(ManagedObjectReference clusterRef, String perspective) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      Object var8;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanProfiler.Point point = _profiler.point("healthSystem.queryClusterHealthSummary");
         Throwable var7 = null;

         try {
            var8 = healthSystem.queryClusterHealthSummary(clusterRef, (Integer)null, (String[])null, true, new String[]{"groups", "timestamp"}, false, perspective, (ManagedObjectReference[])null, (VsanClusterHealthQuerySpec)null, (Boolean)null);
         } catch (Throwable var31) {
            var8 = var31;
            var7 = var31;
            throw var31;
         } finally {
            if (point != null) {
               if (var7 != null) {
                  try {
                     point.close();
                  } catch (Throwable var30) {
                     var7.addSuppressed(var30);
                  }
               } else {
                  point.close();
               }
            }

         }
      } catch (Throwable var33) {
         var4 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var4.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      return (VsanClusterHealthSummary)var8;
   }

   private Map queryDvsProperties(ManagedObjectReference clusterRef) throws Exception {
      ManagedObjectReference datacenter = (ManagedObjectReference)QueryUtil.getProperty(clusterRef, "dc", (Object)null);
      PropertyConstraint id = QueryUtil.createPropertyConstraint(VmwareDistributedVirtualSwitch.class.getSimpleName(), "dc", com.vmware.vise.data.query.Comparator.EQUALS, datacenter);
      ResultSet resultSet = QueryUtil.getData(QueryUtil.buildQuerySpec((Constraint)id, EXISTING_DVS_PROPERTIES));
      DataServiceResponse dvsRefResponse = QueryUtil.getDataServiceResponse(resultSet, EXISTING_DVS_PROPERTIES);
      return dvsRefResponse.getMap();
   }

   private boolean isDvsVersionIncompatible(String dvsVersion, int lowestHostVersion) {
      int parsedDvsVersion = Integer.parseInt(dvsVersion.replaceAll("\\.", ""));
      return lowestHostVersion < parsedDvsVersion;
   }

   private boolean isDvsConnectedToHostInCluster(List hostsInCluster, HostMember[] hostMembers) {
      if (CollectionUtils.isNotEmpty(hostsInCluster) && ArrayUtils.isNotEmpty(hostMembers)) {
         HostMember[] var3 = hostMembers;
         int var4 = hostMembers.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            HostMember hostMember = var3[var5];
            if (hostMember != null && hostMember.config != null && hostMember.config.host != null && hostsInCluster.contains(hostMember.config.host)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isHideSupportInsightStepConfigured(ManagedObjectReference clusterRef) {
      try {
         VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         try {
            OptionManager optionManager = (OptionManager)vcConnection.createStub(OptionManager.class, vcConnection.getContent().setting);
            OptionValue[] optionValues = optionManager.getSetting();
            if (ArrayUtils.isEmpty(optionValues)) {
               boolean var25 = false;
               return var25;
            } else {
               OptionValue[] var6 = optionValues;
               int var7 = optionValues.length;

               for(int var8 = 0; var8 < var7; ++var8) {
                  OptionValue optionValue = var6[var8];
                  if ("config.vsan.hide_ceip_page_in_hci_wofkflow".equals(optionValue.key) && optionValue.value != null && BooleanUtils.isTrue(Boolean.parseBoolean(optionValue.value.toString()))) {
                     boolean var10 = true;
                     return var10;
                  }
               }

               return false;
            }
         } catch (Throwable var22) {
            var3 = var22;
            throw var22;
         } finally {
            if (vcConnection != null) {
               if (var3 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var21) {
                     var3.addSuppressed(var21);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      } catch (Exception var24) {
         logger.error("Failed to read config.vsan.hide_ceip_page_in_hci_wofkflow", var24);
         return false;
      }
   }
}
