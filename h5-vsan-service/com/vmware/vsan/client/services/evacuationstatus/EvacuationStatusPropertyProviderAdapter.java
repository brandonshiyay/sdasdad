package com.vmware.vsan.client.services.evacuationstatus;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EvacuationStatusPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final String VSAN_HOST_EVACUATION_STATUS_SUPPORTED_ON_VC = "evacuationStatusSupportedOnVc";
   private static final String VSAN_HOST_EVACUATION_STATUS_SUPPORTED_ON_CLUSTER = "evacuationStatusSupportedOnCluster";
   private static final Log logger = LogFactory.getLog(EvacuationStatusPropertyProviderAdapter.class);

   @Autowired
   public void setDataServiceExtensionRegistry(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo clusterInfo = new TypeInfo();
      clusterInfo.type = ClusterComputeResource.class.getSimpleName();
      clusterInfo.properties = new String[]{"evacuationStatusSupportedOnVc", "evacuationStatusSupportedOnCluster"};
      TypeInfo[] providedProperties = new TypeInfo[]{clusterInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      ResultSet result = new ResultSet();
      result.items = new ResultItem[0];
      PropertySpec[] var3 = propertyRequest.properties;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         PropertySpec propertySpec = var3[var5];
         String[] var7 = propertySpec.propertyNames;
         int var8 = var7.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            String propertyName = var7[var9];

            try {
               byte var12 = -1;
               switch(propertyName.hashCode()) {
               case -1923978507:
                  if (propertyName.equals("evacuationStatusSupportedOnVc")) {
                     var12 = 0;
                  }
                  break;
               case -1566629198:
                  if (propertyName.equals("evacuationStatusSupportedOnCluster")) {
                     var12 = 1;
                  }
               }

               switch(var12) {
               case 0:
                  result.items = (ResultItem[])((ResultItem[])ArrayUtils.addAll(result.items, this.isPreCheckSupported(propertyRequest.objects)));
                  break;
               case 1:
                  result.items = (ResultItem[])((ResultItem[])ArrayUtils.addAll(result.items, this.isPreCheckSupportedOnCluster(propertyRequest.objects)));
                  break;
               default:
                  throw new IllegalArgumentException("Unexpected property name: " + propertyName);
               }
            } catch (Exception var13) {
               logger.error("Incorrect property requested : ", var13);
               result.error = var13;
            }
         }
      }

      return result;
   }

   private ResultItem[] isPreCheckSupported(Object[] targetObjects) {
      ManagedObjectReference[] clusterRefs = (ManagedObjectReference[])Arrays.copyOf(targetObjects, targetObjects.length, ManagedObjectReference[].class);
      ArrayList result = new ArrayList();
      ManagedObjectReference[] var4 = clusterRefs;
      int var5 = clusterRefs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference clusterRef = var4[var6];
         ManagedObjectReference vcRef = VmodlHelper.getRootFolder(clusterRef.getServerGuid());
         if (!VsanCapabilityUtils.isHostResourcePrecheckSupportedOnVc(vcRef)) {
            result.add(QueryUtil.createResultItem("evacuationStatusSupportedOnVc", false, clusterRef));
         } else {
            result.add(QueryUtil.createResultItem("evacuationStatusSupportedOnVc", true, clusterRef));
         }
      }

      return (ResultItem[])result.toArray(new ResultItem[0]);
   }

   private ResultItem[] isPreCheckSupportedOnCluster(Object[] targetObjects) {
      ManagedObjectReference[] clusterRefs = (ManagedObjectReference[])Arrays.copyOf(targetObjects, targetObjects.length, ManagedObjectReference[].class);
      ArrayList result = new ArrayList();
      ManagedObjectReference[] var4 = clusterRefs;
      int var5 = clusterRefs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference clusterRef = var4[var6];
         result.add(QueryUtil.createResultItem("evacuationStatusSupportedOnCluster", VsanCapabilityUtils.isHostResourcePrecheckSupported(clusterRef), clusterRef));
      }

      return (ResultItem[])result.toArray(new ResultItem[0]);
   }
}
