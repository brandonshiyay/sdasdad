package com.vmware.vsphere.client.vsan.health;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class VsanTestTable {
   public String title;
   public VsanTestColumn[] columns;
   public VsanTestRow[] rows;
   public boolean showHeader;
}
