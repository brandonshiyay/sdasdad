package com.vmware.vsan.client.services.spbm;

import com.vmware.vim.binding.pbm.profile.CapabilityBasedProfileCreateSpec;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.host.VsanInternalSystem.PolicySatisfiability;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.ParameterSpec;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpbmPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final Log logger = LogFactory.getLog(SpbmPropertyProviderAdapter.class);
   private static final String POLICY_SATISFIABILITY_PROPERTY = "policySatisfiability";
   private static final String POLICY_CREATE_SPEC_FIELD = "createProfileSpec";
   private static final String DATASTORE_FIELD = "datastore";
   private static final String CAPACITY_SIZE_FIELD = "capacityInBytes";
   @Autowired
   private SpbmService spbmService;

   public SpbmPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      Validate.notNull(registry);
      registry.registerDataAdapter(this, this.providedProperties());
   }

   private TypeInfo[] providedProperties() {
      return new TypeInfo[]{this.getPolicySatisfiabilityProperty()};
   }

   private TypeInfo getPolicySatisfiabilityProperty() {
      TypeInfo info = new TypeInfo();
      info.type = Folder.class.getSimpleName();
      info.properties = new String[]{"policySatisfiability"};
      return info;
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequestSpec) {
      try {
         List resultItems = new ArrayList();
         List moRefs = new ArrayList(QueryUtil.getObjectRefs(propertyRequestSpec.objects));
         Iterator var4 = moRefs.iterator();

         while(var4.hasNext()) {
            ManagedObjectReference moRef = (ManagedObjectReference)var4.next();
            PropertySpec[] var6 = propertyRequestSpec.properties;
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               PropertySpec propertySpec = var6[var8];
               String[] var10 = propertySpec.propertyNames;
               int var11 = var10.length;

               for(int var12 = 0; var12 < var11; ++var12) {
                  String property = var10[var12];
                  byte var15 = -1;
                  switch(property.hashCode()) {
                  case -2014518455:
                     if (property.equals("policySatisfiability")) {
                        var15 = 0;
                     }
                  }

                  switch(var15) {
                  case 0:
                     Object parameter = this.findParameter(propertySpec, property);
                     resultItems.add(this.getPolicySatisfiability(moRef, parameter));
                     break;
                  default:
                     logger.warn("Unknown property: " + property);
                  }
               }
            }
         }

         return QueryUtil.newResultSet((ResultItem[])resultItems.stream().filter(Objects::nonNull).toArray((x$0) -> {
            return new ResultItem[x$0];
         }));
      } catch (Exception var17) {
         logger.error("Could not fetch the requested properties: " + var17);
         return QueryUtil.newResultSetWithErrors(var17);
      }
   }

   private Object findParameter(PropertySpec property, String propertyName) {
      ParameterSpec parameterSpec = (ParameterSpec)Arrays.stream(property.parameters).filter((param) -> {
         return param.propertyName.equals(propertyName);
      }).findFirst().orElse((Object)null);
      return parameterSpec != null ? parameterSpec.parameter : null;
   }

   private ResultItem getPolicySatisfiability(ManagedObjectReference moRef, Object payload) throws Exception {
      if (!VsanCapabilityUtils.isPolicySatisfiabilitySupportedOnVc(moRef)) {
         logger.warn("Getting policy satisfiability is not supported on the VC.");
         return null;
      } else if (payload == null) {
         logger.warn("Could not get policy satisfiability details due to missing input data");
         return null;
      } else {
         CapabilityBasedProfileCreateSpec policyCreateSpec = (CapabilityBasedProfileCreateSpec)this.valueOf(payload, "createProfileSpec");
         ManagedObjectReference datastoreRef = (ManagedObjectReference)this.valueOf(payload, "datastore");
         Long capacityInBytes = (Long)this.valueOf(payload, "capacityInBytes");
         if (policyCreateSpec != null && datastoreRef != null && capacityInBytes != null) {
            String policy = this.spbmService.getPolicyAsStringXml(moRef.getServerGuid(), policyCreateSpec, datastoreRef);
            PolicySatisfiability policySatisfiability = this.spbmService.getPolicySatisfiability(moRef.getServerGuid(), policy, capacityInBytes);
            return QueryUtil.createResultItem("policySatisfiability", this.toCommonPolicyConsumptionModel(policySatisfiability), moRef);
         } else {
            logger.warn("Could not get policy satisfiability details due to missing input data");
            return null;
         }
      }
   }

   private Object toCommonPolicyConsumptionModel(final PolicySatisfiability policySatisfiability) {
      return new Object() {
         public Object cost = policySatisfiability;
      };
   }

   private Object valueOf(Object obj, String fieldName) {
      try {
         return obj.getClass().getField(fieldName).get(obj);
      } catch (Exception var4) {
         logger.warn("Could not get field '" + fieldName + "' in " + obj.toString());
         return null;
      }
   }
}
