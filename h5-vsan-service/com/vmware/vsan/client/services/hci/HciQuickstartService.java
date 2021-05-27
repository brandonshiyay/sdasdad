package com.vmware.vsan.client.services.hci;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.AuthorizationManager;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.AuthorizationManager.DisabledMethodInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthTest;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthPerspective;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.services.hci.model.BasicClusterConfigData;
import com.vmware.vsan.client.services.hci.model.ConfigCardData;
import com.vmware.vsan.client.services.hci.model.HciWorkflowState;
import com.vmware.vsan.client.services.hci.model.QuickstartViewData;
import com.vmware.vsan.client.services.hci.model.ValidationData;
import com.vmware.vsan.client.services.hci.model.VsanHealthCheck;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.health.VsanHealthStatus;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HciQuickstartService {
   @Autowired
   private HciClusterService hciClusterService;
   @Autowired
   private PermissionService permissionService;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;
   private static final Log logger = LogFactory.getLog(HciQuickstartService.class);
   private static final String DATACENTER_HOST_FOLDER_PROPERTY = "hostFolder";
   private static final String BASIC_CARD_ACTION_ID = "vsphere.core.cluster.actions.edit";
   private static final String ADD_HOSTS_CARD_ACTION_ID = "vsphere.core.hci.addHosts";
   private static final String CONFIGURE_CARD_ACTION_ID = "com.vmware.vsan.client.h5vsanui.cluster.configureHciCluster";
   private static final String CONFIGURE_HCI_DISABLED_METHOD = "configureHCI";
   private static final String EXTEND_HCI_DISABLED_METHOD = "extendHCI";

   @TsService
   public QuickstartViewData getGettingStartedData(ManagedObjectReference clusterRef) {
      BasicClusterConfigData basicClusterData = this.hciClusterService.getBasicClusterConfigData(clusterRef);
      boolean hasEditClusterPermission = true;

      try {
         hasEditClusterPermission = this.permissionService.hasPermissions(clusterRef, new String[]{"Host.Inventory.EditCluster"});
      } catch (Exception var6) {
         logger.error("Unable to determine cluster permissions, assuming granted", var6);
      }

      QuickstartViewData result = this.getQuickstartViewInfo(basicClusterData, hasEditClusterPermission);

      try {
         result.configurationCards = new ConfigCardData[]{this.getBasicConfigCard(basicClusterData, hasEditClusterPermission), this.getAddHostsCard(basicClusterData, hasEditClusterPermission && this.hasAddHostsPermissions(clusterRef)), this.getConfigureCard(basicClusterData, hasEditClusterPermission, clusterRef)};
         return result;
      } catch (Exception var7) {
         throw new VsanUiLocalizableException("vsan.hci.gettingStarted.basicConfigCard.error", var7);
      }
   }

   private QuickstartViewData getQuickstartViewInfo(BasicClusterConfigData basicClusterData, boolean hasEditClusterPermission) {
      QuickstartViewData quickstartData = new QuickstartViewData();
      quickstartData.isVsanEnabled = basicClusterData.vsanEnabled;
      switch(basicClusterData.hciWorkflowState) {
      case IN_PROGRESS:
         quickstartData.header = Utils.getLocalizedString("vsan.hci.gettingStarted.createWorkflow.header");
         quickstartData.text = Utils.getLocalizedString("vsan.hci.gettingStarted.createWorkflow.text");
         quickstartData.showSendFeedbackLink = false;
         break;
      case DONE:
         if (basicClusterData.notConfiguredHosts == 0) {
            quickstartData.header = Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.initial.header");
            quickstartData.text = Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.initial.text");
            quickstartData.showSendFeedbackLink = true;
         } else {
            quickstartData.header = Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.inProgress.header");
            quickstartData.text = Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.inProgress.text");
            quickstartData.showSendFeedbackLink = false;
         }
         break;
      case INVALID:
         quickstartData.header = Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.abandoned.header");
         quickstartData.text = basicClusterData.isComputeOnlyCluster ? Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.abandoned.text.computeOnly") : Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.abandoned.text");
         quickstartData.showSendFeedbackLink = true;
         break;
      case NOT_IN_HCI_WORKFLOW:
      default:
         quickstartData.header = Utils.getLocalizedString("vsan.hci.gettingStarted.createWorkflow.header");
         quickstartData.text = basicClusterData.isComputeOnlyCluster ? Utils.getLocalizedString("vsan.hci.gettingStarted.extendWorkflow.abandoned.text.computeOnly") : Utils.getLocalizedString("vsan.hci.gettingStarted.createWorkflow.text");
         quickstartData.showSendFeedbackLink = false;
      }

      quickstartData.showCloseQuickstartButton = basicClusterData.hciWorkflowState == HciWorkflowState.IN_PROGRESS && hasEditClusterPermission;
      quickstartData.extendCard = basicClusterData.hosts > 0;
      return quickstartData;
   }

   private ConfigCardData getBasicConfigCard(BasicClusterConfigData basicClusterConfigData, boolean hasPermissions) {
      ConfigCardData result = new ConfigCardData(Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.title"), "vsphere.core.cluster.actions.edit", false, false, Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.launchButton.text"));
      result.enabled = basicClusterConfigData.hciWorkflowState == HciWorkflowState.IN_PROGRESS && hasPermissions;
      result.status = ConfigCardData.Status.PASSED;
      result.listItems = this.getEnabledServices(basicClusterConfigData);
      result.personalityManagerEnabledText = basicClusterConfigData.pmanEnabled ? Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.personalityManagerEnabledText") : null;
      result.contentHeader = Utils.getLocalizedString(result.listItems.size() > 0 ? "vsan.hci.gettingStarted.basicConfigCard.contentHeader.selectedServices" : "vsan.hci.gettingStarted.basicConfigCard.contentHeader.noServices");
      return result;
   }

   private ConfigCardData getBasicAddHostsCard() {
      return new ConfigCardData(Utils.getLocalizedString("vsan.hci.gettingStarted.addHostsCard.title"), "vsphere.core.hci.addHosts", true, false, Utils.getLocalizedString("vsan.hci.gettingStarted.addHostsCard.launchButton.text"));
   }

   @TsService
   public ConfigCardData validateCluster(ManagedObjectReference clusterRef) {
      BasicClusterConfigData basicClusterConfigData = this.hciClusterService.getBasicClusterConfigData(clusterRef);
      ConfigCardData result = this.getBasicConfigureCard();
      result.enabled = basicClusterConfigData.notConfiguredHosts > 0;
      if (basicClusterConfigData.hciWorkflowState.equals(HciWorkflowState.DONE)) {
         result.title = Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.titleInExtend");
         result.validateEnabled = basicClusterConfigData.hosts > 0 && basicClusterConfigData.notConfiguredHosts == 0;
      }

      try {
         this.populateHealthChecksResult(result, clusterRef, VsanHealthPerspective.defaultView.toString(), true);
         return result;
      } catch (Exception var5) {
         logger.error("Unable to validate cluster");
         throw new VsanUiLocalizableException("vsan.hci.gettingStarted.cluster.validate.error", var5);
      }
   }

   @TsService
   public ConfigCardData validateNotConfiguredHosts(ManagedObjectReference clusterRef) {
      boolean hasEditClusterPermission = true;

      try {
         hasEditClusterPermission = this.permissionService.hasPermissions(clusterRef, new String[]{"Host.Inventory.EditCluster"});
      } catch (Exception var7) {
         logger.error("Unable to get edit cluster permissions. Default to true", var7);
      }

      BasicClusterConfigData basicClusterConfigData = this.hciClusterService.getBasicClusterConfigData(clusterRef);
      ConfigCardData result = this.getBasicAddHostsCard();
      result.contentHeader = this.getHostsNumLabel(basicClusterConfigData);
      result.validateEnabled = true;
      result.enabled = hasEditClusterPermission && this.hasAddHostsPermissions(clusterRef);

      try {
         this.populateHealthChecksResult(result, clusterRef, VsanHealthPerspective.beforeConfigureHost.toString(), false);
         return result;
      } catch (Exception var6) {
         logger.error("Unable to validate cluster");
         throw new VsanUiLocalizableException("vsan.hci.gettingStarted.cluster.validate.error", var6);
      }
   }

   private void populateHealthChecksResult(ConfigCardData card, ManagedObjectReference clusterRef, String perspective, Boolean showGroupsOnly) throws Exception {
      VsanClusterHealthSummary healthSummary = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var7 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         String[] requiredFields = new String[]{"groups"};
         Measure measure = new Measure("healthSystem.queryClusterHealthSummary");
         Throwable var11 = null;

         try {
            healthSummary = healthSystem.queryClusterHealthSummary(clusterRef, (Integer)null, (String[])null, false, requiredFields, false, perspective, (ManagedObjectReference[])null, (VsanClusterHealthQuerySpec)null, (Boolean)null);
         } catch (Throwable var34) {
            var11 = var34;
            throw var34;
         } finally {
            if (measure != null) {
               if (var11 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var33) {
                     var11.addSuppressed(var33);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Throwable var36) {
         var7 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var7 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var7.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      List healthChecks = new ArrayList();
      if (healthSummary != null && healthSummary.groups != null) {
         VsanClusterHealthGroup[] var39 = healthSummary.groups;
         int var40 = var39.length;

         for(int var43 = 0; var43 < var40; ++var43) {
            VsanClusterHealthGroup group = var39[var43];
            if (showGroupsOnly) {
               VsanHealthCheck healthCheck = new VsanHealthCheck(perspective, group.groupName, (String)null, (String)null, group.groupName, group.groupHealth);
               healthChecks.add(healthCheck);
            } else if (group.groupTests != null) {
               VsanClusterHealthTest[] var45 = group.groupTests;
               int var12 = var45.length;

               for(int var13 = 0; var13 < var12; ++var13) {
                  VsanClusterHealthTest test = var45[var13];
                  VsanHealthCheck healthCheck = new VsanHealthCheck(perspective, group.groupName, test.testId, test.testName, test.testName, test.testHealth);
                  healthChecks.add(healthCheck);
               }
            }
         }
      }

      boolean vsanEnabled = (Boolean)QueryUtil.getProperty(clusterRef, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled", (Object)null);
      card.validationData = new ValidationData(healthChecks, vsanEnabled);
      VsanHealthStatus overallStatus = VsanHealthStatus.valueOf(healthSummary.overallHealth);
      card.status = overallStatus == VsanHealthStatus.red ? ConfigCardData.Status.ERROR : ConfigCardData.Status.PASSED;
   }

   private ConfigCardData getAddHostsCard(BasicClusterConfigData basicClusterConfigData, boolean hasPermissions) {
      ConfigCardData result = this.getBasicAddHostsCard();
      result.enabled = hasPermissions;
      result.validateEnabled = basicClusterConfigData.hosts > 0 && basicClusterConfigData.notConfiguredHosts > 0 && basicClusterConfigData.hciWorkflowState != HciWorkflowState.INVALID;
      result.nextStep = !result.validateEnabled;
      if (basicClusterConfigData.hosts == 0) {
         result.contentText = Utils.getLocalizedString("vsan.hci.gettingStarted.addHostsCard.contentText");
         result.status = ConfigCardData.Status.NOT_AVAILABLE;
      } else {
         result.contentHeader = this.getHostsNumLabel(basicClusterConfigData);
         result.status = ConfigCardData.Status.PASSED;
      }

      return result;
   }

   private String getHostsNumLabel(BasicClusterConfigData basicClusterConfigData) {
      if (basicClusterConfigData.notConfiguredHosts != 0) {
         return basicClusterConfigData.notConfiguredHosts == basicClusterConfigData.hosts ? Utils.getLocalizedString("vsan.hci.gettingStarted.addHostsCard.notConfiguredHostsInTheCluster", String.valueOf(basicClusterConfigData.notConfiguredHosts)) : Utils.getLocalizedString("vsan.hci.gettingStarted.addHostsCard.hostsInTheClusterByType", String.valueOf(basicClusterConfigData.hosts), String.valueOf(basicClusterConfigData.notConfiguredHosts));
      } else {
         return Utils.getLocalizedString("vsan.hci.gettingStarted.addHostsCard.hostsInTheCluster", String.valueOf(basicClusterConfigData.hosts));
      }
   }

   private ConfigCardData getConfigureCard(BasicClusterConfigData basicClusterConfigData, boolean hasEditClusterPermission, ManagedObjectReference clusterRef) {
      ConfigCardData result = this.getBasicConfigureCard();
      result.contentText = this.getConfigureCardContentText(basicClusterConfigData);
      result.operationInProgress = this.isConfigureOperationInProgress(clusterRef);
      result.enabled = !result.operationInProgress && this.isConfigureCardEnabled(basicClusterConfigData, hasEditClusterPermission);
      result.nextStep = result.enabled;
      result.contentHeader = this.getNotConfiguredHostsLabel(basicClusterConfigData);
      result.status = ConfigCardData.Status.NOT_AVAILABLE;
      result.validateEnabled = basicClusterConfigData.hosts > 0 && basicClusterConfigData.notConfiguredHosts == 0 && basicClusterConfigData.hciWorkflowState != HciWorkflowState.NOT_IN_HCI_WORKFLOW;
      if (basicClusterConfigData.hciWorkflowState.equals(HciWorkflowState.DONE)) {
         result.title = Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.titleInExtend");
      }

      return result;
   }

   private ConfigCardData getBasicConfigureCard() {
      return new ConfigCardData(Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.title"), "com.vmware.vsan.client.h5vsanui.cluster.configureHciCluster", true, false, Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.launchButton.text"));
   }

   private String getConfigureCardContentText(BasicClusterConfigData basicClusterConfigData) {
      if (basicClusterConfigData.drsEnabled && basicClusterConfigData.vsanEnabled) {
         return Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.contentText.vMotionVsanTraffic");
      } else if (basicClusterConfigData.drsEnabled) {
         return Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.contentText.vMotionTraffic");
      } else {
         return basicClusterConfigData.vsanEnabled ? Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.contentText.vSanTraffic") : Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.contentText.default");
      }
   }

   private boolean hasAddHostsPermissions(ManagedObjectReference clusterRef) {
      try {
         ManagedObjectReference hostFolder = (ManagedObjectReference)QueryUtil.getPropertyForRelatedObjects(clusterRef, "dc", ClusterComputeResource.class.getSimpleName(), "hostFolder").getPropertyValues()[0].value;
         boolean hasHostPermissions = this.permissionService.hasPermissions(hostFolder, new String[]{"Host.Inventory.AddStandaloneHost", "Host.Inventory.MoveHost"});
         return hasHostPermissions;
      } catch (Exception var4) {
         logger.error("Unable to determine host folder permissions, assuming granted", var4);
         return true;
      }
   }

   private List getEnabledServices(BasicClusterConfigData basicClusterConfigData) {
      List result = new ArrayList();
      if (basicClusterConfigData.drsEnabled) {
         result.add(Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.services.drs"));
      }

      if (basicClusterConfigData.haEnabled) {
         result.add(Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.services.ha"));
      }

      if (basicClusterConfigData.vsanEnabled) {
         result.add(Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.services.vsan"));
      }

      if (basicClusterConfigData.isComputeOnlyCluster) {
         result.add(Utils.getLocalizedString("vsan.hci.gettingStarted.basicConfigCard.services.vsanComputeOnly"));
      }

      return result;
   }

   private boolean isConfigureCardEnabled(BasicClusterConfigData basicClusterConfigData, boolean hasPermission) {
      return basicClusterConfigData.notConfiguredHosts > 0 && basicClusterConfigData.hciWorkflowState != HciWorkflowState.INVALID && hasPermission;
   }

   private String getNotConfiguredHostsLabel(BasicClusterConfigData configData) {
      if (configData.hciWorkflowState == HciWorkflowState.DONE && configData.notConfiguredHosts > 0) {
         return configData.notConfiguredHosts == 1 ? Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.notConfiguredHostText") : Utils.getLocalizedString("vsan.hci.gettingStarted.configureServicesCard.notConfiguredHostsText", String.valueOf(configData.notConfiguredHosts));
      } else {
         return null;
      }
   }

   @TsService
   public boolean isConfigureOperationInProgress(ManagedObjectReference clusterRef) {
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      try {
         AuthorizationManager authorizationManager = (AuthorizationManager)vcConnection.createStub(AuthorizationManager.class, vcConnection.getContent().getAuthorizationManager());
         Measure measure = new Measure("AuthorizationManager.queryDisabledMethods");
         Throwable var7 = null;

         DisabledMethodInfo[] disabledMethods;
         try {
            disabledMethods = authorizationManager.queryDisabledMethods(clusterRef);
         } catch (Throwable var33) {
            var7 = var33;
            throw var33;
         } finally {
            if (measure != null) {
               if (var7 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var32) {
                     var7.addSuppressed(var32);
                  }
               } else {
                  measure.close();
               }
            }

         }

         boolean var38;
         if (ArrayUtils.isEmpty(disabledMethods)) {
            var38 = false;
            return var38;
         } else {
            DisabledMethodInfo[] var37 = disabledMethods;
            int var39 = disabledMethods.length;

            for(int var8 = 0; var8 < var39; ++var8) {
               DisabledMethodInfo info = var37[var8];
               if (info.method != null && StringUtils.isNotEmpty(info.method.getName()) && ("configureHCI".equals(info.method.getName()) || "extendHCI".equals(info.method.getName()))) {
                  boolean var10 = true;
                  return var10;
               }
            }

            var38 = false;
            return var38;
         }
      } catch (Throwable var35) {
         var3 = var35;
         throw var35;
      } finally {
         if (vcConnection != null) {
            if (var3 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var31) {
                  var3.addSuppressed(var31);
               }
            } else {
               vcConnection.close();
            }
         }

      }
   }

   @TsService
   public QuickstartStatusData getHciDataWithStatus(ManagedObjectReference clusterRef) {
      return this.getHciData(clusterRef, (Boolean)null);
   }

   @TsService
   public QuickstartStatusData getHciData(ManagedObjectReference moRef, Boolean isVsanEnabled) {
      QuickstartStatusData data = new QuickstartStatusData();
      ManagedObjectReference clusterRef = BaseUtils.getCluster(moRef);
      data.clusterRef = clusterRef;
      if (BooleanUtils.isFalse(isVsanEnabled)) {
         data.isVsanEnabled = false;
         return data;
      } else {
         data.clusterData = this.hciClusterService.getBasicClusterConfigData(clusterRef);
         data.isVsanEnabled = data.clusterData.vsanEnabled;
         return data;
      }
   }
}
