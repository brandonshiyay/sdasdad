package com.vmware.vsphere.client.vsan.upgrade;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanUpgradePreflightCheckIssue {
   public String message;
   public VsanUpgradePreflightCheckIssue.IssueType type;

   @TsModel
   public static enum IssueType {
      WARNING,
      ERROR;
   }
}
