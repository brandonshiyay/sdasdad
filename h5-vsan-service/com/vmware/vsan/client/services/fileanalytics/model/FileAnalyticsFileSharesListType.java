package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsReportType;
import com.vmware.vsan.client.services.VsanUiLocalizableException;

@TsModel
public enum FileAnalyticsFileSharesListType {
   FASTEST_GROWTH,
   LARGEST,
   TOP_ACCESSED,
   TOP_CAPACITY;

   public VsanFileAnalyticsReportType toVmodl() throws VsanUiLocalizableException {
      switch(this) {
      case FASTEST_GROWTH:
         return VsanFileAnalyticsReportType.by_fileshare_growth;
      case LARGEST:
         return VsanFileAnalyticsReportType.by_fileshare_largest;
      case TOP_ACCESSED:
         return VsanFileAnalyticsReportType.by_fileshare_access;
      case TOP_CAPACITY:
         return VsanFileAnalyticsReportType.by_fileshare_capacity;
      default:
         throw new VsanUiLocalizableException("vsan.fileanalytics.fileSharesList.type.unknown");
      }
   }
}
