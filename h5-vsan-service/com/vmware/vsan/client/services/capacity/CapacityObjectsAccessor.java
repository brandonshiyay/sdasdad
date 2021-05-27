package com.vmware.vsan.client.services.capacity;

import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSpaceSummary;
import com.vmware.vsan.client.util.NumberUtils;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectType;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class CapacityObjectsAccessor {
   private static final Log logger = LogFactory.getLog(CapacityObjectsAccessor.class);
   private MultiValuedMap store = new HashSetValuedHashMap();

   public CapacityObjectsAccessor(VsanObjectSpaceSummary[] objectSpaceSummaries) {
      VsanObjectSpaceSummary[] var2 = objectSpaceSummaries;
      int var3 = objectSpaceSummaries.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         VsanObjectSpaceSummary spaceSummary = var2[var4];
         this.store.put(VsanObjectType.parse(spaceSummary.objType), spaceSummary);
      }

   }

   public long getObjectUsedCapacity(VsanObjectSpaceSummary objectSpaceSummary) {
      return objectSpaceSummary == null ? 0L : NumberUtils.toLong(objectSpaceSummary.usedB);
   }

   public VsanObjectSpaceSummary getAny(VsanObjectType type) {
      Collection values = this.store.get(type);
      if (CollectionUtils.isNotEmpty(values)) {
         if (values.size() > 1) {
            logger.debug("There are more than one VsanObjectSpaceSummary for type [" + type.name() + "] but only first is returned");
         }

         return (VsanObjectSpaceSummary)values.iterator().next();
      } else {
         return null;
      }
   }

   public Collection getAll(VsanObjectType type) {
      Collection values = this.store.get(type);
      return (Collection)(CollectionUtils.isEmpty(values) ? Collections.emptySet() : values);
   }
}
