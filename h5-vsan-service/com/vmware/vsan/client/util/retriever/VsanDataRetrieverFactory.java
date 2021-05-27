package com.vmware.vsan.client.util.retriever;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.common.PermissionService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.pbm.PbmClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanDataRetrieverFactory {
   @Autowired
   public VcClient vcClient;
   @Autowired
   public VsanClient vsanClient;
   @Autowired
   public VmodlHelper vmodlHelper;
   @Autowired
   public PbmClient pbmClient;
   @Autowired
   private PermissionService permissionService;

   public VsanAsyncDataRetriever createVsanAsyncDataRetriever(Measure measure, ManagedObjectReference objRef) {
      return new VsanAsyncDataRetriever(measure, objRef, this.vcClient, this.vsanClient, this.vmodlHelper, this.pbmClient, this.permissionService);
   }

   public VsanAsyncDataRetriever createVsanAsyncDataRetriever(Measure measure) {
      return this.createVsanAsyncDataRetriever(measure, (ManagedObjectReference)null);
   }
}
