package com.vmware.vsan.client.services.fileanalytics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileAnalyticsFileShareDetails;
import com.vmware.vsan.client.services.fileanalytics.FileAnalyticsUtil;
import com.vmware.vsan.client.services.fileservice.model.VsanFileShareProtocol;
import java.util.Date;

@TsModel
public class FileAnalyticsShareData {
   public String uuid;
   public String shareName;
   public VsanFileShareProtocol protocol;
   public long currentUsage;
   public Double usagePercentage;
   public Long quota;
   public long growth;
   public double growthPercentage;
   public long accessCount;
   public Date lastOperationTime;
   public FileOperationType lastOperation;

   public FileAnalyticsShareData() {
   }

   public FileAnalyticsShareData(String uuid, String shareName, VsanFileShareProtocol protocol, Long currentUsage, Long quota, Long growth, long accessCount, Date lastOperationTime, String lastOperation) {
      this.uuid = uuid;
      this.shareName = shareName;
      this.protocol = protocol;
      this.quota = quota;
      this.currentUsage = currentUsage;
      this.usagePercentage = FileAnalyticsUtil.getUsagePercentage(currentUsage, quota);
      this.growth = growth;
      this.growthPercentage = FileAnalyticsUtil.getGrowthPercentage(currentUsage, growth);
      this.accessCount = accessCount;
      this.lastOperationTime = lastOperationTime;
      this.lastOperation = FileOperationType.parse(lastOperation);
   }

   public static FileAnalyticsShareData fromVmodl(VsanFileAnalyticsFileShareDetails share) {
      return new FileAnalyticsShareData("file:ea8c496a-0897-492f-b815-dc2face62097", share.shareName, VsanFileShareProtocol.parse(share.protocols), share.currentUsage, share.quota, share.growth, share.accessCount, share.lastOperationTime != null ? share.lastOperationTime.getTime() : null, share.lastOperation);
   }

   public String toString() {
      return "FileAnalyticsShareData(uuid=" + this.uuid + ", shareName=" + this.shareName + ", protocol=" + this.protocol + ", currentUsage=" + this.currentUsage + ", usagePercentage=" + this.usagePercentage + ", quota=" + this.quota + ", growth=" + this.growth + ", growthPercentage=" + this.growthPercentage + ", accessCount=" + this.accessCount + ", lastOperationTime=" + this.lastOperationTime + ", lastOperation=" + this.lastOperation + ")";
   }
}
