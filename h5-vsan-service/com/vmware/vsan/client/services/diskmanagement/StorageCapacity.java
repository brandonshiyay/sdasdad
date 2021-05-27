package com.vmware.vsan.client.services.diskmanagement;

import com.vmware.proxygen.ts.TsModel;
import java.util.Arrays;
import java.util.Optional;

@TsModel
public class StorageCapacity {
   public long total;
   public long free;
   public Long used;
   public Long reserved;

   public StorageCapacity() {
   }

   public StorageCapacity(long totalCapacity, Long usedCapacity, Long reservedCapacity) {
      this.total = totalCapacity;
      this.used = usedCapacity;
      this.reserved = reservedCapacity;
      this.updateFree();
   }

   private void updateFree() {
      this.free = this.total - ifNull(this.used) - ifNull(this.reserved);
   }

   public static StorageCapacity aggregate(IStorageData[] storageItems) {
      StorageCapacity capacity = new StorageCapacity(0L, 0L, (Long)null);
      Arrays.stream(storageItems).forEach((storage) -> {
         capacity.total += storage.getCapacity().total;
         capacity.used = capacity.used + ifNull(storage.getCapacity().used);
         Long reserved = storage.getCapacity().reserved;
         if (reserved != null) {
            if (capacity.reserved == null) {
               capacity.reserved = 0L;
            }

            capacity.reserved = capacity.reserved + ifNull(reserved);
         }

      });
      capacity.updateFree();
      return capacity;
   }

   private static long ifNull(Long value) {
      return (Long)Optional.ofNullable(value).orElse(0L);
   }

   public String toString() {
      return "StorageCapacity(total=" + this.total + ", free=" + this.free + ", used=" + this.used + ", reserved=" + this.reserved + ")";
   }
}
