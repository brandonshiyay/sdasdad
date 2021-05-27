package com.vmware.vsan.client.services.hardware.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Arrays;

@TsModel
public class HardwareMgmtData {
   public HardwareMgmtOverviewData overviewData;
   public HardwareMgmtMemoryData[] memoryBoards;
   public HardwareMgmtProcessorData[] processors;
   public HardwareMgmtNicData[] nics;
   public HardwareMgmtDiskBox[] diskBoxes;

   public String toString() {
      return "HardwareMgmtData{overviewData=" + this.overviewData + ", memoryBoards=" + Arrays.toString(this.memoryBoards) + ", processors=" + Arrays.toString(this.processors) + ", nics=" + Arrays.toString(this.nics) + ", diskBoxes=" + Arrays.toString(this.diskBoxes) + '}';
   }
}
