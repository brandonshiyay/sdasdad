package com.vmware.vsphere.client.vsan.perf;

import com.vmware.vim.binding.vim.host.OpaqueNetworkInfo;
import com.vmware.vim.binding.vim.host.OpaqueSwitch;
import com.vmware.vim.binding.vim.host.PhysicalNic;
import com.vmware.vim.binding.vim.host.VirtualNic;
import com.vmware.vim.binding.vim.host.OpaqueSwitch.PhysicalNicZone;
import com.vmware.vim.binding.vim.vsan.host.ConfigInfo.NetworkInfo.PortConfig;
import com.vmware.vsphere.client.vsan.perf.model.PerfVnicEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class NetworkUtils {
   public static boolean isStandardModeVnic(VirtualNic vnic) {
      return !StringUtils.isEmpty(vnic.port);
   }

   public static boolean isDistributedSwitchModeVnic(VirtualNic vnic) {
      return vnic.spec != null && vnic.spec.distributedVirtualPort != null && !StringUtils.isEmpty(vnic.spec.distributedVirtualPort.switchUuid);
   }

   public static boolean isNsxOpaqueSwitchModeVnic(VirtualNic vnic) {
      return vnic.spec != null && vnic.spec.opaqueNetwork != null;
   }

   public static List getVsanUsedVnics(PortConfig[] portConfigs, VirtualNic[] configuredVnics) {
      if (!ArrayUtils.isEmpty(portConfigs) && !ArrayUtils.isEmpty(configuredVnics)) {
         List allActiveVnics = (List)Stream.of(portConfigs).map((portConfig) -> {
            return portConfig.getDevice();
         }).collect(Collectors.toList());
         return (List)Stream.of(configuredVnics).filter((vnic) -> {
            return vnic.device != null;
         }).filter((vnic) -> {
            return allActiveVnics.contains(vnic.device);
         }).collect(Collectors.toList());
      } else {
         return Collections.EMPTY_LIST;
      }
   }

   public static List getVsanUsedVnicEntities(PortConfig[] portConfigs, VirtualNic[] configuredVnics, String hostUuid) {
      return (List)getVsanUsedVnics(portConfigs, configuredVnics).stream().map((vnic) -> {
         return createPerfVnicEntity(hostUuid, vnic);
      }).collect(Collectors.toList());
   }

   private static PerfVnicEntity createPerfVnicEntity(String hostUuid, VirtualNic vnic) {
      PerfVnicEntity vnicEntity = new PerfVnicEntity();
      vnicEntity.deviceName = vnic.device;
      vnicEntity.netStackInstanceKey = vnic.getSpec().netStackInstanceKey;
      vnicEntity.hostUuid = hostUuid;
      return vnicEntity;
   }

   public static OpaqueNetworkInfo getOpaqueNetwork(String networkId, String networkType, OpaqueNetworkInfo[] networkInfos) {
      return !StringUtils.isEmpty(networkId) && !StringUtils.isEmpty(networkType) && !ArrayUtils.isEmpty(networkInfos) ? (OpaqueNetworkInfo)Stream.of(networkInfos).filter((info) -> {
         return networkId.equals(info.opaqueNetworkId);
      }).filter((info) -> {
         return networkType.equals(info.opaqueNetworkType);
      }).findFirst().orElse((Object)null) : null;
   }

   public static List getOpaqueSwitchesAttachedToOpaqueNetwork(OpaqueNetworkInfo opaqueNetwork, OpaqueSwitch[] opaqueSwitches) {
      if (opaqueNetwork != null && !ArrayUtils.isEmpty(opaqueNetwork.pnicZone) && !ArrayUtils.isEmpty(opaqueSwitches)) {
         String[] opaqueNetworkZoneIds = opaqueNetwork.pnicZone;
         List result = new ArrayList();
         OpaqueSwitch[] var4 = opaqueSwitches;
         int var5 = opaqueSwitches.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            OpaqueSwitch opaqueSwitch = var4[var6];
            if (checkIfNetworkZonesMatchSwitchZones(opaqueNetworkZoneIds, opaqueSwitch.pnicZone)) {
               result.add(opaqueSwitch);
            }
         }

         return result;
      } else {
         return Collections.EMPTY_LIST;
      }
   }

   private static boolean checkIfNetworkZonesMatchSwitchZones(String[] opaqueNetworkZoneIds, PhysicalNicZone[] opaqueSwitchZones) {
      if (!ArrayUtils.isEmpty(opaqueSwitchZones) && !ArrayUtils.isEmpty(opaqueNetworkZoneIds)) {
         String[] var2 = opaqueNetworkZoneIds;
         int var3 = opaqueNetworkZoneIds.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String opaqueNetworkZoneId = var2[var4];
            boolean found = Stream.of(opaqueSwitchZones).anyMatch((switchZone) -> {
               return opaqueNetworkZoneId.equals(switchZone.key);
            });
            if (!found) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static Set getPhysicalNicNamesFromOpaqueSwitches(PhysicalNic[] physicalNics, List switches) {
      return (Set)switches.stream().filter((opaqueSwitch) -> {
         return !ArrayUtils.isEmpty(opaqueSwitch.pnic);
      }).flatMap((opaqueSwitch) -> {
         return Stream.of(opaqueSwitch.pnic);
      }).map((pnic) -> {
         return getPnicNameFromKey(pnic, physicalNics);
      }).collect(Collectors.toSet());
   }

   private static String getPnicNameFromKey(String pnicKey, PhysicalNic[] physicalNics) {
      return !StringUtils.isEmpty(pnicKey) && !ArrayUtils.isEmpty(physicalNics) ? (String)Stream.of(physicalNics).filter((pnic) -> {
         return pnicKey.equals(pnic.key);
      }).map((pnic) -> {
         return pnic.device;
      }).findFirst().orElse("") : "";
   }
}
