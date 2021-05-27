package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.config.model.VsanClusterType;
import com.vmware.vsphere.client.vsan.health.ExternalProxySettingsConfig;

@TsModel
public class ConfigureWizardData {
   public boolean openYesNoDialog;
   public boolean openWarningDialog;
   public String dialogText;
   public String[] warningDialogContent;
   public boolean isExtend;
   public boolean optOutOfNetworking;
   public boolean optOutOfNetworkingDisabled;
   public boolean enableFaultDomainForSingleSiteCluster;
   public boolean largeScaleClusterSupport;
   public boolean showDvsPage;
   public boolean showVmotionTrafficPage;
   public boolean showVsanTrafficPage;
   public boolean showAdvancedOptionsPage;
   public boolean showClaimDisksPage;
   public boolean showFaultDomainsPageComponent;
   public boolean showSingleSiteFaultDomainsPage;
   public boolean showWitnessHostPageComponent;
   public boolean showClaimDisksWitnessHostPage;
   public boolean disableSupportInsight;
   public boolean isSupportInsightStepHidden;
   public boolean ceipEnabled;
   public ExternalProxySettingsConfig proxySettingsConfig;
   public VsanClusterType selectedVsanClusterType;
}
