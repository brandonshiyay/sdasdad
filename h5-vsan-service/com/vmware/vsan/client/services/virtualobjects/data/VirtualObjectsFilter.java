package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VirtualObjectsFilter {
   VMS,
   ISCSI_TARGETS,
   FCD_OBJECTS,
   FILE_SHARES,
   VOLUMES,
   FILE_VOLUMES,
   EXTENSION_APP,
   VR_TARGETS,
   OTHERS;
}
