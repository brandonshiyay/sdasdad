package com.vmware.vsan.client.services.network;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class RdmaData {
   public boolean isRdmaEnabled;
   public List unsupportedHosts;

   public String toString() {
      return "RdmaData(isRdmaEnabled=" + this.isRdmaEnabled + ", unsupportedHosts=" + this.unsupportedHosts + ")";
   }
}
