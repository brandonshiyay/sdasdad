package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanTestColumn {
   public String columnLabel;
   public ColumnType columnType;

   public VsanTestColumn() {
   }

   public VsanTestColumn(String columnLabel, ColumnType columnType) {
      this.columnLabel = columnLabel;
      this.columnType = columnType;
   }
}
