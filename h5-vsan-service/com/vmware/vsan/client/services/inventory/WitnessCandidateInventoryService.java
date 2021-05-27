package com.vmware.vsan.client.services.inventory;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.ResourcePool;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.dataprotection.model.PscConnectionDetails;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WitnessCandidateInventoryService extends InventoryBrowserService {
   private static final Log logger = LogFactory.getLog(WitnessCandidateInventoryService.class);
   @Autowired
   private ComputeInventoryService computeInventoryService;
   @Autowired
   private VmodlHelper vmodlHelper;

   @TsService
   public InventoryEntryData[] getNodeInfo(ManagedObjectReference[] nodeRefs, PscConnectionDetails pscDetails) throws Exception {
      return super.getNodeInfo(nodeRefs, pscDetails);
   }

   @TsService
   public InventoryEntryData[] getNodeChildren(ManagedObjectReference parentRef, PscConnectionDetails pscDetails, @ProxygenSerializer.ElementType(key = FilterContextKey.class,value = ManagedObjectReference.class) Map filterContext) throws Exception {
      return super.getNodeChildren(parentRef, pscDetails, filterContext);
   }

   protected List listChildrenRefs(ManagedObjectReference parent, PscConnectionDetails pscDetails, Map filterContext) {
      List childrenRefs = this.computeInventoryService.listChildrenRefs(parent, pscDetails, filterContext);
      return this.filterChildren(childrenRefs, filterContext);
   }

   private List filterChildren(List allChildren, Map filterContext) {
      List result = new ArrayList();
      List clusterRefs = new ArrayList();
      Iterator var5 = allChildren.iterator();

      while(var5.hasNext()) {
         ManagedObjectReference child = (ManagedObjectReference)var5.next();
         if (this.vmodlHelper.isOfType(child, ClusterComputeResource.class)) {
            clusterRefs.add(child);
         } else if (!this.vmodlHelper.isOfType(child, ResourcePool.class) && !child.equals(filterContext.get(FilterContextKey.CURRENT_WITNESS_HOST_REF))) {
            result.add(child);
         }
      }

      try {
         DataServiceResponse clusterDsProperties = QueryUtil.getProperties((ManagedObjectReference[])clusterRefs.toArray(new ManagedObjectReference[0]), new String[]{"configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled"});
         Iterator var11 = clusterDsProperties.getResourceObjects().iterator();

         while(var11.hasNext()) {
            Object resourceObject = var11.next();
            boolean vsanEnabled = (Boolean)clusterDsProperties.getProperty(resourceObject, "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled");
            if (!vsanEnabled && !resourceObject.equals(filterContext.get(FilterContextKey.CONTEXT_REF))) {
               result.add((ManagedObjectReference)resourceObject);
            }
         }
      } catch (Exception var9) {
         logger.error("Unable to query configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled properties for clusters", var9);
      }

      return result;
   }

   protected boolean isLeafNode(ManagedObjectReference item) {
      return this.computeInventoryService.isLeafNode(item);
   }

   protected List createRemoteNodeModel(List nodeRefs, PscConnectionDetails pscDetails) {
      throw new NotImplementedException("Withness candidate inventory does not support remote VC!");
   }

   protected InventoryEntryData createDSModel(ResultItem item) {
      return this.computeInventoryService.createDSModel(item);
   }
}
