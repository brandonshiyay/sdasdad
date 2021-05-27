package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Date;

@TsModel
public class FileAnalyticsFileShareGrowthData {
   public Date date;
   public long shareSize;
   public long growthValue;
   public double growthPercentage;

   public FileAnalyticsFileShareGrowthData() {
   }

   public FileAnalyticsFileShareGrowthData(Date date, long shareSize, long growthValue, double growthPercentage) {
      this.date = date;
      this.shareSize = shareSize;
      this.growthValue = growthValue;
      this.growthPercentage = growthPercentage;
   }

   public String toString() {
      return "FileAnalyticsFileShareGrowthData(date=" + this.date + ", shareSize=" + this.shareSize + ", growthValue=" + this.growthValue + ", growthPercentage=" + this.growthPercentage + ")";
   }
}
