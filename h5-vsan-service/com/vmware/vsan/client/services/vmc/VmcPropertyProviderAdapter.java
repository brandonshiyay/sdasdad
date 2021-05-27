package com.vmware.vsan.client.services.vmc;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VmcPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private final Log logger = LogFactory.getLog(this.getClass());
   private static final String IS_VMC = "isVmc";
   @Autowired
   private VmcService vmcService;

   public VmcPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      TypeInfo[] providedProperties = new TypeInfo[]{this.createTypeInfo(Folder.class), this.createTypeInfo(Datacenter.class), this.createTypeInfo(Datastore.class), this.createTypeInfo(HostSystem.class), this.createTypeInfo(ClusterComputeResource.class), this.createTypeInfo(VirtualMachine.class)};
      registry.registerDataAdapter(this, providedProperties);
   }

   private TypeInfo createTypeInfo(Class clazz) {
      TypeInfo objectInfo = new TypeInfo();
      objectInfo.type = clazz.getSimpleName();
      objectInfo.properties = new String[]{"isVmc"};
      return objectInfo;
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      this.logger.debug("Processing DS request: " + Utils.toString(propertyRequest));
      List resultItems = (List)Stream.of(propertyRequest.objects).map((obj) -> {
         return (ManagedObjectReference)obj;
      }).map((moRef) -> {
         return this.createResultItem(moRef, this.vmcService.isVmc(moRef.getServerGuid()));
      }).collect(Collectors.toList());
      this.logger.debug("Returning DS response: " + Utils.toString(resultItems));
      return QueryUtil.newResultSet((ResultItem[])resultItems.toArray(new ResultItem[0]));
   }

   private ResultItem createResultItem(ManagedObjectReference moRef, Boolean isVmc) {
      PropertyValue propertyValue = new PropertyValue();
      propertyValue.propertyName = "isVmc";
      propertyValue.value = isVmc;
      ResultItem resultItem = new ResultItem();
      resultItem.resourceObject = moRef;
      resultItem.properties = new PropertyValue[]{propertyValue};
      return resultItem;
   }
}
