package com.vmware.vsphere.client.vsan.upgrade;

import com.vmware.proxygen.ts.TsModel;
import java.util.HashMap;
import java.util.Map;

@TsModel
public class VsanVersionInfoPerHost {
   public Map versions = new HashMap();

   public VsanVersionInfoPerHost(VsanDiskVersionData[] vsanDiskVersionsData) {
      if (vsanDiskVersionsData != null && vsanDiskVersionsData.length != 0) {
         VsanDiskVersionData[] var2 = vsanDiskVersionsData;
         int var3 = vsanDiskVersionsData.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            VsanDiskVersionData versionData = var2[var4];
            String key = String.valueOf(versionData.version);
            if (key != null) {
               if (this.versions.containsKey(key)) {
                  this.versions.put(key, (Integer)this.versions.get(key) + 1);
               } else {
                  this.versions.put(key, 1);
               }
            }
         }

      }
   }
}
