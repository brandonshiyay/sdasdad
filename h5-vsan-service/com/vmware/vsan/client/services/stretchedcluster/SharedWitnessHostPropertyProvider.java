package com.vmware.vsan.client.services.stretchedcluster;

import com.google.common.collect.Maps;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ClusterRuntimeInfo;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SharedWitnessHostPropertyProvider extends VsanBasePropertyProviderAdapter {
   private static final String IS_SHARED_WITNESS_SUPPORTED_PROPERTY = "isSharedWitnessHostSupported";
   private static final Log logger = LogFactory.getLog(SharedWitnessHostPropertyProvider.class);
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanInventoryHelper vsanInventoryHelper;
   @Autowired
   private VmodlHelper vmodlHelper;

   public SharedWitnessHostPropertyProvider(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo hostInfo = new TypeInfo();
      hostInfo.type = HostSystem.class.getSimpleName();
      hostInfo.properties = new String[]{"isSharedWitnessHostSupported"};
      registry.registerDataAdapter(this, new TypeInfo[]{hostInfo});
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      List hosts = new ArrayList(QueryUtil.getObjectRefs(propertyRequest.objects));
      String[] requestedProperties = QueryUtil.getPropertyNames(propertyRequest.properties);

      try {
         Measure measure = new Measure("Retrieve witness host properties");
         Throwable var22 = null;

         ResultSet var9;
         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, (ManagedObjectReference)null);
            String[] propertiesToGetFromDS = new String[]{"config.vsanHostConfig.enabled", "parent"};
            this.loadData(hosts, requestedProperties, dataRetriever);
            DataServiceResponse propsFromDS = QueryUtil.getProperties((ManagedObjectReference[])hosts.toArray(new ManagedObjectReference[0]), propertiesToGetFromDS);
            var9 = this.getPropertiesResult(requestedProperties, dataRetriever, propsFromDS);
         } catch (Throwable var19) {
            var22 = var19;
            throw var19;
         } finally {
            if (measure != null) {
               if (var22 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var18) {
                     var22.addSuppressed(var18);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var9;
      } catch (Exception var21) {
         logger.error("Failed to fetch the requested properties for shared witness host: " + var21);
         ResultSet resultSet = new ResultSet();
         resultSet.error = var21;
         return resultSet;
      }
   }

   private void loadData(List hostRefs, String[] properties, VsanAsyncDataRetriever dataRetriever) {
      Arrays.stream(properties).forEach((property) -> {
         if ("isSharedWitnessHostSupported".equals(property)) {
            dataRetriever.loadAssignedSharedWitnessClusters(hostRefs).loadAreHostsWitnessVirtualAppliances(hostRefs);
         }

      });
   }

   private ResultSet getPropertiesResult(String[] properties, VsanAsyncDataRetriever dataRetriever, DataServiceResponse propsFromDS) {
      ResultItem[] resultItems = (ResultItem[])propsFromDS.getResourceObjects().stream().map((hostRefx) -> {
         ResultItem resultItem = new ResultItem();
         resultItem.resourceObject = hostRefx;
         resultItem.properties = new PropertyValue[properties.length];
         return resultItem;
      }).toArray((x$0) -> {
         return new ResultItem[x$0];
      });

      for(int propertyIndex = 0; propertyIndex < properties.length; ++propertyIndex) {
         String property = properties[propertyIndex];
         Map hostToPropertyValue = new HashMap();
         if (property.equals("isSharedWitnessHostSupported")) {
            hostToPropertyValue = this.getHostsSupportSharedWitness(dataRetriever, propsFromDS);
         } else {
            logger.warn("Skipping unknown property: " + property);
         }

         ResultItem[] var8 = resultItems;
         int var9 = resultItems.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            ResultItem resultItem = var8[var10];
            ManagedObjectReference hostRef = (ManagedObjectReference)resultItem.resourceObject;
            resultItem.properties[propertyIndex] = QueryUtil.createPropValue(property, ((Map)hostToPropertyValue).get(hostRef), hostRef);
         }
      }

      return QueryUtil.newResultSet(resultItems);
   }

   private Map getHostsSupportSharedWitness(VsanAsyncDataRetriever dataRetriever, DataServiceResponse propsFromDS) {
      Map hostWitnessApplianceResults = dataRetriever.getAreHostsWitnessVirtualAppliances();
      Map assignedSharedWitnessClustersResults = dataRetriever.getAssignedSharedWitnessClusters();
      return (Map)propsFromDS.getResourceObjects().stream().map((hostRef) -> {
         List assignedClustersInfo = (List)assignedSharedWitnessClustersResults.get(hostRef);
         if (assignedClustersInfo == null) {
            return Maps.immutableEntry(hostRef, false);
         } else {
            boolean isHostVsanEnabled = (Boolean)propsFromDS.getProperty(hostRef, "config.vsanHostConfig.enabled");
            ManagedObjectReference hostParent = (ManagedObjectReference)propsFromDS.getProperty(hostRef, "parent");
            boolean isHostInCluster = this.vmodlHelper.isOfType(hostParent, ClusterComputeResource.class);
            if (assignedClustersInfo.isEmpty()) {
               return !isHostVsanEnabled && !isHostInCluster ? Maps.immutableEntry(hostRef, hostWitnessApplianceResults.get(hostRef)) : Maps.immutableEntry(hostRef, false);
            } else {
               if (assignedClustersInfo.size() == 1) {
                  int clusterHostsCount = this.vsanInventoryHelper.getNumberOfClusterHosts(VmodlHelper.assignServerGuid(((ClusterRuntimeInfo)assignedClustersInfo.get(0)).getCluster(), hostRef.getServerGuid()));
                  if (clusterHostsCount != 2) {
                     return Maps.immutableEntry(hostRef, false);
                  }
               }

               return Maps.immutableEntry(hostRef, true);
            }
         }
      }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
   }
}
