package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsRecentActivity;
import java.util.Date;

@TsModel
public class FileAnalyticsShareFileData {
   public String fileName;
   public Date lastOperationTime;
   public FileOperationType lastOperation;

   public static FileAnalyticsShareFileData fromVmodl(VsanFileAnalyticsRecentActivity vmodl) {
      if (vmodl == null) {
         return null;
      } else {
         FileAnalyticsShareFileData shareFileData = new FileAnalyticsShareFileData();
         shareFileData.fileName = vmodl.fileName;
         shareFileData.lastOperationTime = vmodl.lastOperationTime != null ? vmodl.lastOperationTime.getTime() : null;
         shareFileData.lastOperation = FileOperationType.parse(vmodl.lastOperation);
         return shareFileData;
      }
   }

   public String toString() {
      return "FileAnalyticsShareFileData(fileName=" + this.fileName + ", lastOperationTime=" + this.lastOperationTime + ", lastOperation=" + this.lastOperation + ")";
   }
}
