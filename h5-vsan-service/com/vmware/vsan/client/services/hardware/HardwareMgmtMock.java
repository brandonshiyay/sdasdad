package com.vmware.vsan.client.services.hardware;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
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
import com.vmware.vim.vsan.binding.vim.host.VsanHostHardwareInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanStorageEnclosureInfo;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RelationalConstraint;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardwareMgmtMock {
   private static final Logger logger = LoggerFactory.getLogger(HardwareMgmtMock.class);
   private static final String[] MANIFACTURERS = new String[]{"Intel", "HP", "IBM", "Intel long long long long vendor name"};
   private static final String[] STRING_CHARS = "ABCDEFGHIMNP0123456789".split("");
   private static final String[] NUMERIC_CHARS = "0123456789".split("");
   private static final Long[] SPEED_MHZ = new Long[]{3200L, 3600L, 4000L, 4200L, 4600L, null};
   private static final String[] HEALTH_STATUSES = new String[]{"OK", "CRITICAL", "WARNING"};
   private static final String[] CPU_MODELS = new String[]{"Xeon W-3200", "Xeon W-4100", "Itanium 2", "Pentium Dual-Core", "Itanium 2 long long long model Itanium 2 long long long model"};
   private static final Long[] CPU_CORES_COUNTS = new Long[]{28L, 30L, 32L, 34L, 36L};
   private static final Long[] CPU_THREADS_COUNT = new Long[]{48L, 52L, 56L, null};
   private static final Long[] MEMORY_SIZES_MB = new Long[]{16384L, 32768L, 65536L};
   private static final String[] MEMORY_MODELS = new String[]{"EEMC3", "EEMC1", "MCR3", "MCR3 some very long model"};
   private static final String[] MEMORY_TYPES = new String[]{"DRAM", "RAM"};
   private static final String[] BOOT_DEVICE_MODES = new String[]{"BIOS", "UEFI"};
   private static final String[] DISK_GROUPS = new String[]{"diskGroup1", "diskGroup1", "diskGroup2", "diskGroup3", "diskGroup4"};
   private static final String[] VM_KERNEL_ADAPTERS = new String[]{"vmk0", "vmk1", "vmk2", "vmk3", "vmk4", "vmk5"};

   public static VsanHostHardwareInfo mockApiResult(ManagedObjectReference hostRef) {
      int mockCaseIndex = getHostPosition(hostRef) % HardwareMgmtMock.MockCase.values().length;
      HardwareMgmtMock.MockCase mockCase = HardwareMgmtMock.MockCase.values()[mockCaseIndex];
      return new VsanHostHardwareInfo("a host name", mockStorageControllers(mockCase), mockPNics(), mockChassis(mockCase), mockProcessors(), mockMemory(), mockBootDevice());
   }

   private static int getHostPosition(ManagedObjectReference hostRef) {
      RelationalConstraint clusterConstraint = (RelationalConstraint)QueryUtil.createConstraintForRelationship(hostRef, "cluster", ClusterComputeResource.class.getSimpleName());
      RelationalConstraint hostsConstraint = QueryUtil.createRelationalConstraint("host", clusterConstraint, true, HostSystem.class.getSimpleName());
      QuerySpec spec = QueryUtil.buildQuerySpec((Constraint)hostsConstraint, new String[]{"name"});
      TreeMap result = new TreeMap();

      try {
         ResultSet rs = QueryUtil.getData(spec);
         Arrays.stream(rs.items).forEach((item) -> {
            result.put(item.properties[0].value.toString(), (ManagedObjectReference)item.properties[0].resourceObject);
         });
      } catch (Exception var6) {
         logger.error("Error getting mock index, return first available", var6);
         return 0;
      }

      return (new ArrayList(result.values())).indexOf(hostRef);
   }

   private static VsanHcmStorageControllerInfo[] mockStorageControllers(HardwareMgmtMock.MockCase mockCase) {
      VsanHcmStorageControllerInfo controller = new VsanHcmStorageControllerInfo();
      mockCommonDeviceInfo(controller, mockCommonDeviceInfo());
      controller.model = "Virtual disk model dummy";
      controller.health = "OK";
      controller.serialNumber = "55789";
      controller.operatingMode = "RAID";
      controller.drives = mockDrives(mockCase);
      return new VsanHcmStorageControllerInfo[]{controller};
   }

   private static VsanHclCommonDeviceInfo mockCommonDeviceInfo() {
      VsanHclCommonDeviceInfo result = new VsanHclCommonDeviceInfo();
      result.deviceName = "vmhba1";
      result.displayName = "Intel Corporation PIIX4";
      result.vendorId = Long.parseLong(String.join("", getSubset(NUMERIC_CHARS, 4, true)));
      result.subVendorId = Long.parseLong(String.join("", getSubset(NUMERIC_CHARS, 6, true)));
      result.deviceId = Long.parseLong(String.join("", getSubset(NUMERIC_CHARS, 8, true)));
      result.subDeviceId = Long.parseLong(String.join("", getSubset(NUMERIC_CHARS, 10, true)));
      return result;
   }

   private static void mockCommonDeviceInfo(VsanHclCommonDeviceInfo result, VsanHclCommonDeviceInfo mockCommonDeviceInfo) {
      result.deviceName = mockCommonDeviceInfo.deviceName;
      result.displayName = mockCommonDeviceInfo.displayName;
      result.vendorId = mockCommonDeviceInfo.vendorId;
      result.subVendorId = mockCommonDeviceInfo.subVendorId;
      result.deviceId = mockCommonDeviceInfo.deviceId;
      result.subDeviceId = mockCommonDeviceInfo.subDeviceId;
   }

   private static VsanHcmDriveInfo mockDrive1() {
      VsanHcmDriveInfo result = new VsanHcmDriveInfo();
      mockCommonInfo(result, mockCommonInfo1());
      result.deviceName = "mpx.vmhba0:C0:T3:L0";
      result.isSsd = true;
      result.usedByVsan = true;
      result.ineligibleForVsan = false;
      result.isCapacity = false;
      result.diskGroupUuid = "529ff849-0c0e-ad28-b3c2-fec78686cc81";
      result.ledOperationSupported = true;
      result.interfaceType = "NVMe";
      result.location = "1:1";
      result.capacityBytes = 10737418240L;
      return result;
   }

   private static VsanHcmDriveInfo mockDrive2() {
      VsanHcmDriveInfo result = new VsanHcmDriveInfo();
      mockCommonInfo(result, mockCommonInfo2());
      result.deviceName = "mpx.vmhba0:C0:T4:L0";
      result.isSsd = false;
      result.usedByVsan = true;
      result.ineligibleForVsan = false;
      result.isCapacity = true;
      result.diskGroupUuid = "529ff849-0c0e-ad28-b3c2-fec78686cc81";
      result.ledOperationSupported = true;
      result.interfaceType = "NVMe";
      result.location = "1:4";
      result.capacityBytes = 10737418240L;
      return result;
   }

   private static VsanHcmDriveInfo mockDrive3() {
      VsanHcmDriveInfo result = new VsanHcmDriveInfo();
      mockCommonInfo(result, mockCommonInfo3());
      result.deviceName = "mpx.vmhba0:C0:T4:L1";
      result.isSsd = true;
      result.usedByVsan = true;
      result.ineligibleForVsan = false;
      result.isCapacity = true;
      result.diskGroupUuid = "529ff849-0c0e-ad28-b3c2-fec78686cc81";
      result.ledOperationSupported = false;
      result.interfaceType = "NVMe";
      result.location = "1:5";
      result.capacityBytes = 10737418240L;
      return result;
   }

   private static void mockCommonInfo(VsanHardwareCommonInfo result, VsanHardwareCommonInfo commonInfo) {
      result.model = commonInfo.model;
      result.manufacturer = commonInfo.manufacturer;
      result.health = commonInfo.health;
      result.error = commonInfo.error;
      result.serialNumber = commonInfo.serialNumber;
      result.sku = commonInfo.sku;
   }

   private static VsanHardwareCommonInfo mockCommonInfo1() {
      return new VsanHardwareCommonInfo("Virtual disk model dummy", "VMware dummy", "OK", (Exception)null, "55789", "87H6");
   }

   private static VsanHardwareCommonInfo mockCommonInfo2() {
      return new VsanHardwareCommonInfo("Virtual disk model dummy", "VMware dummy", "WARNING", new Exception("Something wrong is going on with this disk"), "145874", "87H6");
   }

   private static VsanHardwareCommonInfo mockCommonInfo3() {
      return new VsanHardwareCommonInfo("Virtual disk model dummy", "VMware dummy", "CRITICAL", new Exception("This disk is really unhealthy."), "22335588", "87H6");
   }

   private static VsanHcmDriveInfo mockDrive(int box, int slot) {
      return mockDrive(box + ":" + slot);
   }

   private static VsanHcmDriveInfo mockDrive(String location) {
      VsanHcmDriveInfo result = new VsanHcmDriveInfo();
      mockCommonInfo(result, (VsanHardwareCommonInfo)getRandom(new VsanHardwareCommonInfo[]{mockCommonInfo1(), mockCommonInfo1(), mockCommonInfo1(), mockCommonInfo2(), mockCommonInfo3()}));
      result.deviceName = "mpx.vmhba0:C0:T3:L0";
      result.isSsd = genBoolean();
      result.usedByVsan = genBoolean();
      result.ineligibleForVsan = genBoolean();
      result.isCapacity = genBoolean();
      result.diskGroupUuid = (String)getRandom(DISK_GROUPS);
      result.ledOperationSupported = false;
      result.interfaceType = "NVMe";
      result.location = location;
      result.capacityBytes = 10737418240L;
      return result;
   }

   private static VsanHcmNicInfo[] mockPNics() {
      return new VsanHcmNicInfo[]{mockNickInfo(4), mockNickInfo(2), mockNickInfo(6)};
   }

   private static VsanHcmNicInfo mockNickInfo(int portsCount) {
      VsanHcmNicInfo result = new VsanHcmNicInfo();
      mockCommonDeviceInfo(result, mockCommonDeviceInfo());
      result.hardwareInfo = (VsanHardwareCommonInfo)getRandom(new VsanHardwareCommonInfo[]{mockCommonInfo1(), mockCommonInfo2(), mockCommonInfo3()});
      result.physicalPorts = new VsanHcmNetworkPortInfo[portsCount];

      for(int i = 0; i < portsCount; ++i) {
         result.physicalPorts[i] = mockNicPort();
      }

      return result;
   }

   private static VsanHcmNetworkPortInfo mockNicPort() {
      StringBuilder mac = new StringBuilder();

      for(int i = 0; i < 6; ++i) {
         mac.append(genString(2));
         if (i != 5) {
            mac.append("-");
         }
      }

      return new VsanHcmNetworkPortInfo(genString((new Random()).nextInt(4)) + "-" + genString((new Random()).nextInt(6)), mac.toString(), (String)getRandom(new String[]{"up", "down", "unavailable"}), (Long)getRandom(new Long[]{1000L, 100L, 10L}), (String)getRandom(HEALTH_STATUSES), getSubset(VM_KERNEL_ADAPTERS, (new Random()).nextInt(VM_KERNEL_ADAPTERS.length - 1), false), genBoolean(), (new Random()).nextInt(10) > 8 ? new Exception(genString(15)) : null);
   }

   private static VsanHcmChassisInfo mockChassis(HardwareMgmtMock.MockCase mockCase) {
      VsanHcmChassisInfo result = new VsanHcmChassisInfo();
      mockCommonInfo(result, mockCommonInfo1());
      result.storageEnclosures = mockBoxes(mockCase);
      mockEnclosureName(result, mockCase);
      return result;
   }

   private static void mockEnclosureName(VsanHcmChassisInfo result, HardwareMgmtMock.MockCase mockCase) {
      switch(mockCase) {
      case BOXES:
         result.setEnclosureDisplayName("Box");
         result.setHideEnclosureName(false);
         break;
      case SAS_EXPANDER:
         result.setEnclosureDisplayName((String)null);
         result.setHideEnclosureName(true);
         result.setSasExpanderInstalled(true);
         break;
      case NO_BOXES:
         result.setEnclosureDisplayName("Should not see this");
         result.setHideEnclosureName(false);
         break;
      case MIXED:
         result.setEnclosureDisplayName("Mixed");
         result.setHideEnclosureName(false);
      }

   }

   private static VsanStorageEnclosureInfo[] mockBoxes(HardwareMgmtMock.MockCase mockCase) {
      switch(mockCase) {
      case BOXES:
         return mockBoxes();
      case SAS_EXPANDER:
         return mockSasExpanderBoxes();
      case NO_BOXES:
         return mockNoBoxesBoxes();
      case MIXED:
      default:
         return mockMixedBoxes();
      }
   }

   private static VsanHcmDriveInfo[] mockDrives(HardwareMgmtMock.MockCase mockCase) {
      switch(mockCase) {
      case BOXES:
         return mockBoxesDrives();
      case SAS_EXPANDER:
         return mockSasExpanderDrives();
      case NO_BOXES:
         return mockNoBoxesDrives();
      case MIXED:
      default:
         return mockMixedDrives();
      }
   }

   private static VsanStorageEnclosureInfo[] mockBoxes() {
      return new VsanStorageEnclosureInfo[]{new VsanStorageEnclosureInfo(1, "FRONT", 8, 1, 8, new int[0]), new VsanStorageEnclosureInfo(3, "MIDDLE", 2, 1, 2, new int[0]), new VsanStorageEnclosureInfo(2, "BACK", 6, 1, 6, new int[0]), new VsanStorageEnclosureInfo(4, (String)null, 4, 1, 5, new int[]{4})};
   }

   private static VsanHcmDriveInfo[] mockBoxesDrives() {
      return new VsanHcmDriveInfo[]{mockDrive1(), mockDrive2(), mockDrive3(), mockDrive(2, 1), mockDrive(2, 3), mockDrive(2, 4), mockDrive(2, 6), mockDrive(3, 1), mockDrive(3, 2), mockDrive(4, 1), mockDrive(4, 3), mockDrive(4, 4)};
   }

   private static VsanStorageEnclosureInfo[] mockSasExpanderBoxes() {
      return new VsanStorageEnclosureInfo[]{new VsanStorageEnclosureInfo(1, "FRONT", 24, 1, 24, new int[0]), new VsanStorageEnclosureInfo(2, "BACK", 2, 1, 2, new int[0]), new VsanStorageEnclosureInfo(3, "BACK", 2, 25, 27, new int[]{26})};
   }

   private static VsanHcmDriveInfo[] mockSasExpanderDrives() {
      return new VsanHcmDriveInfo[]{mockDrive(1, 1), mockDrive(1, 2), mockDrive(1, 3), mockDrive(1, 4), mockDrive(1, 5), mockDrive(1, 6), mockDrive(1, 7), mockDrive(1, 8), mockDrive(1, 9), mockDrive(1, 10), mockDrive(1, 11), mockDrive(1, 12), mockDrive(1, 13), mockDrive(1, 14), mockDrive(1, 15), mockDrive(1, 16), mockDrive(1, 17), mockDrive(1, 18), mockDrive(1, 19), mockDrive(1, 20), mockDrive(1, 21), mockDrive(1, 22), mockDrive(1, 23), mockDrive(1, 24), mockDrive(2, 1), mockDrive(2, 2), mockDrive(3, 25), mockDrive(3, 27)};
   }

   private static VsanStorageEnclosureInfo[] mockNoBoxesBoxes() {
      return null;
   }

   private static VsanHcmDriveInfo[] mockNoBoxesDrives() {
      return new VsanHcmDriveInfo[]{mockDrive(""), mockDrive(""), mockDrive(""), mockDrive(""), mockDrive(""), mockDrive(""), mockDrive("")};
   }

   private static VsanStorageEnclosureInfo[] mockMixedBoxes() {
      return new VsanStorageEnclosureInfo[]{new VsanStorageEnclosureInfo(1, "FRONT", 4, 1, 4, new int[0]), new VsanStorageEnclosureInfo(3, "BACK", 3, 7, 10, new int[]{8})};
   }

   private static VsanHcmDriveInfo[] mockMixedDrives() {
      return new VsanHcmDriveInfo[]{mockDrive(1, 1), mockDrive(1, 2), mockDrive(1, 2), mockDrive(1, 4), mockDrive(1, 9), mockDrive(1, 10), mockDrive("1:"), mockDrive("1:"), mockDrive(2, 5), mockDrive(2, 6), mockDrive(3, 8), mockDrive(3, 10), mockDrive(4, 1), mockDrive(""), mockDrive(""), mockDrive(""), mockDrive(""), mockDrive(""), mockDrive("")};
   }

   private static VsanHcmProcessorInfo[] mockProcessors() {
      return new VsanHcmProcessorInfo[]{mockProcessorInfo(), mockProcessorInfo(), mockProcessorInfo(), mockProcessorInfo(), mockProcessorInfo(), mockProcessorInfo()};
   }

   private static VsanHcmProcessorInfo mockProcessorInfo() {
      return new VsanHcmProcessorInfo((String)getRandom(CPU_MODELS), (String)getRandom(MANIFACTURERS), (String)getRandom(HEALTH_STATUSES), (Exception)null, genString(10), genString(4), 1, (Long)getRandom(SPEED_MHZ), (Long)getRandom(SPEED_MHZ), (Long)getRandom(CPU_CORES_COUNTS), (Long)getRandom(CPU_THREADS_COUNT));
   }

   private static VsanHcmMemoryInfo[] mockMemory() {
      VsanHcmPhysicalMemInfo[] board1Slots = new VsanHcmPhysicalMemInfo[]{mockMemSlot("1"), mockMemSlot("2"), mockMemSlot("3"), mockMemSlot("4"), mockMemSlot("5"), mockMemSlot("6"), mockMemSlot("7"), mockMemSlot("8"), mockMemSlot("9"), mockMemSlot("10")};
      VsanHcmPhysicalMemInfo[] board2Slots = new VsanHcmPhysicalMemInfo[]{mockMemSlot("1"), mockMemSlot("2"), mockMemSlot("3"), mockMemSlot("4"), mockMemSlot("5"), mockMemSlot("6"), mockMemSlot("7"), mockMemSlot("8")};
      return new VsanHcmMemoryInfo[]{new VsanHcmMemoryInfo((Long)Arrays.stream(board1Slots).map(VsanHcmPhysicalMemInfo::getSizeMb).reduce(0L, Long::sum), 1, board1Slots.length, (Long)getRandom(SPEED_MHZ), board1Slots), new VsanHcmMemoryInfo((Long)Arrays.stream(board2Slots).map(VsanHcmPhysicalMemInfo::getSizeMb).reduce(0L, Long::sum), 2, board1Slots.length, (Long)getRandom(SPEED_MHZ), board2Slots)};
   }

   private static VsanHcmPhysicalMemInfo mockMemSlot(String location) {
      return new VsanHcmPhysicalMemInfo((String)getRandom(MEMORY_MODELS), (String)getRandom(MANIFACTURERS), (String)getRandom(HEALTH_STATUSES), (Exception)null, genString(10), genString(4), location, (Long)getRandom(MEMORY_SIZES_MB), (Long)getRandom(SPEED_MHZ), (String)getRandom(MEMORY_TYPES));
   }

   private static VsanHcmBootDeviceInfo mockBootDevice() {
      return new VsanHcmBootDeviceInfo((String)getRandom(BOOT_DEVICE_MODES), "mpx.vmhba0:C0:T1:L0", (new Random()).nextInt(10) > 8 ? new Exception(genString(15)) : null);
   }

   private static Object getRandom(Object[] availableValues) {
      int randomValueIndex = (new Random()).nextInt(availableValues.length);
      return availableValues[randomValueIndex];
   }

   private static String[] getSubset(String[] availableValues, int count, boolean allowRepetative) {
      int valuesCount = availableValues.length;
      count = Math.min(count, valuesCount);
      Object result = allowRepetative ? new ArrayList() : new HashSet();

      while(((Collection)result).size() < count) {
         ((Collection)result).add(getRandom(availableValues));
      }

      return (String[])((Collection)result).toArray(new String[0]);
   }

   private static boolean genBoolean() {
      return (Boolean)getRandom(new Boolean[]{true, false});
   }

   public static String genString(int size) {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < size; ++i) {
         sb.append((String)getRandom(STRING_CHARS));
      }

      return sb.toString();
   }

   private static enum MockCase {
      BOXES,
      SAS_EXPANDER,
      NO_BOXES,
      MIXED;
   }
}
