package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.VirtualMachine.PowerState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataServiceHelper {
   @Autowired
   private QueryExecutor queryExecutor;

   public DataServiceResponse getHostsWithConnectionState(ManagedObjectReference clusterRef) {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select("runtime.connectionState").from(clusterRef).join(HostSystem.class).on("host").end().build();
      return this.queryExecutor.execute(requestSpec).getDataServiceResponse();
   }

   public List getVmsOnlyWithoutTemplates(ManagedObjectReference hostRef) throws Exception {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select().from(hostRef).join(VirtualMachine.class).on("vm").where().propertyEquals("config.template", false).end().build();
      QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
      List vms = new ArrayList(result.getQueryResult().getResourceObjects());
      return vms;
   }

   public boolean isAllHostVMsPoweredOff(ManagedObjectReference hostRef) {
      RequestSpec requestSpec = (new QueryBuilder()).newQuery().select("powerState").from(hostRef).join(VirtualMachine.class).on("vm").where().propertyEquals("powerState", PowerState.poweredOn).end().build();
      QueryExecutorResult resultSet = this.queryExecutor.execute(requestSpec);
      return resultSet.getQueryResult().items.size() == 0;
   }

   public boolean isHostConnected(ManagedObjectReference hostRef) throws Exception {
      return ConnectionState.connected.equals(this.getHostState(hostRef));
   }

   public ConnectionState getHostState(ManagedObjectReference hostRef) throws Exception {
      return (ConnectionState)QueryUtil.getProperty(hostRef, "runtime.connectionState");
   }
}
