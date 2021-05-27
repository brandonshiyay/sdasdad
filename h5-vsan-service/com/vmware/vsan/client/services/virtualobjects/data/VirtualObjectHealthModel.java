package com.vmware.vsan.client.services.virtualobjects.data;

import com.vmware.vim.vsan.binding.vim.host.VsanObjectHealthStateV2;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectCompositeHealth;
import com.vmware.vsphere.client.vsan.base.data.VsanObjectHealthState;
import com.vmware.vsphere.client.vsan.util.EnumUtils;

public class VirtualObjectHealthModel {
   public VsanObjectHealthState health;
   public VsanObjectCompositeHealth compositeHealth;

   public VirtualObjectHealthModel(String health, VsanObjectHealthStateV2 healthV2) {
      if (healthV2 != null) {
         this.compositeHealth = new VsanObjectCompositeHealth(healthV2);
      } else {
         this.health = (VsanObjectHealthState)EnumUtils.fromStringIgnoreCase(VsanObjectHealthState.class, health, VsanObjectHealthState.UNKNOWN);
      }

   }
}
