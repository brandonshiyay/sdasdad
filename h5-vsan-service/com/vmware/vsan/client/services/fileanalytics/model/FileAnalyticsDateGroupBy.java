package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum FileAnalyticsDateGroupBy implements EnumWithKey {
   CREATED_DATE("created_date"),
   ACCESSED_DATE("last_accessed_date"),
   MODIFIED_DATE("last_modified_date");

   private String key;

   private FileAnalyticsDateGroupBy(String key) {
      this.key = key;
   }

   public String getKey() {
      return this.key;
   }

   public static FileAnalyticsDateGroupBy fromString(String text) {
      return (FileAnalyticsDateGroupBy)EnumUtils.fromString(FileAnalyticsDateGroupBy.class, text);
   }

   public String toString() {
      return "FileAnalyticsDateGroupBy." + this.name() + "(key=" + this.getKey() + ")";
   }
}
