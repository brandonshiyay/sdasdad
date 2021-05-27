package com.vmware.vsan.client.services.virtualobjects;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.ObjectIdentityConstraint;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RelationalConstraint;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class VsanVirtualObjectsProvider {
   private static final Log _logger = LogFactory.getLog(VsanVirtualObjectsProvider.class);
   private static final String COMPOSITE_UUID = "compositeUuid";
   private final Map times = new HashMap();

   public Set getVirtualObjectsUuids(ManagedObjectReference clusterRef) throws Exception {
      this.startTimer("getVirtualObjectsUuids");
      String[] jsonProperties = new String[]{"vsanPhysicalDiskVirtualMapping"};
      QuerySpec querySpec = this.getClusterHostsQuerySpec(clusterRef, "host", jsonProperties);
      ResultItem[] resultItems = QueryUtil.getData(querySpec).items;
      this.stopTimer("getVirtualObjectsUuids");
      if (resultItems == null) {
         return Collections.emptySet();
      } else {
         Set vsanUuids = new HashSet();
         ResultItem[] var6 = resultItems;
         int var7 = resultItems.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            ResultItem resultItem = var6[var8];
            JsonNode hostJsonData = this.getHostJsonData(resultItem);
            if (hostJsonData != null) {
               List hostVsanUuids = hostJsonData.findValuesAsText("compositeUuid");
               vsanUuids.addAll(hostVsanUuids);
            }
         }

         return vsanUuids;
      }
   }

   private QuerySpec getClusterHostsQuerySpec(ManagedObjectReference clusterRef, String relation, String[] properties) {
      ObjectIdentityConstraint clusterConstraint = QueryUtil.createObjectIdentityConstraint(clusterRef);
      RelationalConstraint clusterHostsConstraint = QueryUtil.createRelationalConstraint(relation, clusterConstraint, true, HostSystem.class.getSimpleName());
      QuerySpec querySpecHosts = QueryUtil.buildQuerySpec((Constraint)clusterHostsConstraint, properties);
      return querySpecHosts;
   }

   private JsonNode getHostJsonData(ResultItem resultItem) {
      PropertyValue[] var2 = resultItem.properties;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PropertyValue propValue = var2[var4];
         if ("vsanPhysicalDiskVirtualMapping".equals(propValue.propertyName)) {
            return Utils.getJsonRootNode((String)propValue.value);
         }
      }

      return null;
   }

   private void startTimer(String timerName) {
      long startTime = System.currentTimeMillis();
      if (!this.times.containsKey(timerName)) {
         this.times.remove(timerName);
      }

      this.times.put(timerName, startTime);
   }

   private void stopTimer(String timerName) {
      if (!this.times.containsKey(timerName)) {
         _logger.info("No start time for " + timerName);
      } else {
         _logger.info(timerName + " total time: " + (System.currentTimeMillis() - (Long)this.times.get(timerName)));
      }
   }
}
