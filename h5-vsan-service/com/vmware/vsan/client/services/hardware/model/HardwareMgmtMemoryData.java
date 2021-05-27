package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Arrays;

@TsModel
public class HardwareMgmtMemoryData {
   public long totalMemoryMb;
   public int boardCpuNumber;
   public int slotsCount;
   public long operatingFrequencyMhz;
   public HardwareMgmtPhysicalMemoryData[] memorySlots;

   public String toString() {
      return "HardwareMgmtMemoryData{totalMemoryMb=" + this.totalMemoryMb + ", boardCpuNumber=" + this.boardCpuNumber + ", slotsCount=" + this.slotsCount + ", operatingFrequencyMhz=" + this.operatingFrequencyMhz + ", memorySlots=" + Arrays.toString(this.memorySlots) + "}";
   }
}
