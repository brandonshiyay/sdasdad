package com.vmware.vsan.client.services.capacity;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSpaceUsageDetailResult;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capacity.model.ReducedCapacityData;
import com.vmware.vsphere.client.vsan.base.util.BaseUtils;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ReducedCapacityMessagesService {
   @TsService
   public String[] getReducedCapacityMessagesForHistory(ManagedObjectReference objectRef) {
      return this.getReducedCapacityMessages(BaseUtils.getCluster(objectRef), (VsanSpaceUsageDetailResult)null, false).reducedCapacityMessages;
   }

   public ReducedCapacityData getReducedCapacityMessages(ManagedObjectReference clusterRef, VsanSpaceUsageDetailResult spaceDetail, boolean considerSpaceDetail) {
      DataServiceResponse hostPropertiesResponse;
      try {
         hostPropertiesResponse = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"name", "config.vsanHostConfig.enabled", "runtime.connectionState", "runtime.inMaintenanceMode"});
      } catch (Exception var9) {
         throw new VsanUiLocalizableException("vsan.cluster.monitor.capacity.hostProperties.error");
      }

      if (hostPropertiesResponse != null && !ArrayUtils.isEmpty(hostPropertiesResponse.getPropertyValues())) {
         List hostsInMM = new ArrayList();
         List disconnectedHosts = new ArrayList();
         List hostsNotInVsanCluster = new ArrayList();
         this.processHosts(hostPropertiesResponse, hostsInMM, disconnectedHosts, hostsNotInVsanCluster);
         int faultyHostsTotalCount = hostsInMM.size() + disconnectedHosts.size() + hostsNotInVsanCluster.size();
         if (faultyHostsTotalCount == hostPropertiesResponse.getResourceObjects().size()) {
            return new ReducedCapacityData(new String[]{Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.noEligibleHosts")}, false);
         } else if (considerSpaceDetail && spaceDetail == null) {
            return new ReducedCapacityData(new String[]{Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.noDisks")}, true);
         } else {
            return faultyHostsTotalCount == 0 ? new ReducedCapacityData((String[])null, true) : this.getReducedCapacityMessages(hostsInMM, disconnectedHosts, hostsNotInVsanCluster);
         }
      } else {
         return new ReducedCapacityData(new String[]{Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.noHosts")}, false);
      }
   }

   private void processHosts(DataServiceResponse hostProperties, List hostsInMM, List hostsDisconnected, List hostsNotInVsanCluster) {
      Iterator var5 = hostProperties.getResourceObjects().iterator();

      while(var5.hasNext()) {
         Object obj = var5.next();
         ManagedObjectReference hostRef = (ManagedObjectReference)obj;
         String hostName = (String)hostProperties.getProperty(hostRef, "name");
         ConnectionState connectionState = (ConnectionState)hostProperties.getProperty(hostRef, "runtime.connectionState");
         Boolean isVsanEnabled = (Boolean)hostProperties.getProperty(hostRef, "config.vsanHostConfig.enabled");
         Boolean isInMaintenanceMode = (Boolean)hostProperties.getProperty(hostRef, "runtime.inMaintenanceMode");
         if (!com.vmware.vsan.client.services.common.data.ConnectionState.fromHostState(connectionState).equals(com.vmware.vsan.client.services.common.data.ConnectionState.connected)) {
            hostsDisconnected.add(hostName);
         } else if (!Boolean.TRUE.equals(isVsanEnabled)) {
            hostsNotInVsanCluster.add(hostName);
         } else if (Boolean.TRUE.equals(isInMaintenanceMode)) {
            hostsInMM.add(hostName);
         }
      }

   }

   private ReducedCapacityData getReducedCapacityMessages(List hostsInMM, List hostsDisconnected, List hostsNotInVsanCluster) {
      List reducedCapacityMessages = new ArrayList();
      reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.partialCapacity"));
      String hostsNotInVsanMessage;
      if (hostsInMM.size() == 1) {
         reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.hostInMM", (String)hostsInMM.get(0)));
      } else if (hostsInMM.size() > 1) {
         hostsNotInVsanMessage = StringUtils.join(hostsInMM, ",");
         reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.hostsInMM", hostsNotInVsanMessage));
      }

      if (hostsDisconnected.size() == 1) {
         reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.hostDisconnected", (String)hostsDisconnected.get(0)));
      } else if (hostsDisconnected.size() > 1) {
         hostsNotInVsanMessage = StringUtils.join(hostsDisconnected, ",");
         reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.hostsDisconnected", hostsNotInVsanMessage));
      }

      if (hostsNotInVsanCluster.size() == 1) {
         reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.hostNotInVsanCluster", (String)hostsNotInVsanCluster.get(0)));
      } else if (hostsNotInVsanCluster.size() > 1) {
         hostsNotInVsanMessage = StringUtils.join(hostsNotInVsanCluster, ",");
         reducedCapacityMessages.add(Utils.getLocalizedString("vsan.cluster.monitor.capacity.reducedCapacityMessage.hostsNotInVsanCluster", hostsNotInVsanMessage));
      }

      return new ReducedCapacityData((String[])reducedCapacityMessages.toArray(new String[0]), true);
   }
}
