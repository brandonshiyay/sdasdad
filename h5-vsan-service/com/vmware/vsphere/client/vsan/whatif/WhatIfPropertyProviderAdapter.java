package com.vmware.vsphere.client.vsan.whatif;

import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.vsan.host.DecommissionMode.ObjectAction;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.ParameterSpec;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class WhatIfPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final String VSAN_HOST_WHAT_IF_RESULT = "hostWhatIfResult";
   private static final Log _logger = LogFactory.getLog(WhatIfPropertyProviderAdapter.class);
   @Autowired
   private WhatIfPropertyProvider whatIfProvider;

   public WhatIfPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo hostInfo = new TypeInfo();
      hostInfo.type = HostSystem.class.getSimpleName();
      hostInfo.properties = new String[]{"hostWhatIfResult"};
      TypeInfo[] providedProperties = new TypeInfo[]{hostInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      ResultSet resultSet;
      if (!this.isValidRequest(propertyRequest)) {
         resultSet = new ResultSet();
         resultSet.totalMatchedObjectCount = 0;
         return resultSet;
      } else {
         List resultItems = new ArrayList();
         WhatIfSpec spec = new WhatIfSpec();
         PropertySpec[] var5 = propertyRequest.properties;
         int var6 = var5.length;

         int var7;
         for(var7 = 0; var7 < var6; ++var7) {
            PropertySpec propertySpec = var5[var7];
            if (ArrayUtils.isNotEmpty(propertySpec.parameters)) {
               ParameterSpec[] var9 = propertySpec.parameters;
               int var10 = var9.length;

               for(int var11 = 0; var11 < var10; ++var11) {
                  ParameterSpec parameterSpec = var9[var11];
                  if ("hostWhatIfResult".equals(parameterSpec.propertyName)) {
                     spec.clusterRef = (ManagedObjectReference)parameterSpec.parameter;
                  }
               }
            }
         }

         Object[] var15 = propertyRequest.objects;
         var6 = var15.length;

         for(var7 = 0; var7 < var6; ++var7) {
            Object host = var15[var7];

            try {
               ManagedObjectReference hostRef = (ManagedObjectReference)host;
               WhatIfResult whatIfResult = this.whatIfProvider.getWhatIfResult(hostRef, spec);
               Map result = new HashMap();
               if (whatIfResult.isWhatIfSupported) {
                  result = this.createWhatIfResultObject(whatIfResult);
               } else {
                  ((Map)result).put("isWhatIfSupported", false);
               }

               PropertyValue resultPropValue = QueryUtil.newProperty("hostWhatIfResult", result);
               ResultItem resultItem = QueryUtil.newResultItem(hostRef, resultPropValue);
               resultItems.add(resultItem);
            } catch (Exception var14) {
               _logger.error("Failed to retrieve hostWhatIfResult property. ", var14);
               resultSet = new ResultSet();
               resultSet.error = var14;
               return resultSet;
            }
         }

         resultSet = QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
         return resultSet;
      }
   }

   private Map createWhatIfResultObject(WhatIfResult whatIfResult) {
      Map result = new HashMap();
      List ensureAccessibilityMap = new ArrayList();
      ensureAccessibilityMap.add(whatIfResult.ensureAccessibility.summary);
      ensureAccessibilityMap.add(whatIfResult.ensureAccessibility.successWithoutDataLoss);
      ensureAccessibilityMap.add(whatIfResult.ensureAccessibility.successWithInaccessibleOrNonCompliantObjects);
      ensureAccessibilityMap.add(whatIfResult.ensureAccessibility.repairTime);
      List fullDataMigrationMap = new ArrayList();
      fullDataMigrationMap.add(whatIfResult.fullDataMigration.summary);
      fullDataMigrationMap.add(whatIfResult.fullDataMigration.successWithoutDataLoss);
      fullDataMigrationMap.add(whatIfResult.fullDataMigration.successWithInaccessibleOrNonCompliantObjects);
      fullDataMigrationMap.add(whatIfResult.fullDataMigration.repairTime);
      List noDataMigrationMap = new ArrayList();
      noDataMigrationMap.add(whatIfResult.noDataMigration.summary);
      noDataMigrationMap.add(whatIfResult.noDataMigration.successWithoutDataLoss);
      noDataMigrationMap.add(whatIfResult.noDataMigration.successWithInaccessibleOrNonCompliantObjects);
      noDataMigrationMap.add(whatIfResult.noDataMigration.repairTime);
      result.put("isWhatIfSupported", true);
      result.put(ObjectAction.ensureObjectAccessibility, ensureAccessibilityMap);
      result.put(ObjectAction.evacuateAllData, fullDataMigrationMap);
      result.put(ObjectAction.noAction, noDataMigrationMap);
      return result;
   }

   private boolean isValidRequest(PropertyRequestSpec propertyRequest) {
      if (propertyRequest == null) {
         return false;
      } else if (!ArrayUtils.isEmpty(propertyRequest.objects) && !ArrayUtils.isEmpty(propertyRequest.properties)) {
         return true;
      } else {
         _logger.error("Property provider adapter got a null or empty list of properties or objects");
         return false;
      }
   }
}
