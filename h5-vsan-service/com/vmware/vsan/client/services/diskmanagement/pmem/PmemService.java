package com.vmware.vsan.client.services.diskmanagement.pmem;

import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.Datastore.HostMount;
import com.vmware.vim.binding.vim.Datastore.Summary;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.diskmanagement.StorageCapacity;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsphere.client.vsan.health.DatastoreHealthStatus;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PmemService {
   private static final Logger logger = LoggerFactory.getLogger(PmemService.class);
   @Autowired
   private QueryExecutor queryExecutor;

   public Map getPmemStorage(ManagedObjectReference clusterRef, boolean onlyManageableStorage) {
      if (!VsanCapabilityUtils.isManagedPMemSupportedOnVC(clusterRef)) {
         return new HashMap();
      } else {
         DataServiceResponse response = this.queryExecutor.execute(this.getPmemStorageSpec(clusterRef)).getDataServiceResponse();
         return (Map)response.getResourceObjects().stream().map((dsRef) -> {
            return this.getPmemStorage(response, dsRef);
         }).filter(Objects::nonNull).filter((storage) -> {
            return !onlyManageableStorage || storage.isManageableByVsan;
         }).collect(HashMap::new, (result, storage) -> {
            List var10000 = (List)result.put(storage.hostRef, this.getHostPmemStorage(result, storage));
         }, HashMap::putAll);
      }
   }

   private RequestSpec getPmemStorageSpec(ManagedObjectReference clusterRef) {
      return (new QueryBuilder()).newQuery().select("summary", "host", "info.pmem.uuid", "overallStatus").from(clusterRef).join(Datastore.class).on("datastore").where().propertyEquals("summary.type", "PMEM").end().build();
   }

   private PmemStorage getPmemStorage(DataServiceResponse dsProperties, ManagedObjectReference dsRef) {
      if (!this.validate(dsProperties, dsRef)) {
         logger.error("Failed to load PMem datastore " + dsRef);
         return null;
      } else {
         String uuid = (String)dsProperties.getProperty(dsRef, "info.pmem.uuid");
         String overallStatus = dsProperties.getProperty(dsRef, "overallStatus").toString();
         Summary summary = (Summary)dsProperties.getProperty(dsRef, "summary");
         HostMount[] hostMounts = (HostMount[])dsProperties.getProperty(dsRef, "host");
         ManagedObjectReference hostRef = hostMounts[0].key;
         PmemStorage storage = new PmemStorage();
         storage.dsRef = dsRef;
         storage.hostRef = hostRef;
         storage.name = summary.name;
         storage.uuid = uuid;
         storage.overallStatus = DatastoreHealthStatus.parse(overallStatus);
         storage.isAccessible = summary.accessible;
         storage.isMounted = hostMounts[0].mountInfo.mounted;
         storage.capacity = new StorageCapacity(summary.capacity, summary.capacity - summary.freeSpace, (Long)null);
         storage.isManageableByVsan = VsanCapabilityUtils.isManagedPMemSupported(hostRef);
         return storage;
      }
   }

   private List getHostPmemStorage(Map hostToPmemStorage, PmemStorage storage) {
      List pmemStorage = hostToPmemStorage.containsKey(storage.hostRef) ? (List)hostToPmemStorage.get(storage.hostRef) : new ArrayList();
      ((List)pmemStorage).add(storage);
      return (List)pmemStorage;
   }

   private boolean validate(DataServiceResponse dsProperties, ManagedObjectReference dsRef) {
      boolean summaryIsValid = dsProperties.hasProperty(dsRef, "summary") && dsProperties.getProperty(dsRef, "summary") != null;
      boolean hostIsValid = dsProperties.hasProperty(dsRef, "host") && ArrayUtils.isNotEmpty((HostMount[])((HostMount[])dsProperties.getProperty(dsRef, "host"))) && ((HostMount[])((HostMount[])dsProperties.getProperty(dsRef, "host"))).length == 1;
      boolean uuidIsNotEmpty = dsProperties.hasProperty(dsRef, "info.pmem.uuid") && dsProperties.getProperty(dsRef, "info.pmem.uuid") != null;
      boolean statusIsNotEmpty = dsProperties.hasProperty(dsRef, "overallStatus") && dsProperties.getProperty(dsRef, "overallStatus") != null;
      return summaryIsValid && hostIsValid && uuidIsNotEmpty && statusIsNotEmpty;
   }
}
