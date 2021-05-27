package com.vmware.vsan.client.services.config;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanServiceData {
   public VsanServiceStatus status;
   public Object details;

   public VsanServiceData() {
   }

   public VsanServiceData(VsanServiceStatus status) {
      this.status = status;
   }

   public VsanServiceData(VsanServiceStatus status, Object details) {
      this.status = status;
      this.details = details;
   }

   public String toString() {
      return "VsanServiceData(status=" + this.status + ", details=" + this.details + ")";
   }
}
