package com.vmware.vsphere.client.vsan.health;

import com.vmware.vim.binding.vmodl.data;
import java.util.List;

@data
public class VsanHealthServicePreCheckResult {
   public boolean passed;
   public boolean vumRegistered;
   public List testsData;
}
