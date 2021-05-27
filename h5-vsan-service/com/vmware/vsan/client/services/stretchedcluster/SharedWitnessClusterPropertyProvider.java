package com.vmware.vsan.client.services.stretchedcluster;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.QueryModel;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

public class SharedWitnessClusterPropertyProvider extends VsanBasePropertyProviderAdapter {
   private static final String PROPERTY_IS_SHARED_WITNESS_AVAILABLE_FOR_CLUSTER = "isSharedWitnessAvailableForCluster";
   private static final Log logger = LogFactory.getLog(SharedWitnessClusterPropertyProvider.class);
   @Autowired
   private QueryExecutor queryExecutor;
   @Autowired
   private SharedWitnessHelper sharedWitnessHelper;

   public SharedWitnessClusterPropertyProvider(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo clusterInfo = new TypeInfo();
      clusterInfo.type = ClusterComputeResource.class.getSimpleName();
      clusterInfo.properties = new String[]{"isSharedWitnessAvailableForCluster"};
      TypeInfo[] providedProperties = new TypeInfo[]{clusterInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      List clustersRefs = (List)Arrays.stream(propertyRequest.objects).map((object) -> {
         return (ManagedObjectReference)object;
      }).filter(this::isCluster).collect(Collectors.toList());
      if (!CollectionUtils.isEmpty(clustersRefs) && !this.areClustersFromDifferentVc(clustersRefs) && VsanCapabilityUtils.isSharedWitnessSupportedOnVc((ManagedObjectReference)clustersRefs.get(0))) {
         Measure measure = new Measure("Retrieving witness hosts");
         Throwable var5 = null;

         Map witnessHostsFutures;
         try {
            witnessHostsFutures = this.sharedWitnessHelper.getWitnessHostsFutures(clustersRefs, measure);
         } catch (Throwable var14) {
            var5 = var14;
            throw var14;
         } finally {
            if (measure != null) {
               if (var5 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var13) {
                     var5.addSuppressed(var13);
                  }
               } else {
                  measure.close();
               }
            }

         }

         boolean areClustersEligibleForSharedWitness = this.areAllClustersTwoNode(clustersRefs) && this.isSharedWitnessSupportedOnAllClusters(clustersRefs) && this.areAllClustersStretched(clustersRefs, witnessHostsFutures);
         return this.getResultSet(propertyRequest, areClustersEligibleForSharedWitness);
      } else {
         logger.warn("There is a cluster that doesn't match all shared witness conditions. All clusters' properties are set to false.");
         return this.getResultSet(propertyRequest, false);
      }
   }

   private boolean isCluster(ManagedObjectReference moRef) {
      return moRef != null && ClusterComputeResource.class.getSimpleName().equals(moRef.getType());
   }

   private boolean areClustersFromDifferentVc(List clustersRefs) {
      return clustersRefs.stream().map((clusterRef) -> {
         return clusterRef.getServerGuid();
      }).distinct().count() > 1L;
   }

   private boolean areAllClustersTwoNode(List clustersRefs) {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select("host._length").from((Collection)clustersRefs).end().build();
      QueryExecutorResult queryExecutorResult = this.queryExecutor.execute(requestSpec);
      return queryExecutorResult.getQueryResult().items.stream().filter(this::isClusterTwoNode).count() == (long)clustersRefs.size();
   }

   private boolean isClusterTwoNode(QueryModel queryModel) {
      if (queryModel != null && queryModel.properties != null) {
         Map props = queryModel.properties;
         Integer clusterHostCount = (Integer)props.get("host._length");
         return clusterHostCount != null && clusterHostCount.equals(2);
      } else {
         logger.error("Cannot query cluster's properties");
         return false;
      }
   }

   private boolean isSharedWitnessSupportedOnAllClusters(List clustersRefs) {
      Iterator var2 = clustersRefs.iterator();

      ManagedObjectReference clusterRef;
      do {
         if (!var2.hasNext()) {
            return true;
         }

         clusterRef = (ManagedObjectReference)var2.next();
      } while(VsanCapabilityUtils.isSharedWitnessSupported(clusterRef));

      return false;
   }

   private boolean areAllClustersStretched(List clustersRefs, Map witnessHostsFutures) {
      Iterator var3 = clustersRefs.iterator();

      while(var3.hasNext()) {
         ManagedObjectReference clusterRef = (ManagedObjectReference)var3.next();

         ManagedObjectReference witnessHostRef;
         try {
            witnessHostRef = this.sharedWitnessHelper.findWitnessHost(witnessHostsFutures, clusterRef);
         } catch (Exception var7) {
            logger.error("Failed to retrieve clusters' witness hosts", var7);
            return false;
         }

         if (witnessHostRef == null) {
            return false;
         }
      }

      return true;
   }

   private ResultSet getResultSet(PropertyRequestSpec propertyRequest, boolean areClustersEligibleForSharedWitness) {
      List resultItems = new ArrayList();
      Object[] var4 = propertyRequest.objects;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Object objectRef = var4[var6];
         ManagedObjectReference moRef = (ManagedObjectReference)objectRef;
         if (objectRef != null) {
            ResultItem resultItem = null;
            if (ClusterComputeResource.class.getSimpleName().equals(moRef.getType())) {
               PropertyValue[] clusterProperties = this.getClusterProperties(propertyRequest.properties, objectRef, areClustersEligibleForSharedWitness);
               resultItem = QueryUtil.newResultItem(objectRef, clusterProperties);
            }

            resultItems.add(resultItem);
         }
      }

      return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[resultItems.size()]));
   }

   private PropertyValue[] getClusterProperties(PropertySpec[] properties, Object objectRef, boolean isClusterEligibleForSharedWitness) {
      List propValues = new ArrayList();
      if (QueryUtil.isAnyPropertyRequested(properties, "isSharedWitnessAvailableForCluster")) {
         PropertyValue propValue = QueryUtil.newProperty("isSharedWitnessAvailableForCluster", isClusterEligibleForSharedWitness);
         propValue.resourceObject = objectRef;
         propValues.add(propValue);
      }

      return (PropertyValue[])propValues.toArray(new PropertyValue[0]);
   }
}
