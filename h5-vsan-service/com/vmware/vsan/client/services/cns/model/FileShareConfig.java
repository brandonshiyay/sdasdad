package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.fileservice.model.VsanFileShareProtocol;

@TsModel
public class FileShareConfig {
   public String domainName;
   public VsanFileShareProtocol protocol;

   public FileShareConfig() {
   }

   public FileShareConfig(String domainName, VsanFileShareProtocol protocol) {
      this.domainName = domainName;
      this.protocol = protocol;
   }
}
