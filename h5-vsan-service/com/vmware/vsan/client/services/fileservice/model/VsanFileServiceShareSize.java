package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Arrays;
import java.util.List;

@TsModel
public enum VsanFileServiceShareSize implements EnumWithKey {
   MB("MEM_MB", new String[]{"MB", "M"}, 2),
   GB("MEM_GB", new String[]{"GB", "G"}, 3),
   TB("MEM_TB", new String[]{"TB", "T"}, 4);

   public final String labelKey;
   public final List values;
   public final double multiplier;

   private VsanFileServiceShareSize(String labelKey, String[] values, int power) {
      this.labelKey = labelKey;
      this.values = Arrays.asList(values);
      this.multiplier = Math.pow(1024.0D, (double)power);
   }

   public String[] getKey() {
      return (String[])this.values.toArray(new String[0]);
   }

   public static VsanFileServiceShareSize parse(String value) {
      return (VsanFileServiceShareSize)EnumUtils.fromStringIgnoreCase(VsanFileServiceShareSize.class, value);
   }

   public String toString() {
      return Utils.getLocalizedString(this.labelKey);
   }
}
