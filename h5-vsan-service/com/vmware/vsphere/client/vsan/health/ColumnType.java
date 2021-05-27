package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum ColumnType {
   mor,
   listMor,
   listString,
   listFloat,
   listLong,
   vsanObjectUuid,
   health,
   string,
   Long,
   Float,
   dynamic,
   HostReference,
   vsanObjectHealth,
   vsanObjectHealthv2,
   pspHealth,
   date,
   unknown;
}
