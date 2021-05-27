package com.vmware.vsan.client.services.ioinsight.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum IoInsightRunningState implements EnumWithKey {
   RUNNING("running"),
   COMPLETED("completed");

   private String text;

   private IoInsightRunningState(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public static IoInsightRunningState fromString(String text) {
      return (IoInsightRunningState)EnumUtils.fromString(IoInsightRunningState.class, text);
   }

   public String getKey() {
      return this.text;
   }
}
