package com.vmware.vsan.client.services.csd;

import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.Iterator;

public class CsdUtils {
   public static final String[] SHAREABLE_DATASTORE_QUERY_PROPERTIES = new String[]{"primaryIconId", "summary", "info.containerId", "vm._length"};

   public static String convertClusterUuidToDsUuidFormat(String clusterUuid) {
      String formattedUuid = clusterUuid.replace("-", "");
      return formattedUuid.substring(0, 16) + "-" + formattedUuid.substring(16, 32);
   }

   public static ManagedObjectReference getDatastoreServerCluster(ManagedObjectReference datastoreRef) {
      try {
         DataServiceResponse dsResponse = getDatastoreHostsDsResponse(datastoreRef);
         Iterator var2 = dsResponse.getResourceObjects().iterator();

         ManagedObjectReference clusterRef;
         do {
            if (!var2.hasNext()) {
               throw new VsanUiLocalizableException("vsan.csd.loadServerCluster.error");
            }

            ManagedObjectReference hostRef = (ManagedObjectReference)var2.next();
            clusterRef = (ManagedObjectReference)dsResponse.getProperty(hostRef, "cluster");
         } while(clusterRef == null);

         return clusterRef;
      } catch (Exception var5) {
         throw new VsanUiLocalizableException("vsan.csd.loadServerCluster.error");
      }
   }

   private static DataServiceResponse getDatastoreHostsDsResponse(ManagedObjectReference datastoreRef) throws Exception {
      Constraint serverHostsConstraint = QueryUtil.createConstraintForRelationship(datastoreRef, "serverHosts", HostSystem.class.getSimpleName());
      QuerySpec query = QueryUtil.buildQuerySpec(serverHostsConstraint, new String[]{"cluster"});
      return QueryUtil.getDataServiceResponse(QueryUtil.getData(query), new String[]{"cluster"});
   }
}
