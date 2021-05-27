package com.vmware.vsan.client.services.networkdiagnostics.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.KeyAnyValue;
import com.vmware.vim.vsan.binding.vim.cluster.VsanNetworkDiagnostics;
import java.util.Date;

@TsModel
public class NetworkDiagnostic {
   private static final String NICNAME_KEY = "nicname";
   private static final String MESSAGE_KEY = "message";
   public Date createdTime;
   public String eventTypeId;
   public String nicname;
   public String errorMessage;

   public static NetworkDiagnostic create(VsanNetworkDiagnostics vsanNetworkDiagnostic) {
      NetworkDiagnostic result = new NetworkDiagnostic();
      result.createdTime = vsanNetworkDiagnostic.createdTime.getTime();
      KeyAnyValue[] var2 = vsanNetworkDiagnostic.arguments;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         KeyAnyValue argument = var2[var4];
         if ("nicname".equals(argument.getKey())) {
            result.nicname = (String)argument.getValue();
         } else if ("message".equals(argument.getKey())) {
            result.errorMessage = (String)argument.getValue();
         }
      }

      result.eventTypeId = vsanNetworkDiagnostic.eventTypeId;
      return result;
   }

   public String toString() {
      return "NetworkDiagnostic(createdTime=" + this.createdTime + ", eventTypeId=" + this.eventTypeId + ", nicname=" + this.nicname + ", errorMessage=" + this.errorMessage + ")";
   }
}
