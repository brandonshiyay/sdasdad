package com.vmware.vsphere.client.vsan.util;

import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.data.VsanSemiAutoClaimDisksData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

public class VsanHybridClaimOptionRecommender extends VsanBaseClaimOptionRecommender {
   private static final Comparator _comparator = new Comparator() {
      public int compare(VsanDiskData o1, VsanDiskData o2) {
         long diff = VsanBaseClaimOptionRecommender.calculateSize(o1.disk.capacity) - VsanBaseClaimOptionRecommender.calculateSize(o2.disk.capacity);
         if (diff > 0L) {
            return 1;
         } else {
            return diff < 0L ? -1 : 0;
         }
      }
   };

   public VsanHybridClaimOptionRecommender(VsanSemiAutoClaimDisksData data) {
      super(data);
   }

   public void recommend() {
      if (!ArrayUtils.isEmpty(this.getData().notInUseDisks)) {
         this.makeHybridConfigRecommendation(this.getData());
      }
   }

   private void makeHybridConfigRecommendation(VsanSemiAutoClaimDisksData data) {
      List ssds = new ArrayList();
      List hdds = new ArrayList();
      VsanDiskData[] var4 = data.notInUseDisks;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         VsanDiskData disk = var4[var6];
         if (disk.disk.ssd) {
            ((List)ssds).add(disk);
         } else {
            hdds.add(disk);
         }
      }

      Collections.sort((List)ssds, _comparator);
      if (((List)ssds).size() > hdds.size()) {
         ssds = ((List)ssds).subList(0, hdds.size());
      }

      this.makeConfigRecommendation((List)ssds, hdds, data.numHybridGroups);
   }

   protected void markDisksForClaimingOption(List disks, ClaimOption option) {
      if (!CollectionUtils.isEmpty(disks)) {
         VsanDiskData disk;
         for(Iterator var3 = disks.iterator(); var3.hasNext(); disk.recommendedHybridClaimOption = option) {
            disk = (VsanDiskData)var3.next();
         }

      }
   }
}
