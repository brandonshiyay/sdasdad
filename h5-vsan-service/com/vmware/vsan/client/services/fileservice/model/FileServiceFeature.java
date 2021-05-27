package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum FileServiceFeature {
   PAGINATION,
   SMB,
   SMB_PERFORMANCE,
   KERBEROS,
   OWE,
   SNAPSHOT,
   AFFINITY_SITE;
}
