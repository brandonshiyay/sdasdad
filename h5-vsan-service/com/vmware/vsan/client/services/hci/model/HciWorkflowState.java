package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.util.EnumUtils;
import com.vmware.vsphere.client.vsan.util.EnumWithKey;

@TsModel
public enum HciWorkflowState implements EnumWithKey {
   IN_PROGRESS("in_progress"),
   DONE("done"),
   INVALID("invalid"),
   NOT_IN_HCI_WORKFLOW("not_in_hci_workflow");

   private String text;

   private HciWorkflowState(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public static HciWorkflowState fromString(String text) {
      return (HciWorkflowState)EnumUtils.fromString(HciWorkflowState.class, text, NOT_IN_HCI_WORKFLOW);
   }

   public String getKey() {
      return this.text;
   }
}
