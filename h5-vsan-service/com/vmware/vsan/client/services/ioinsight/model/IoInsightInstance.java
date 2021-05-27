package com.vmware.vsan.client.services.ioinsight.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIoInsightInstance;
import com.vmware.vim.vsan.binding.vim.host.VsanHostIoInsightInfo;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.FormatUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class IoInsightInstance {
   public String name;
   public IoInsightRunningState state;
   public Date startTime;
   public Date endTime;
   public String duration;
   public long durationInMinutes;
   public long remainingTimeInMinutes;
   public String remainingTimeLocalizedTimeUnit;
   @ProxygenSerializer.ElementType(HostIoInsightInfo.class)
   public List hostIoInsightInfos = new ArrayList();
   public List vmUuids = new ArrayList();

   public String toString() {
      return "IoInsight name: " + this.name;
   }

   public static IoInsightInstance create(VsanIoInsightInstance vsanIoInsightInstance, DataServiceResponse hostProperties) {
      IoInsightInstance instance = new IoInsightInstance();
      instance.name = vsanIoInsightInstance.runName;
      instance.state = IoInsightRunningState.fromString(vsanIoInsightInstance.state);
      instance.startTime = vsanIoInsightInstance.startTime.getTime();
      instance.endTime = vsanIoInsightInstance.endTime.getTime();
      long durationInSeconds = (instance.endTime.getTime() - instance.startTime.getTime()) / 1000L;
      long durationInMinutes = Math.round((double)durationInSeconds / 60.0D);
      instance.duration = FormatUtil.parseSecondsToLocalizedTimeUnit(durationInMinutes * 60L);
      instance.durationInMinutes = durationInMinutes;
      if (instance.state == IoInsightRunningState.RUNNING) {
         long remainingTimeInSeconds = Math.max(FormatUtil.getSecondsFromNow(instance.endTime.getTime()), 0L);
         instance.remainingTimeLocalizedTimeUnit = FormatUtil.parseSecondsToLocalizedTimeUnit(remainingTimeInSeconds);
         instance.remainingTimeInMinutes = remainingTimeInSeconds / 60L;
      }

      if (!ArrayUtils.isEmpty(vsanIoInsightInstance.hostUuids) && hostProperties != null && !MapUtils.isEmpty(hostProperties.getMap())) {
         Set hostUuids = new HashSet(Arrays.asList(vsanIoInsightInstance.hostUuids));
         Stream var10000 = hostProperties.getMap().entrySet().stream().filter((entry) -> {
            return hostUuids.contains(((Map)entry.getValue()).get("config.vsanHostConfig.clusterInfo.nodeUuid"));
         }).map((entry) -> {
            return createHostIoInsightInfo(vsanIoInsightInstance, (String)((Map)entry.getValue()).get("name"), (String)((Map)entry.getValue()).get("primaryIconId"), (ManagedObjectReference)entry.getKey());
         });
         List var10001 = instance.hostIoInsightInfos;
         var10000.forEach(var10001::add);
         if (!ArrayUtils.isEmpty(vsanIoInsightInstance.vmUuids)) {
            instance.vmUuids = Arrays.asList(vsanIoInsightInstance.vmUuids);
         }

         return instance;
      } else {
         return instance;
      }
   }

   private static HostIoInsightInfo createHostIoInsightInfo(VsanIoInsightInstance vsanIoInsightInstance, String hostName, String hostIcon, ManagedObjectReference hostRef) {
      HostIoInsightInfo hostInfo = new HostIoInsightInfo();
      hostInfo.host = new InventoryNode(hostRef, hostName, hostIcon);
      if (ArrayUtils.isNotEmpty(vsanIoInsightInstance.hostsIoInsightInfo)) {
         VsanHostIoInsightInfo vsanHostIoInsightInfo = (VsanHostIoInsightInfo)Arrays.stream(vsanIoInsightInstance.hostsIoInsightInfo).filter((info) -> {
            return areManagedObjectsEquals(hostRef, info.host);
         }).findFirst().get();
         hostInfo.faultMessage = vsanHostIoInsightInfo.faultMessage;
         if (vsanHostIoInsightInfo.ioinsightInfo != null && vsanHostIoInsightInfo.ioinsightInfo.monitoredVMs != null) {
            hostInfo.monitoredVms = (List)Arrays.stream(vsanHostIoInsightInfo.ioinsightInfo.monitoredVMs).map((vm) -> {
               return VmodlHelper.assignServerGuid(vm, hostRef.getServerGuid());
            }).collect(Collectors.toList());
         }
      }

      return hostInfo;
   }

   private static boolean areManagedObjectsEquals(ManagedObjectReference obj1, ManagedObjectReference obj2) {
      return areEqual(obj1.getType(), obj2.getType()) && areEqual(obj1.getValue(), obj2.getValue());
   }

   private static boolean areEqual(String string1, String string2) {
      return string1 == null ? string2 == null : string1.equals(string2);
   }
}
