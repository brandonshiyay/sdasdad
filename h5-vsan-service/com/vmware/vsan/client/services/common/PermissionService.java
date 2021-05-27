package com.vmware.vsan.client.services.common;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.ParameterSpec;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PermissionService {
   private Logger logger = LoggerFactory.getLogger(PermissionService.class);
   private static final String HAS_PRIVILEGES = "hasPrivileges";
   @Autowired
   ObjectReferenceService objectReferenceService;

   @TsService
   public Map queryPermissions(ManagedObjectReference objRef, String[] privileges) {
      Map result = new HashMap();
      if (privileges != null && privileges.length != 0) {
         String[] var4 = privileges;
         int var5 = privileges.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            String pv = var4[var6];

            try {
               result.put(pv, this.hasPermissions(objRef, new String[]{pv}));
            } catch (Exception var9) {
               this.logger.error("Unable to query priviledge: " + pv, var9);
            }
         }

         return result;
      } else {
         return result;
      }
   }

   @TsService
   public boolean hasVcPermissions(ManagedObjectReference moref, String[] privileges) throws Exception {
      ManagedObjectReference vcRoot = VmodlHelper.getRootFolder(moref.getServerGuid());
      return this.hasPermissions(vcRoot, privileges);
   }

   @TsService
   public boolean hasPermissions(ManagedObjectReference objRef, String[] privileges) throws Exception {
      QuerySpec query = QueryUtil.buildQuerySpec(objRef, new String[]{"hasPrivileges"});
      return this.checkPermissions(privileges, query);
   }

   @TsService
   public boolean havePermissions(ManagedObjectReference[] objRefs, String[] privileges) throws Exception {
      QuerySpec query = QueryUtil.buildQuerySpec(objRefs, new String[]{"hasPrivileges"});
      return this.checkPermissions(privileges, query);
   }

   private boolean checkPermissions(String[] privileges, QuerySpec query) throws Exception {
      ParameterSpec param = new ParameterSpec();
      param.propertyName = "hasPrivileges";
      param.parameter = privileges;
      query.resourceSpec.propertySpecs[0].parameters = new ParameterSpec[]{param};
      ResultSet resultSet = QueryUtil.getData(query);
      return this.hasUserPermissions(resultSet);
   }

   private boolean hasUserPermissions(ResultSet resultSet) {
      boolean hasPrivilege = true;
      if (resultSet != null && ArrayUtils.isNotEmpty(resultSet.items)) {
         ResultItem[] var3 = resultSet.items;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            ResultItem item = var3[var5];
            PropertyValue[] var7 = item.properties;
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               PropertyValue pv = var7[var9];
               if ("hasPrivileges".equalsIgnoreCase(pv.propertyName)) {
                  hasPrivilege = (Boolean)pv.value;
                  break;
               }
            }
         }
      }

      return hasPrivilege;
   }
}
