package com.vmware.vsphere.client.vsan.iscsi.adapter;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiInitiatorGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTarget;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetBasicInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.CompositeConstraint;
import com.vmware.vise.data.query.PropertyConstraint;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.Response;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.type;
import com.vmware.vsan.client.services.common.VsanBaseDataProviderAdapter;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.utils.VsanIscsiTargetUriUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

@type("VsanIscsiTarget")
public class VsanIscsiTargetDataAdapter extends VsanBaseDataProviderAdapter {
   private static final String VSAN_ISCSI_TARGET_URI_PREFIX = "urn:vsaniscsi:VsanIscsiTarget:VsanIscsiTargetList:NO#";
   private static final String VSAN_ISCSI_TARGET_CLUSTERREF_PROPERTY = "clusterRef";
   private static final String VSAN_ISCSI_TARGET_INITIATORGROUPIQN_PROPERTY = "initiatorGroupIqn";
   private static final String VSAN_ISCSI_TARGET_IQN_FIELD = "iqn";
   private static final String VSAN_ISCSI_TARGET_ALIAS_FIELD = "alias";
   private static final String VSAN_ISCSI_TARGET_AUTHTYPE_FIELD = "authType";
   private static final VsanProfiler _profiler = new VsanProfiler(VsanIscsiTargetDataAdapter.class);
   @Autowired
   private VsanClient vsanClient;

   protected Response getResponse(RequestSpec request) throws Exception {
      ManagedObjectReference clusterRef = null;
      String initiatorGroupIqn = null;
      Constraint constraint = request.querySpec[0].resourceSpec.constraint;
      if (constraint instanceof CompositeConstraint) {
         CompositeConstraint compositeConstraint = (CompositeConstraint)constraint;
         Constraint[] childConstraints = compositeConstraint.nestedConstraints;
         Constraint[] var7 = childConstraints;
         int var8 = childConstraints.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            Constraint childConstraint = var7[var9];
            if (childConstraint instanceof PropertyConstraint) {
               PropertyConstraint propertyConstraint = (PropertyConstraint)childConstraint;
               if (propertyConstraint.propertyName.equals("clusterRef")) {
                  clusterRef = (ManagedObjectReference)propertyConstraint.comparableValue;
               } else if (propertyConstraint.propertyName.equals("initiatorGroupIqn")) {
                  initiatorGroupIqn = (String)propertyConstraint.comparableValue;
               }
            }
         }
      }

      Validate.notNull(clusterRef);
      Validate.notEmpty(initiatorGroupIqn);
      Response res = new Response();
      ResultSet rs = new ResultSet();
      ResultItem[] its = this.createResultItems(this.getTargetsNotInAccessibleList(clusterRef, initiatorGroupIqn));
      rs.items = its;
      rs.totalMatchedObjectCount = rs.items.length;
      ResultSet[] rss = new ResultSet[]{rs};
      res.resultSet = rss;
      return res;
   }

   private VsanIscsiTarget[] getTargetsNotInAccessibleList(ManagedObjectReference clusterRef, String initiatorGroupIqn) throws Exception {
      VsanIscsiInitiatorGroup vsanIscsiInitiatorGroup = null;
      VsanIscsiTarget[] allTargets = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      try {
         VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();

         try {
            VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiTargets");
            Throwable var43 = null;

            try {
               allTargets = vsanIscsiSystem.getIscsiTargets(clusterRef);
            } catch (Throwable var34) {
               var43 = var34;
               throw var34;
            } finally {
               if (p != null) {
                  if (var43 != null) {
                     try {
                        p.close();
                     } catch (Throwable var33) {
                        var43.addSuppressed(var33);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var36) {
            Exception ex = new Exception(var36.getLocalizedMessage(), var36);
            throw ex;
         }

         vsanIscsiInitiatorGroup = vsanIscsiSystem.getIscsiInitiatorGroup(clusterRef, initiatorGroupIqn);
      } catch (Throwable var37) {
         var6 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var6.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      if (allTargets == null) {
         return null;
      } else {
         VsanIscsiTargetBasicInfo[] targetsInAccessibleList = vsanIscsiInitiatorGroup.getTargets();
         ArrayList allNotInAccessibleTargets = new ArrayList(Arrays.asList(allTargets));
         if (targetsInAccessibleList != null && targetsInAccessibleList.length > 0) {
            VsanIscsiTargetBasicInfo[] var41 = targetsInAccessibleList;
            int var42 = targetsInAccessibleList.length;

            for(int var44 = 0; var44 < var42; ++var44) {
               VsanIscsiTargetBasicInfo targetInAccessibleList = var41[var44];

               for(int i = allNotInAccessibleTargets.size() - 1; i >= 0; --i) {
                  VsanIscsiTarget unsureTarget = (VsanIscsiTarget)allNotInAccessibleTargets.get(i);
                  if (unsureTarget != null && unsureTarget.iqn.equals(targetInAccessibleList.iqn)) {
                     allNotInAccessibleTargets.remove(i);
                  }
               }
            }
         }

         return (VsanIscsiTarget[])allNotInAccessibleTargets.toArray(new VsanIscsiTarget[0]);
      }
   }

   private ResultItem[] createResultItems(VsanIscsiTarget[] targets) throws Exception {
      if (targets != null && targets.length != 0) {
         int targetsCount = targets.length;
         ResultItem[] its = new ResultItem[targetsCount];

         for(int i = 0; i < targetsCount; ++i) {
            VsanIscsiTarget target = targets[i];
            ResultItem it = new ResultItem();
            it.resourceObject = new URI("urn:vsaniscsi:VsanIscsiTarget:VsanIscsiTargetList:NO#" + VsanIscsiTargetUriUtil.encode(target.alias));
            it.properties = this.createPropertyValues(target);
            its[i] = it;
         }

         return its;
      } else {
         return new ResultItem[0];
      }
   }

   private PropertyValue[] createPropertyValues(VsanIscsiTarget target) throws Exception {
      PropertyValue iqn_pv = new PropertyValue();
      iqn_pv.propertyName = "iqn";
      iqn_pv.value = target.iqn;
      PropertyValue alias_pv = new PropertyValue();
      alias_pv.propertyName = "alias";
      alias_pv.value = target.alias;
      PropertyValue authType_pv = new PropertyValue();
      authType_pv.propertyName = "authType";
      authType_pv.value = target.authSpec.authType;
      PropertyValue[] pvs = new PropertyValue[]{iqn_pv, alias_pv, authType_pv};
      return pvs;
   }
}
