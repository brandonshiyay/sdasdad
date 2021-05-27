package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum FileAnalyticsAgeDistribution {
   less_than_one_month,
   one_month_to_three_months,
   three_months_to_six_months,
   six_months_to_one_year,
   more_than_one_year;

   public String toString() {
      return "FileAnalyticsAgeDistribution." + this.name();
   }
}
