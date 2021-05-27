package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public enum PspObjectHealthState implements EnumWithKey {
   HEALTHY("healthy"),
   REDUCED_AVAILABILITY("reducedavailability"),
   REBUILD("rebuild"),
   INACCESSIBLE("inaccessible"),
   UNKNOWN("unknown");

   private static final Log _logger = LogFactory.getLog(PspObjectHealthState.class);
   private final String key;

   private PspObjectHealthState(String key) {
      this.key = key;
   }

   private static PspObjectHealthState fromString(String text) {
      PspObjectHealthState result = (PspObjectHealthState)EnumUtils.fromString(PspObjectHealthState.class, text, UNKNOWN);
      if (result == UNKNOWN) {
         _logger.warn("Unknown PspObjectHealthState detected: " + text);
      }

      return result;
   }

   public static PspObjectHealthState fromServerLocalizedString(String text) {
      if (text != null) {
         text = text.replaceAll(" ", "");
         return fromString(text);
      } else {
         _logger.warn("Empty PspObjectHealthState text detected!");
         return UNKNOWN;
      }
   }

   public String getKey() {
      return this.key;
   }
}
