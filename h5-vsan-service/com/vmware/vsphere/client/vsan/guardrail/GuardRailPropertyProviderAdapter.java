package com.vmware.vsphere.client.vsan.guardrail;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanSyncingObjectQueryResult;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.services.diskmanagement.DiskManagementService;
import com.vmware.vsan.client.services.resyncing.VsanResyncingComponentsProvider;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.FormatUtil;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GuardRailPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final String VSAN_GUARD_RAIL_RESULT = "clusterGuardRailResult";
   private static final String VSAN_GUARD_RAIL_MESSAGES = "clusterGuardRailMessages";
   public static final String HOST_NAME_SEPARATOR = ", ";
   private static final String[] HOST_PROPERTIES = new String[]{"name", "runtime.connectionState", "runtime.inMaintenanceMode"};
   private static final Log _logger = LogFactory.getLog(GuardRailPropertyProviderAdapter.class);
   @Autowired
   public VsanResyncingComponentsProvider resyncingComponentsProvider;
   @Autowired
   private DiskManagementService diskMgmtService;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;

   public GuardRailPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo clusterInfo = new TypeInfo();
      clusterInfo.type = ClusterComputeResource.class.getSimpleName();
      clusterInfo.properties = new String[]{"clusterGuardRailResult", "clusterGuardRailMessages"};
      TypeInfo[] providedProperties = new TypeInfo[]{clusterInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      ArrayList resultItems = new ArrayList();

      try {
         String[] propertyNames = QueryUtil.getPropertyNames(propertyRequest.properties);
         Object[] var19 = propertyRequest.objects;
         int var5 = var19.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Object obj = var19[var6];
            ManagedObjectReference clusterRef = (ManagedObjectReference)obj;
            List propertyValues = new ArrayList();
            GuardRailResult guardRailResult = this.getGuardRailResult(clusterRef);
            String[] var11 = propertyNames;
            int var12 = propertyNames.length;

            for(int var13 = 0; var13 < var12; ++var13) {
               String property = var11[var13];
               PropertyValue propertyValue = null;
               if ("clusterGuardRailMessages".equals(property)) {
                  Map guardRailMessages = this.convertToGuardRailMessages(guardRailResult);
                  propertyValue = QueryUtil.newProperty("clusterGuardRailMessages", guardRailMessages);
               } else if ("clusterGuardRailResult".equals(property)) {
                  propertyValue = QueryUtil.newProperty("clusterGuardRailResult", guardRailResult);
               } else {
                  _logger.warn("Unknown property: " + property);
               }

               if (propertyValue != null) {
                  propertyValues.add(propertyValue);
               }
            }

            ResultItem resultItem = new ResultItem();
            resultItem.properties = (PropertyValue[])propertyValues.toArray(new PropertyValue[propertyValues.size()]);
            resultItem.resourceObject = clusterRef;
            resultItems.add(resultItem);
         }
      } catch (Exception var17) {
         _logger.error("Failed to retrieve ClusterGuardRailResult property. ", var17);
         ResultSet resultSet = new ResultSet();
         resultSet.error = new Exception(Utils.getLocalizedString("vsan.guardRail.providerGeneralError"));
         return resultSet;
      }

      ResultSet result = new ResultSet();
      result.items = (ResultItem[])resultItems.toArray(new ResultItem[resultItems.size()]);
      result.totalMatchedObjectCount = resultItems.size();
      return result;
   }

   private Map convertToGuardRailMessages(GuardRailResult guardRailResult) {
      Map messages = new HashMap();
      List warningMessages = new ArrayList();
      List infoMessages = new ArrayList();
      String hostNames;
      if (ArrayUtils.isNotEmpty(guardRailResult.hostsInMaintenanceMode)) {
         hostNames = StringUtils.join(guardRailResult.hostsInMaintenanceMode, ", ");
         warningMessages.add(Utils.getLocalizedString("vsan.guardRail.hostInMaintenanceMode", hostNames));
      }

      if (ArrayUtils.isNotEmpty(guardRailResult.hostsNotConnected)) {
         hostNames = StringUtils.join(guardRailResult.hostsNotConnected, ", ");
         warningMessages.add(Utils.getLocalizedString("vsan.guardRail.hostsNotConnected", hostNames));
      }

      boolean areObjectsResyncing = guardRailResult.isClusterInResync && guardRailResult.objectsToSyncCount != null && guardRailResult.objectsToSyncCount > 0L;
      if (areObjectsResyncing) {
         String message = Utils.getLocalizedString("vsan.guardRail.clusterInResync", String.valueOf(guardRailResult.objectsToSyncCount));
         warningMessages.add(message);
      } else if (guardRailResult.repairTimerData.objectsCount > 0L) {
         long minutesToRepair = FormatUtil.getMinutesFromNow(guardRailResult.repairTimerData.minTimer);
         String message = minutesToRepair <= 1L ? Utils.getLocalizedString("vsan.guardRail.scheduledResync.oneMinute") : Utils.getLocalizedString("vsan.guardRail.scheduledResync", String.valueOf(minutesToRepair));
         warningMessages.add(message);
      }

      if (guardRailResult.hasNetworkPartitioning) {
         warningMessages.add(Utils.getLocalizedString("vsan.guardRail.networkPartitioning"));
      }

      if (!guardRailResult.isAutomaticRebalanceSupported && !guardRailResult.resyncCollected) {
         infoMessages.add(Utils.getLocalizedString("vsan.guardRail.legacyClusterInResync"));
      }

      messages.put(GuardRailMessageStatus.WARNING, warningMessages);
      messages.put(GuardRailMessageStatus.INFO, infoMessages);
      return messages;
   }

   private GuardRailResult getGuardRailResult(ManagedObjectReference clusterRef) throws Exception {
      Measure measure = new Measure("Retrieving guard rail data");
      Throwable var3 = null;

      GuardRailResult var27;
      try {
         VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadWitnessHosts();
         DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "allVsanHosts", ClusterComputeResource.class.getSimpleName(), HOST_PROPERTIES);
         Set hostsInMM = new TreeSet(String.CASE_INSENSITIVE_ORDER);
         Set hostsNotConnected = new TreeSet(String.CASE_INSENSITIVE_ORDER);
         Set hostsConnected = new HashSet();
         boolean isAutomaticRebalanceSupported = VsanCapabilityUtils.isAutomaticRebalanceSupported(clusterRef);
         Future vsanSyncingObjectQueryResultFuture = null;
         Iterator var11 = response.getResourceObjects().iterator();

         while(var11.hasNext()) {
            Object obj = var11.next();
            ManagedObjectReference hostRef = (ManagedObjectReference)obj;
            boolean isInMaintenanceMode = (Boolean)response.getProperty(hostRef, "runtime.inMaintenanceMode");
            ConnectionState connectionState = (ConnectionState)response.getProperty(hostRef, "runtime.connectionState");
            boolean isConnected = ConnectionState.connected == connectionState;
            if (isConnected) {
               hostsConnected.add(hostRef);
            } else {
               hostsNotConnected.add((String)response.getProperty(hostRef, "name"));
            }

            if (isInMaintenanceMode) {
               hostsInMM.add((String)response.getProperty(hostRef, "name"));
            } else if (!isAutomaticRebalanceSupported && vsanSyncingObjectQueryResultFuture == null && VsanCapabilityUtils.isResyncEnhancedApiSupported(hostRef) && isConnected) {
               vsanSyncingObjectQueryResultFuture = this.querySyncingVsanObjects(measure, hostRef);
            }
         }

         Future[] repairTimerDataFutures = null;
         if (!isAutomaticRebalanceSupported) {
            repairTimerDataFutures = this.resyncingComponentsProvider.getRepairTimerDataFutures(clusterRef, measure);
         }

         var27 = this.getGuardRailResultProperties(clusterRef, hostsConnected, hostsInMM, hostsNotConnected, vsanSyncingObjectQueryResultFuture, repairTimerDataFutures, dataRetriever, isAutomaticRebalanceSupported);
      } catch (Throwable var24) {
         var3 = var24;
         throw var24;
      } finally {
         if (measure != null) {
            if (var3 != null) {
               try {
                  measure.close();
               } catch (Throwable var23) {
                  var3.addSuppressed(var23);
               }
            } else {
               measure.close();
            }
         }

      }

      return var27;
   }

   private Future querySyncingVsanObjects(Measure measure, ManagedObjectReference hostRef) {
      Future future = null;
      VsanConnection connection = this.vsanClient.getConnection(hostRef.getServerGuid());
      Throwable var5 = null;

      Future var7;
      try {
         VsanSystemEx vsanSystemEx = connection.getVsanSystemEx(hostRef);
         if (vsanSystemEx != null) {
            future = measure.newFuture("vsanSystemEx.querySyncingVsanObjects");
            vsanSystemEx.querySyncingVsanObjects((String[])null, 0, 1, true, future);
         }

         var7 = future;
      } catch (Throwable var16) {
         var5 = var16;
         throw var16;
      } finally {
         if (connection != null) {
            if (var5 != null) {
               try {
                  connection.close();
               } catch (Throwable var15) {
                  var5.addSuppressed(var15);
               }
            } else {
               connection.close();
            }
         }

      }

      return var7;
   }

   private GuardRailResult getGuardRailResultProperties(ManagedObjectReference clusterRef, Set hostsConnected, Set hostsInMM, Set hostsNotConnected, Future vsanSyncingObjectFuture, Future[] repairTimerDataFutures, VsanAsyncDataRetriever dataRetriever, boolean isAutomaticRebalanceSupported) throws ExecutionException, InterruptedException {
      GuardRailResult guardRailResult = new GuardRailResult();
      if (CollectionUtils.isNotEmpty(hostsInMM)) {
         guardRailResult.hostsInMaintenanceMode = (String[])hostsInMM.toArray(new String[0]);
      }

      if (CollectionUtils.isNotEmpty(hostsNotConnected)) {
         guardRailResult.hostsNotConnected = (String[])hostsNotConnected.toArray(new String[0]);
      }

      guardRailResult.hasNetworkPartitioning = this.diskMgmtService.hasNetworkPartition(clusterRef, dataRetriever, hostsConnected);
      guardRailResult.repairTimerData = this.resyncingComponentsProvider.getRepairTimerData(repairTimerDataFutures);
      if (isAutomaticRebalanceSupported) {
         guardRailResult.isAutomaticRebalanceSupported = true;
         return guardRailResult;
      } else {
         guardRailResult.resyncCollected = vsanSyncingObjectFuture != null;
         if (guardRailResult.resyncCollected) {
            VsanSyncingObjectQueryResult vsanSyncingObject = (VsanSyncingObjectQueryResult)vsanSyncingObjectFuture.get();
            if (ArrayUtils.isNotEmpty(vsanSyncingObject.objects)) {
               guardRailResult.isClusterInResync = true;
               guardRailResult.recoveryETA = vsanSyncingObject.totalRecoveryETA;
               guardRailResult.objectsToSyncCount = vsanSyncingObject.totalObjectsToSync;
            }
         }

         return guardRailResult;
      }
   }
}
