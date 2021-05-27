package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VsanFileServiceShareAccessType {
   ALL_ACCESS,
   NO_ACCESS,
   CUSTOM_ACCESS;
}
