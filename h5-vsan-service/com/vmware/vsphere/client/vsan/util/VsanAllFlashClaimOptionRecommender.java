package com.vmware.vsphere.client.vsan.util;

import com.vmware.vim.binding.vim.host.ScsiDisk;
import com.vmware.vsphere.client.vsan.data.ClaimOption;
import com.vmware.vsphere.client.vsan.data.VsanDiskData;
import com.vmware.vsphere.client.vsan.data.VsanSemiAutoClaimDisksData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

public class VsanAllFlashClaimOptionRecommender extends VsanBaseClaimOptionRecommender {
   private static final int CACHE_TO_CAPACITY_DIVIDER = 8;
   private static final int CACHE_TO_CAPACITY_SIZE_DIVIDER = 11;
   private boolean _isAllFlashAvailable = true;
   private Map _HCL = null;

   public VsanAllFlashClaimOptionRecommender(VsanSemiAutoClaimDisksData data, Map HCL) {
      super(data);
      this._isAllFlashAvailable = data.isAllFlashAvailable;
      this._HCL = HCL;
   }

   public void recommend() {
      if (!ArrayUtils.isEmpty(this.getData().notInUseDisks)) {
         if (this._isAllFlashAvailable && (this._HCL == null || this._HCL.size() <= 0)) {
            this.makeAllFlashConfigRecommendation(this.getData());
         }

      }
   }

   private void makeAllFlashConfigRecommendation(VsanSemiAutoClaimDisksData data) {
      SortedMap ssdsBySize = new TreeMap();
      List storageSsdDisks = new ArrayList();
      long disksCapacity = 0L;
      int disksCount = 0;
      VsanDiskData[] var7 = data.notInUseDisks;
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         VsanDiskData disk = var7[var9];
         ScsiDisk scsiDisk = disk.disk;
         if (scsiDisk.ssd) {
            ++disksCount;
            Long capacity = calculateSize(scsiDisk.capacity);
            disksCapacity += capacity;
            if (disk.markedAsCapacityFlash) {
               storageSsdDisks.add(disk);
            } else {
               if (!ssdsBySize.containsKey(capacity)) {
                  ssdsBySize.put(capacity, new ArrayList());
               }

               ((List)ssdsBySize.get(capacity)).add(disk);
            }
         }
      }

      if (ssdsBySize.size() == 1) {
         this.makeConfigRecommendation((List)null, (List)ssdsBySize.get(ssdsBySize.firstKey()), data.numAllFlashGroups);
      } else if (ssdsBySize.size() > 1) {
         long minCacheCapacity = disksCapacity / 11L;
         VsanAllFlashClaimOptionRecommender.SortedDiskGroups ssdGroupsBySize = new VsanAllFlashClaimOptionRecommender.SortedDiskGroups();
         Iterator var19 = ssdsBySize.entrySet().iterator();

         while(var19.hasNext()) {
            Entry entry = (Entry)var19.next();
            ssdGroupsBySize.addDiskGroup((Long)entry.getKey() * (long)((List)entry.getValue()).size(), (List)entry.getValue());
         }

         boolean moreCacheNeeded = true;
         int cacheDisksCount = 0;
         List cacheSsdDisks = new LinkedList();
         long currentCache = 0L;

         List disks;
         while(ssdGroupsBySize.getDiskGroupsCount() != 0) {
            Long size = ssdGroupsBySize.getSmallestDiskGroupCapacity();
            currentCache += size;
            if (currentCache >= minCacheCapacity) {
               moreCacheNeeded = false;
            }

            disks = ssdGroupsBySize.getSmallestGroupListWithLeastDisks();
            cacheSsdDisks.addAll(disks);
            cacheDisksCount += disks.size();
            if (!moreCacheNeeded) {
               break;
            }
         }

         int maxCacheDisks = (disksCount - 1) / 8 + 1;

         while(true) {
            if (maxCacheDisks <= cacheDisksCount || ssdGroupsBySize.getDiskGroupsCount() == 0) {
               storageSsdDisks.addAll(ssdGroupsBySize.getAllDisks());
               if (storageSsdDisks.size() == 0 || cacheSsdDisks.size() > storageSsdDisks.size()) {
                  return;
               }

               this.makeConfigRecommendation(cacheSsdDisks, storageSsdDisks, data.numAllFlashGroups);
               break;
            }

            disks = ssdGroupsBySize.getSmallestGroupListWithLeastDisks();
            cacheDisksCount += disks.size();
            cacheSsdDisks.addAll(disks);
         }
      }

   }

   protected void markDisksForClaimingOption(List disks, ClaimOption option) {
      if (!CollectionUtils.isEmpty(disks)) {
         VsanDiskData disk;
         for(Iterator var3 = disks.iterator(); var3.hasNext(); disk.recommendedAllFlashClaimOption = option) {
            disk = (VsanDiskData)var3.next();
         }

      }
   }

   private static class DiskGroupsList {
      private List _list;

      private DiskGroupsList() {
         this._list = new ArrayList();
      }

      public void addDiskGroup(List group) {
         this._list.add(group);
      }

      public List getAllDiskGroups() {
         return this._list;
      }

      public List getItemWithSmallestSize() {
         List result = null;
         if (this._list.size() > 1) {
            int minimumDisks = ((List)this._list.get(0)).size();
            int indexMinimumSize = 0;

            for(int i = 0; i < this._list.size(); ++i) {
               if (minimumDisks > ((List)this._list.get(i)).size()) {
                  minimumDisks = ((List)this._list.get(i)).size();
                  indexMinimumSize = i;
               }
            }

            result = (List)this._list.remove(indexMinimumSize);
         } else if (this._list.size() == 1) {
            result = (List)this._list.get(0);
         }

         return result;
      }

      // $FF: synthetic method
      DiskGroupsList(Object x0) {
         this();
      }
   }

   private static class SortedDiskGroups {
      private SortedMap _map;

      private SortedDiskGroups() {
         this._map = new TreeMap();
      }

      public void addDiskGroup(Long groupSize, List group) {
         if (!this._map.containsKey(groupSize)) {
            this._map.put(groupSize, new VsanAllFlashClaimOptionRecommender.DiskGroupsList());
         }

         VsanAllFlashClaimOptionRecommender.DiskGroupsList list = (VsanAllFlashClaimOptionRecommender.DiskGroupsList)this._map.get(groupSize);
         list.addDiskGroup(group);
      }

      public List getSmallestGroupListWithLeastDisks() {
         return this.getGroupListWithLeastDisks(this.getSmallestDiskGroupCapacity());
      }

      private List getGroupListWithLeastDisks(Long size) {
         List result = null;
         VsanAllFlashClaimOptionRecommender.DiskGroupsList currentItem = (VsanAllFlashClaimOptionRecommender.DiskGroupsList)this._map.get(size);
         if (currentItem.getAllDiskGroups().size() > 1) {
            result = currentItem.getItemWithSmallestSize();
         } else {
            result = ((VsanAllFlashClaimOptionRecommender.DiskGroupsList)this._map.remove(size)).getItemWithSmallestSize();
         }

         return result;
      }

      public int getDiskGroupsCount() {
         return this._map.size();
      }

      public List getAllDisks() {
         List result = new ArrayList();
         Iterator var2 = this._map.values().iterator();

         while(var2.hasNext()) {
            VsanAllFlashClaimOptionRecommender.DiskGroupsList groups = (VsanAllFlashClaimOptionRecommender.DiskGroupsList)var2.next();
            Iterator var4 = groups.getAllDiskGroups().iterator();

            while(var4.hasNext()) {
               List diskGroup = (List)var4.next();
               result.addAll(diskGroup);
            }
         }

         return result;
      }

      public Long getSmallestDiskGroupCapacity() {
         return (Long)this._map.firstKey();
      }

      // $FF: synthetic method
      SortedDiskGroups(Object x0) {
         this();
      }
   }
}
