package com.vmware.vsan.client.services.physicaldisks;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsService;
import com.vmware.vsphere.client.vsan.data.HostPhysicalMappingData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PhysicalDisksService {
   private static final Log logger = LogFactory.getLog(PhysicalDisksService.class);
   @Autowired
   private VsanDiskMappingsProvider diskMappingsProvider;
   @Autowired
   private VirtualObjectsService virtualObjectsService;

   @TsService
   public List getPhysicalDisksData(ManagedObjectReference clusterRef) throws Exception {
      List vsanHostsPhysicalDiskData = this.diskMappingsProvider.getVsanHostsPhysicalDiskData(clusterRef);
      Object diskItems = new ArrayList();

      try {
         diskItems = this.virtualObjectsService.listVirtualObjects(clusterRef);
      } catch (Exception var8) {
         logger.error("Unable to extract physical disks virtual objects data: " + var8);
      }

      List result = new ArrayList(vsanHostsPhysicalDiskData.size());
      Iterator var5 = vsanHostsPhysicalDiskData.iterator();

      while(var5.hasNext()) {
         HostPhysicalMappingData hostDisksMappingData = (HostPhysicalMappingData)var5.next();
         PhysicalDisksHierarchicalData hostData = new PhysicalDisksHierarchicalData(hostDisksMappingData);
         hostData.setVirtualObjectsData((List)diskItems);
         result.add(hostData);
      }

      return result;
   }
}
