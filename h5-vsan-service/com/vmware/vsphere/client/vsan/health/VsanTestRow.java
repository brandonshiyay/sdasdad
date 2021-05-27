package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;
import java.util.List;

@TsModel
public class VsanTestRow {
   public VsanTestCell[] rowValues;
   public List nestedRows;

   public VsanTestRow() {
   }

   public VsanTestRow(VsanTestCell[] rowValues, List nestedRows) {
      this.rowValues = rowValues;
      this.nestedRows = nestedRows;
   }
}
