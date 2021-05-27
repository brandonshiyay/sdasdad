package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.ResourceSpec;
import com.vmware.vise.data.query.DataException;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.ObjectIdentityConstraint;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RelationalConstraint;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.Response;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.common.VsanBaseDataProviderAdapter;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanWitnessHostProvider extends VsanBaseDataProviderAdapter {
   public static final String UNICAST_AGENT_ADDRESS = "unicastAgentAddress";
   public static final String IS_WITNESS_HOST_PROPERTY = "isWitnessHost";
   public static final String IS_METADATA_WITNESS_HOST_PROPERTY = "isMetadataWitnessHost";
   private static final Log _logger = LogFactory.getLog(VsanWitnessHostProvider.class);
   private final DataServiceExtensionRegistry _registry;
   @Autowired
   private VsanStretchedClusterService stretchedClusterService;

   public VsanWitnessHostProvider(DataServiceExtensionRegistry registry) {
      this._registry = registry;
      this._registry.registerDataAdapter(this, new String[]{ClusterComputeResource.class.getSimpleName()});
   }

   protected Response getResponse(RequestSpec requestSpec) {
      if (requestSpec != null && requestSpec.querySpec != null) {
         ArrayList resultSets = new ArrayList(requestSpec.querySpec.length);
         QuerySpec[] var3 = requestSpec.querySpec;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            QuerySpec spec = var3[var5];
            ResultSet resultSet = new ResultSet();
            VsanWitnessHostProvider.RequestData requestData = this.getClusterRefs(spec);
            if (!requestData.clusterRefs.isEmpty()) {
               try {
                  resultSet = this.getHosts(requestData);
               } catch (Exception var10) {
                  _logger.error("Error retrieving witness hosts: ", var10);
                  resultSet.error = DataException.newInstance(var10);
               }
            }

            resultSets.add(resultSet);
         }

         Response response = new Response();
         response.resultSet = (ResultSet[])resultSets.toArray(new ResultSet[0]);
         return response;
      } else {
         throw new IllegalArgumentException("requestSpec");
      }
   }

   private VsanWitnessHostProvider.RequestData getClusterRefs(QuerySpec spec) {
      if (spec == null) {
         return new VsanWitnessHostProvider.RequestData();
      } else {
         ResourceSpec resourceSpec = spec.resourceSpec;
         if (resourceSpec == null) {
            return new VsanWitnessHostProvider.RequestData();
         } else {
            return resourceSpec.constraint == null ? new VsanWitnessHostProvider.RequestData() : this.getClusterRefs(resourceSpec.constraint);
         }
      }
   }

   private VsanWitnessHostProvider.RequestData getClusterRefs(Constraint constraint) {
      if (constraint instanceof RelationalConstraint) {
         RelationalConstraint relConstraint = (RelationalConstraint)constraint;
         if (("witnessHost".equals(relConstraint.relation) || "allVsanHosts".equals(relConstraint.relation)) && relConstraint.constraintOnRelatedObject instanceof ObjectIdentityConstraint) {
            ObjectIdentityConstraint oiConstraint = (ObjectIdentityConstraint)relConstraint.constraintOnRelatedObject;
            if (oiConstraint.target != null) {
               return new VsanWitnessHostProvider.RequestData(Collections.singletonList((ManagedObjectReference)oiConstraint.target), "allVsanHosts".equals(relConstraint.relation));
            }
         }
      }

      return new VsanWitnessHostProvider.RequestData();
   }

   private ResultSet getHosts(VsanWitnessHostProvider.RequestData requestData) throws Exception {
      List witnessHosts = this.getWitnessHosts(requestData.clusterRefs);
      List regularHosts = this.getHostsInCluster(requestData);
      ResultItem[] resultItems = new ResultItem[witnessHosts.size() + regularHosts.size()];
      int index = 0;

      Iterator var6;
      WitnessHostData witnessData;
      for(var6 = witnessHosts.iterator(); var6.hasNext(); resultItems[index++] = this.createResultItem(witnessData.witnessHost, witnessData.preferredFaultDomainName, witnessData.unicastAgentAddress, true, witnessData.isMetadataWitnessHost)) {
         witnessData = (WitnessHostData)var6.next();
      }

      ManagedObjectReference hostRef;
      for(var6 = regularHosts.iterator(); var6.hasNext(); resultItems[index++] = this.createResultItem(hostRef, "", "", false, false)) {
         hostRef = (ManagedObjectReference)var6.next();
      }

      return QueryUtil.newResultSet(resultItems);
   }

   private List getHostsInCluster(VsanWitnessHostProvider.RequestData requestData) throws Exception {
      List hosts = new ArrayList();
      if (requestData.isAllHostsRelation) {
         PropertyValue[] propValues = QueryUtil.getProperties((ManagedObjectReference[])requestData.clusterRefs.toArray(new ManagedObjectReference[0]), new String[]{"host"}).getPropertyValues();
         PropertyValue[] var4 = propValues;
         int var5 = propValues.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            PropertyValue propValue = var4[var6];
            ManagedObjectReference[] clusterHosts = (ManagedObjectReference[])((ManagedObjectReference[])propValue.value);
            if (clusterHosts != null) {
               Collections.addAll(hosts, clusterHosts);
            }
         }
      }

      return hosts;
   }

   private List getWitnessHosts(List clusterRefs) {
      ArrayList allWitnessHosts = new ArrayList();

      try {
         Iterator var3 = clusterRefs.iterator();

         while(var3.hasNext()) {
            ManagedObjectReference clusterRef = (ManagedObjectReference)var3.next();
            List witnessHosts = this.stretchedClusterService.getWitnessHosts(clusterRef);
            if (witnessHosts != null) {
               allWitnessHosts.addAll(witnessHosts);
            }
         }
      } catch (Exception var6) {
         _logger.error("Could not retrieve witness hosts", var6);
      }

      return allWitnessHosts;
   }

   private ResultItem createResultItem(ManagedObjectReference moRef, String preferredFd, String unicastAgentAddress, boolean isWitnessHost, boolean isMetadataWitnessHost) {
      return QueryUtil.newResultItem(moRef, QueryUtil.newProperty("preferredFaultDomain", getNotNull(preferredFd)), QueryUtil.newProperty("unicastAgentAddress", getNotNull(unicastAgentAddress)), QueryUtil.newProperty("isWitnessHost", isWitnessHost), QueryUtil.newProperty("isMetadataWitnessHost", isMetadataWitnessHost));
   }

   private static String getNotNull(String value) {
      return value == null ? "" : value;
   }

   private class RequestData {
      public List clusterRefs = new ArrayList();
      public boolean isAllHostsRelation = false;

      public RequestData(List clusterRefs, boolean isAllHostsRelation) {
         this.clusterRefs = clusterRefs;
         this.isAllHostsRelation = isAllHostsRelation;
      }

      public RequestData() {
      }
   }
}
