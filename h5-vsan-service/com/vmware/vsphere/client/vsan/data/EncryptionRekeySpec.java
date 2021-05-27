package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class EncryptionRekeySpec {
   public boolean reEncryptData;
   public boolean allowReducedRedundancy;
}
