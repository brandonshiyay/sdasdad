package com.vmware.vsan.client.services.hardware;

import com.vmware.vim.binding.vim.host.PlugStoreTopology.Adapter;
import com.vmware.vim.binding.vim.host.PlugStoreTopology.Target;
import com.vmware.vim.binding.vim.vsan.host.DiskResult;
import com.vmware.vim.vsan.binding.vim.host.VsanHardwareCommonInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHclCommonDeviceInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmBootDeviceInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmChassisInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmDriveInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmMemoryInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmNetworkPortInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmNicInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmPhysicalMemInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmProcessorInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHcmStorageControllerInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanStorageEnclosureInfo;
import com.vmware.vsan.client.services.diskmanagement.DiskData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtBootDeviceData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtCommonData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtDeviceData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtDiskBox;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtDiskData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtDiskSlot;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtHealthStatus;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtMemoryData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtNetworkPortData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtNicData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtOverviewData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtPhysicalMemoryData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtProcessorData;
import com.vmware.vsan.client.services.hardware.model.HardwareMgmtStorageControllerData;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class HardwareMgmtUtil {
   private static final String DISK_LOCATION_SEPARATOR = ":";
   private static final int DISK_LOCATION_CHUNKS_BOX_POSITION_INDEX = 0;
   private static final int DISK_LOCATION_CHUNKS_DISK_POSITION_INDEX = 1;
   private static final int DISK_SLOT_ZERO_INDEX = 0;
   private static final String HEX_FORMAT = "0x%04X";
   static final String DUMMY_BOX_ID = Integer.toString(Integer.MIN_VALUE);

   public static HardwareMgmtOverviewData createOverviewData(VsanHardwareCommonInfo chassisInfo, VsanHcmBootDeviceInfo bootDeviceInfo) {
      if (chassisInfo == null && bootDeviceInfo == null) {
         return null;
      } else {
         HardwareMgmtOverviewData result = new HardwareMgmtOverviewData();
         result.common = buildHardwareCommonData(chassisInfo);
         result.bootDevice = buildBootDevice(bootDeviceInfo);
         return result;
      }
   }

   public static HardwareMgmtDiskBox[] createDiskBoxes(VsanHcmChassisInfo chassisInfo, VsanHcmStorageControllerInfo[] controllerInfos, DiskResult[] additionalDisksData) {
      Map boxesById = buildDiskBoxesByIds(chassisInfo);
      if (ArrayUtils.isEmpty(controllerInfos)) {
         return (HardwareMgmtDiskBox[])boxesById.values().toArray(new HardwareMgmtDiskBox[0]);
      } else {
         Map disksDataByDisplayName = getDiskDataByDisplayName(additionalDisksData);
         Arrays.stream(controllerInfos).forEach((controllerInfo) -> {
            buildControllerWithDisks(controllerInfo, boxesById, disksDataByDisplayName);
         });
         boxesById.values().forEach(HardwareMgmtUtil::fixDiskSlots);
         return (HardwareMgmtDiskBox[])boxesById.values().toArray(new HardwareMgmtDiskBox[0]);
      }
   }

   public static HardwareMgmtNicData[] createNics(VsanHcmNicInfo[] pnics) {
      if (ArrayUtils.isEmpty(pnics)) {
         return new HardwareMgmtNicData[0];
      } else {
         List result = new LinkedList();

         for(int i = 0; i < pnics.length; ++i) {
            if (pnics[i] != null) {
               result.add(buildNicData(pnics[i], i + 1));
            }
         }

         return (HardwareMgmtNicData[])result.toArray(new HardwareMgmtNicData[0]);
      }
   }

   public static HardwareMgmtProcessorData[] createProcessors(VsanHcmProcessorInfo[] processors) {
      return ArrayUtils.isEmpty(processors) ? new HardwareMgmtProcessorData[0] : (HardwareMgmtProcessorData[])Arrays.stream(processors).filter(Objects::nonNull).map(HardwareMgmtUtil::buildProcessorData).toArray((x$0) -> {
         return new HardwareMgmtProcessorData[x$0];
      });
   }

   public static HardwareMgmtMemoryData[] createMemoryBoards(VsanHcmMemoryInfo[] memoryInfos) {
      return ArrayUtils.isEmpty(memoryInfos) ? new HardwareMgmtMemoryData[0] : (HardwareMgmtMemoryData[])Arrays.stream(memoryInfos).filter(Objects::nonNull).map(HardwareMgmtUtil::buildMemoryData).toArray((x$0) -> {
         return new HardwareMgmtMemoryData[x$0];
      });
   }

   private static HardwareMgmtCommonData buildHardwareCommonData(VsanHardwareCommonInfo hardwareCommonInfo) {
      if (hardwareCommonInfo == null) {
         return null;
      } else {
         HardwareMgmtCommonData result = new HardwareMgmtCommonData();
         result.vendor = hardwareCommonInfo.getManufacturer();
         result.model = hardwareCommonInfo.getModel();
         result.sku = hardwareCommonInfo.getSku();
         result.serialNumber = hardwareCommonInfo.getSerialNumber();
         result.healthStatus = HardwareMgmtHealthStatus.fromString(hardwareCommonInfo.getHealth());
         result.errorMessage = getErrorMessage(hardwareCommonInfo.getError());
         return result;
      }
   }

   private static HardwareMgmtBootDeviceData buildBootDevice(VsanHcmBootDeviceInfo bootDeviceInfo) {
      if (bootDeviceInfo == null) {
         return null;
      } else {
         HardwareMgmtBootDeviceData result = new HardwareMgmtBootDeviceData();
         result.name = bootDeviceInfo.getBootDevice();
         result.bootMode = bootDeviceInfo.getBootMode();
         result.errorMessage = getErrorMessage(bootDeviceInfo.getError());
         return result;
      }
   }

   private static String getErrorMessage(Exception error) {
      if (error == null) {
         return null;
      } else {
         return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
      }
   }

   private static Map buildDiskBoxesByIds(VsanHcmChassisInfo chassisInfo) {
      Map result = new HashMap();
      VsanStorageEnclosureInfo[] diskBoxInfos = chassisInfo == null ? null : chassisInfo.storageEnclosures;
      return (Map)(ArrayUtils.isEmpty(diskBoxInfos) ? result : (Map)Arrays.stream(diskBoxInfos).filter(Objects::nonNull).map((diskBoxInfo) -> {
         return buildDiskBox(diskBoxInfo, chassisInfo);
      }).collect(Collectors.toMap((box) -> {
         return box.boxId;
      }, (box) -> {
         return box;
      })));
   }

   private static HardwareMgmtDiskBox buildDiskBox(VsanStorageEnclosureInfo diskBoxInfo, VsanHcmChassisInfo chassisInfo) {
      HardwareMgmtDiskBox result = new HardwareMgmtDiskBox();
      result.boxId = Integer.toString(diskBoxInfo.getBoxId());
      result.title = getDiskBoxTitle(chassisInfo, diskBoxInfo.getBoxId());
      result.position = HardwareMgmtDiskBox.Position.fromString(diskBoxInfo.getPosition());
      result.diskSlots = (List)IntStream.range(diskBoxInfo.minSlotIndex, diskBoxInfo.maxSlotIndex + 1).filter((index) -> {
         return ArrayUtils.indexOf(diskBoxInfo.skippedSlotIndexes, index) == -1;
      }).mapToObj((index) -> {
         HardwareMgmtDiskSlot placeholder = new HardwareMgmtDiskSlot();
         placeholder.index = index;
         return placeholder;
      }).collect(Collectors.toList());
      return result;
   }

   private static String getDiskBoxTitle(VsanHcmChassisInfo chassisInfo, int boxId) {
      if (chassisInfo != null && !BooleanUtils.isTrue(chassisInfo.getHideEnclosureName())) {
         String boxIdStr = Integer.toString(boxId);
         return StringUtils.isEmpty(chassisInfo.getEnclosureDisplayName()) ? boxIdStr : Utils.getLocalizedString("vsan.hardware.mgmt.box.title.full", chassisInfo.getEnclosureDisplayName(), boxIdStr);
      } else {
         return null;
      }
   }

   private static Map getDiskDataByDisplayName(DiskResult[] diskResults) {
      Map result = new HashMap();
      return (Map)(ArrayUtils.isEmpty(diskResults) ? result : (Map)Arrays.stream(diskResults).map((diskResult) -> {
         return DiskData.fromScsiDisk(diskResult.disk, (String)null, (Target)null, (Adapter)null, diskResult.state);
      }).filter((diskData) -> {
         return diskData.disk != null;
      }).collect(Collectors.toMap((diskData) -> {
         return diskData.disk.canonicalName;
      }, (diskData) -> {
         return diskData;
      })));
   }

   private static void buildControllerWithDisks(VsanHcmStorageControllerInfo controllerInfo, Map boxesById, Map disksDataByDisplayName) {
      if (controllerInfo != null && !ArrayUtils.isEmpty(controllerInfo.drives)) {
         HardwareMgmtStorageControllerData controller = new HardwareMgmtStorageControllerData();
         controller.operationalMode = controllerInfo.getOperatingMode();
         controller.deviceData = buildDeviceData(controllerInfo);
         controller.common = new HardwareMgmtCommonData();
         controller.common.serialNumber = controllerInfo.getSerialNumber();
         controller.common.model = controllerInfo.getModel();
         controller.common.healthStatus = HardwareMgmtHealthStatus.fromString(controllerInfo.getHealth());
         controller.common.errorMessage = getErrorMessage(controllerInfo.error);
         Arrays.stream(controllerInfo.drives).filter(Objects::nonNull).forEach((drive) -> {
            buildDisk(drive, controller, boxesById, disksDataByDisplayName);
         });
      }
   }

   private static HardwareMgmtDeviceData buildDeviceData(VsanHclCommonDeviceInfo commonDeviceInfo) {
      if (commonDeviceInfo == null) {
         return null;
      } else {
         HardwareMgmtDeviceData result = new HardwareMgmtDeviceData();
         if (commonDeviceInfo.getDeviceId() != null) {
            result.deviceId = commonDeviceInfo.getDeviceId().toString();
         }

         result.deviceName = commonDeviceInfo.getDeviceName();
         result.displayName = commonDeviceInfo.getDisplayName();
         result.driverName = commonDeviceInfo.getDriverName();
         result.driverVersion = commonDeviceInfo.getDriverVersion();
         result.deviceId = toHex(commonDeviceInfo.getDeviceId());
         result.vendorId = toHex(commonDeviceInfo.getVendorId());
         result.subDeviceId = toHex(commonDeviceInfo.getSubDeviceId());
         result.subVendorId = toHex(commonDeviceInfo.getSubVendorId());
         return result;
      }
   }

   private static String toHex(Long longValue) {
      return longValue == null ? null : String.format("0x%04X", longValue);
   }

   private static void buildDisk(VsanHcmDriveInfo driveInfo, HardwareMgmtStorageControllerData controller, Map boxesById, Map disksDataByDisplayName) {
      if (driveInfo != null) {
         HardwareMgmtDiskData disk = new HardwareMgmtDiskData();
         disk.canBeClaimed = !driveInfo.isIneligibleForVsan() && !driveInfo.isUsedByVsan();
         disk.isUsedByVsan = driveInfo.isUsedByVsan();
         disk.isCapacity = driveInfo.isUsedByVsan() ? BooleanUtils.isTrue(driveInfo.isCapacity) : null;
         disk.capacityBytes = driveInfo.capacityBytes;
         disk.common = buildHardwareCommonData(driveInfo);
         disk.controller = controller;
         disk.deviceName = driveInfo.getDeviceName();
         disk.diskGroupUuid = driveInfo.getDiskGroupUuid();
         disk.interfaceType = driveInfo.getInterfaceType();
         disk.isLedOnSupported = driveInfo.isLedOperationSupported();
         disk.isSsd = driveInfo.isIsSsd();
         disk.isLedOn = BooleanUtils.isTrue(driveInfo.indicatorLEDIsOn);
         if (disksDataByDisplayName != null && disksDataByDisplayName.get(disk.deviceName) != null) {
            DiskData additionalDiskData = (DiskData)disksDataByDisplayName.get(disk.deviceName);
            disk.iconId = DiskData.getDiskIcon(additionalDiskData.isDeviceStateHealthy(), additionalDiskData.isFlash);
            disk.uuid = additionalDiskData.uuid;
         }

         String boxId = null;
         Integer diskIndex = null;
         if (StringUtils.isNotBlank(driveInfo.location)) {
            String[] locationChunks = driveInfo.location.split(":");
            boxId = locationChunks[0];

            try {
               diskIndex = Integer.parseInt(locationChunks[1]);
            } catch (Exception var9) {
            }
         }

         HardwareMgmtDiskBox box = (HardwareMgmtDiskBox)boxesById.get(boxId);
         if (box == null) {
            box = (HardwareMgmtDiskBox)boxesById.get(DUMMY_BOX_ID);
         }

         if (box == null) {
            box = new HardwareMgmtDiskBox();
            box.boxId = DUMMY_BOX_ID;
            box.position = HardwareMgmtDiskBox.Position.UNKNOWN;
            boxesById.put(box.boxId, box);
         }

         HardwareMgmtDiskSlot diskSlot = getDiskSlot(box, diskIndex);
         diskSlot.disk = disk;
      }
   }

   private static HardwareMgmtDiskSlot getDiskSlot(HardwareMgmtDiskBox box, Integer diskIndex) {
      Optional slot = box.diskSlots.stream().filter((placeholder) -> {
         return placeholder.index == diskIndex;
      }).findFirst();
      HardwareMgmtDiskSlot slotFound;
      if (!slot.isPresent()) {
         slotFound = new HardwareMgmtDiskSlot();
         slotFound.index = diskIndex;
         box.diskSlots.add(slotFound);
         return slotFound;
      } else {
         slotFound = (HardwareMgmtDiskSlot)slot.get();
         if (slotFound.disk != null) {
            HardwareMgmtDiskSlot duplicatedSlot = new HardwareMgmtDiskSlot();
            box.diskSlots.add(duplicatedSlot);
            return duplicatedSlot;
         } else {
            return slotFound;
         }
      }
   }

   private static void fixDiskSlots(HardwareMgmtDiskBox box) {
      if (!CollectionUtils.isEmpty(box.diskSlots)) {
         int maxSlotIndex = box.diskSlots.stream().filter((slotx) -> {
            return slotx.index != null;
         }).mapToInt((slotx) -> {
            return slotx.index;
         }).reduce(0, Math::max);
         int nextNotUsedSlotIndex = maxSlotIndex + 1;
         Iterator var3 = box.diskSlots.iterator();

         while(var3.hasNext()) {
            HardwareMgmtDiskSlot slot = (HardwareMgmtDiskSlot)var3.next();
            if (slot.index == null) {
               slot.index = nextNotUsedSlotIndex++;
            }
         }

         box.diskSlots.sort((slot1, slot2) -> {
            return slot1.index.compareTo(slot2.index);
         });
      }
   }

   private static HardwareMgmtNicData buildNicData(VsanHcmNicInfo vsanHcmNicInfo, int index) {
      HardwareMgmtNicData result = new HardwareMgmtNicData();
      result.index = index;
      result.deviceData = buildDeviceData(vsanHcmNicInfo);
      result.common = buildHardwareCommonData(vsanHcmNicInfo.getHardwareInfo());
      if (ArrayUtils.isEmpty(vsanHcmNicInfo.physicalPorts)) {
         return result;
      } else {
         for(int i = 0; i < vsanHcmNicInfo.physicalPorts.length; ++i) {
            if (vsanHcmNicInfo.physicalPorts[i] != null) {
               result.ports.add(buildNetworkPortData(vsanHcmNicInfo.physicalPorts[i], i + 1));
            }
         }

         return result;
      }
   }

   private static HardwareMgmtNetworkPortData buildNetworkPortData(VsanHcmNetworkPortInfo vsanHcmNetworkPortInfo, int index) {
      HardwareMgmtNetworkPortData result = new HardwareMgmtNetworkPortData();
      result.index = index;
      result.isUsedByVsan = vsanHcmNetworkPortInfo.isUsedByVsan();
      result.deviceName = vsanHcmNetworkPortInfo.getDeviceName();
      result.linkSpeedMbps = vsanHcmNetworkPortInfo.getLinkSpeedMbps();
      result.linkStatus = HardwareMgmtNetworkPortData.LinkStatus.fromString(vsanHcmNetworkPortInfo.getLinkStatus());
      result.macAddress = vsanHcmNetworkPortInfo.getMacAddress();
      result.healthStatus = HardwareMgmtHealthStatus.fromString(vsanHcmNetworkPortInfo.getHealth());
      result.errorMessage = getErrorMessage(vsanHcmNetworkPortInfo.getError());
      result.vmNics = new ArrayList();
      if (ArrayUtils.isNotEmpty(vsanHcmNetworkPortInfo.getVmknics())) {
         result.vmNics.addAll((Collection)Arrays.stream(vsanHcmNetworkPortInfo.getVmknics()).collect(Collectors.toList()));
      }

      return result;
   }

   private static HardwareMgmtProcessorData buildProcessorData(VsanHcmProcessorInfo vsanHcmProcessorInfo) {
      if (vsanHcmProcessorInfo == null) {
         return null;
      } else {
         HardwareMgmtProcessorData result = new HardwareMgmtProcessorData();
         result.id = vsanHcmProcessorInfo.id;
         result.common = buildHardwareCommonData(vsanHcmProcessorInfo);
         result.speedMHz = vsanHcmProcessorInfo.getSpeedMHz();
         result.totalCores = NumberUtils.toInt(vsanHcmProcessorInfo.getTotalCores());
         result.totalThreads = vsanHcmProcessorInfo.getTotalThreads();
         return result;
      }
   }

   private static HardwareMgmtMemoryData buildMemoryData(VsanHcmMemoryInfo memoryInfo) {
      if (memoryInfo == null) {
         return null;
      } else {
         HardwareMgmtMemoryData result = new HardwareMgmtMemoryData();
         result.boardCpuNumber = NumberUtils.toInt(memoryInfo.getBoardCpuNumber());
         result.operatingFrequencyMhz = NumberUtils.toLong(memoryInfo.getOperatingFrequencyMhz());
         result.slotsCount = NumberUtils.toInt(memoryInfo.getTotalMemSlots());
         result.totalMemoryMb = memoryInfo.getTotalMemoryMb();
         result.memorySlots = buildMemorySlots(memoryInfo.getPhysicalMemInfo());
         return result;
      }
   }

   private static HardwareMgmtPhysicalMemoryData[] buildMemorySlots(VsanHcmPhysicalMemInfo[] physicalMemInfos) {
      return ArrayUtils.isEmpty(physicalMemInfos) ? new HardwareMgmtPhysicalMemoryData[0] : (HardwareMgmtPhysicalMemoryData[])Arrays.stream(physicalMemInfos).filter(Objects::nonNull).map(HardwareMgmtUtil::buildMemorySlot).toArray((x$0) -> {
         return new HardwareMgmtPhysicalMemoryData[x$0];
      });
   }

   private static HardwareMgmtPhysicalMemoryData buildMemorySlot(VsanHcmPhysicalMemInfo physicalMemInfo) {
      HardwareMgmtPhysicalMemoryData result = new HardwareMgmtPhysicalMemoryData();
      result.common = buildHardwareCommonData(physicalMemInfo);
      result.location = physicalMemInfo.getLocation();
      result.maxOperatingFrequencyMhz = NumberUtils.toLong(physicalMemInfo.getMaxOperatingFrequencyMhz());
      result.memoryType = physicalMemInfo.getMemoryType();
      result.totalSizeMb = physicalMemInfo.getSizeMb();
      return result;
   }
}
