package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanTestCell {
   public ColumnType cellType;
   public Object cellValue;

   public VsanTestCell() {
   }

   public VsanTestCell(ColumnType cellType, Object cellValue) {
      this.cellType = cellType;
      this.cellValue = cellValue;
   }
}
