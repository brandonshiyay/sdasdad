package com.vmware.vsan.client.sessionmanager.vlsi.client.vchealth;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vsan.client.sessionmanager.vlsi.client.VlsiConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.util.RequestContextUtil;

public class VcHealthConnection extends VlsiConnection {
   private static final ManagedObjectReference VSPHERE_HEALTH_SYSTEM_MO_REF = new ManagedObjectReference("VsanVcClusterHealthSystem", "cloud-health", (String)null);

   public VsanVcClusterHealthSystem getVsphereHealthSystem() {
      return (VsanVcClusterHealthSystem)RequestContextUtil.setVcSessionCookie(super.createStub(VsanVcClusterHealthSystem.class, VSPHERE_HEALTH_SYSTEM_MO_REF), this.settings.getSessionCookie());
   }
}
