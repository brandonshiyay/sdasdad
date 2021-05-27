package com.vmware.vsphere.client.vsan.perf.model;

import com.vmware.vim.binding.vmodl.data;

@data
public class PerformanceDiagnosticException {
   public String message;
   public String description;
   public String exceptionUrl;

   public PerformanceDiagnosticException() {
   }

   public PerformanceDiagnosticException(String exceptionMessage, String exceptionDetails, String exceptionUrl) {
      this.message = exceptionMessage;
      this.description = exceptionDetails;
      this.exceptionUrl = exceptionUrl;
   }
}
