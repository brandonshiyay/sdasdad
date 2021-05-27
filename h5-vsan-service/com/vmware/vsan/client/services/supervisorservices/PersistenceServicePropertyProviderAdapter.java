package com.vmware.vsan.client.services.supervisorservices;

import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PersistenceServicePropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final Log logger = LogFactory.getLog(PersistenceServicePropertyProviderAdapter.class);
   private static final String PROP_IS_PERSISTENCE_SERVICE_ENABLED = "isPersistenceServiceEnabled";
   private static final String ADV_OPT_IS_HOST_AFFINITY_ENABLED = "config.vpxd.vsan.hostaffinity.enable";
   @Autowired
   private VcClient vcClient;

   public PersistenceServicePropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo folderInfo = new TypeInfo();
      folderInfo.type = Folder.class.getSimpleName();
      folderInfo.properties = new String[]{"isPersistenceServiceEnabled"};
      TypeInfo[] providedProperties = new TypeInfo[]{folderInfo};
      registry.registerDataAdapter(this, providedProperties);
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      Measure measure = new Measure("OptionManager.queryView");
      Throwable var4 = null;

      List resultItems;
      try {
         Map futures = (Map)Arrays.stream(propertyRequest.objects).map((obj) -> {
            return (ManagedObjectReference)obj;
         }).collect(Collectors.toMap((moRef) -> {
            return moRef;
         }, (moRef) -> {
            return this.getConfigOption(measure, moRef, "config.vpxd.vsan.hostaffinity.enable");
         }));
         Map results = (Map)futures.entrySet().stream().collect(Collectors.toMap(Entry::getKey, (e) -> {
            return this.isPersistenceServiceEnabled((Future)e.getValue());
         }));
         resultItems = (List)results.entrySet().stream().map((entry) -> {
            return this.createResultItem((ManagedObjectReference)entry.getKey(), (Boolean)entry.getValue());
         }).collect(Collectors.toList());
      } catch (Throwable var14) {
         var4 = var14;
         throw var14;
      } finally {
         if (measure != null) {
            if (var4 != null) {
               try {
                  measure.close();
               } catch (Throwable var13) {
                  var4.addSuppressed(var13);
               }
            } else {
               measure.close();
            }
         }

      }

      return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
   }

   private Future getConfigOption(Measure measure, ManagedObjectReference vcRef, String option) {
      Future future = measure.newFuture(vcRef.toString());
      VcConnection conn = this.vcClient.getConnection(vcRef.getServerGuid());
      Throwable var6 = null;

      try {
         OptionManager optionManager = conn.getOptionManager();
         optionManager.queryView(option, future);
      } catch (Throwable var15) {
         var6 = var15;
         throw var15;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var14) {
                  var6.addSuppressed(var14);
               }
            } else {
               conn.close();
            }
         }

      }

      return future;
   }

   private boolean isPersistenceServiceEnabled(Future future) {
      try {
         OptionValue[] optionValues = (OptionValue[])future.get();
         if (ArrayUtils.isEmpty(optionValues)) {
            return false;
         } else {
            boolean isHostAffinityAdvancedOptionEnabled = (Boolean)Optional.ofNullable(optionValues[0].value).map((val) -> {
               return Boolean.parseBoolean(val.toString().trim().toLowerCase());
            }).orElse(false);
            return !isHostAffinityAdvancedOptionEnabled;
         }
      } catch (Exception var4) {
         logger.warn("Cannot retrieve advanced option 'config.vpxd.vsan.hostaffinity.enable': ", var4);
         return true;
      }
   }

   private ResultItem createResultItem(ManagedObjectReference moRef, Boolean isPersistenceServiceEnabled) {
      PropertyValue propertyValue = new PropertyValue();
      propertyValue.propertyName = "isPersistenceServiceEnabled";
      propertyValue.value = isPersistenceServiceEnabled;
      ResultItem resultItem = new ResultItem();
      resultItem.resourceObject = moRef;
      resultItem.properties = new PropertyValue[]{propertyValue};
      return resultItem;
   }
}
