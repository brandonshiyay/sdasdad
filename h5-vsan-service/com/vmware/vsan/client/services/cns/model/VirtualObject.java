package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.virtualobjects.data.DisplayObjectType;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class VirtualObject {
   public List uuids = new ArrayList();
   public DisplayObjectType filter;

   public VirtualObject() {
   }

   public VirtualObject(List uuids, DisplayObjectType filter) {
      this.uuids = uuids;
      this.filter = filter;
   }
}
