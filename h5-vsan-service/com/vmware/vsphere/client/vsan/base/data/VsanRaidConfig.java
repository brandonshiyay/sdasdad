package com.vmware.vsphere.client.vsan.base.data;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class VsanRaidConfig extends VsanComponent {
   public List children = new ArrayList();
}
