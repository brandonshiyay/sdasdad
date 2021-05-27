package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum FileAnalyticsSizeDistribution {
   less_than_a_mb,
   one_mb_to_ten_mb,
   ten_mb_to_hundred_mb,
   hundred_mb_to_one_gb,
   more_than_one_gb;

   public String toString() {
      return "FileAnalyticsSizeDistribution." + this.name();
   }
}
