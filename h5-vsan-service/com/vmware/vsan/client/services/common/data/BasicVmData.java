package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import java.util.Comparator;

@TsModel
public class BasicVmData {
   public String name;
   public boolean isPodVM;
   public String primaryIconId;
   public ManagedObjectReference vmRef;
   public ManagedObjectReference hostRef;
   public static final Comparator COMPARATOR = new Comparator() {
      public int compare(BasicVmData o1, BasicVmData o2) {
         return o1.name.compareTo(o2.name);
      }
   };

   public BasicVmData(ManagedObjectReference vmRef) {
      this.vmRef = vmRef;
   }
}
