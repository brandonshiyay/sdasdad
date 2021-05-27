package com.vmware.vsan.client.services.csd;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vmodl.LocalizableMessage;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanRemoteDatastoreSystem;
import com.vmware.vim.vsan.binding.vim.vsan.MountPrecheckItem;
import com.vmware.vim.vsan.binding.vim.vsan.MountPrecheckResult;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.Comparator;
import com.vmware.vise.data.query.Conjoiner;
import com.vmware.vise.data.query.PropertyConstraint;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RelationalConstraint;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.csd.model.MountPrecheckTest;
import com.vmware.vsan.client.services.csd.model.ShareableDatastore;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.health.VsanHealthStatus;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CsdMountService {
   private static final short CSD_MOUNT_LIMIT = 5;
   private static final Logger logger = LoggerFactory.getLogger(CsdMountService.class);
   @Autowired
   private CsdService csdService;
   @Autowired
   private VsanClient vsanClient;

   @TsService
   public List getAvailableShareableDatastores(ManagedObjectReference clusterRef) {
      try {
         DataServiceResponse datastoresProperties = this.getAvailableDatastoresProperties(clusterRef);
         if (datastoresProperties.getResourceObjects().isEmpty()) {
            return Collections.EMPTY_LIST;
         } else {
            Set availableDatastores = this.getDatastoresAvailableForSharing(datastoresProperties, clusterRef);
            List result = new ArrayList();
            Iterator var5 = datastoresProperties.getResourceObjects().iterator();

            while(var5.hasNext()) {
               ManagedObjectReference datastoreRef = (ManagedObjectReference)var5.next();
               if (availableDatastores.contains(datastoreRef)) {
                  String dsContainerId = (String)datastoresProperties.getProperty(datastoreRef, "info.containerId");

                  try {
                     ManagedObjectReference serverCluster = CsdUtils.getDatastoreServerCluster(datastoreRef);
                     List clientClusters = this.csdService.getDatastoreClientClusters(serverCluster, dsContainerId);
                     result.add(ShareableDatastore.composeShareableDatastore(datastoresProperties, datastoreRef, serverCluster, clientClusters));
                  } catch (Exception var10) {
                     logger.warn("Failed to load server and client clusters: ", var10);
                  }
               }
            }

            return result;
         }
      } catch (Exception var11) {
         throw new VsanUiLocalizableException("vsan.csd.loadAvailableShareableDatastores.error", var11);
      }
   }

   private DataServiceResponse getAvailableDatastoresProperties(ManagedObjectReference clusterRef) throws Exception {
      RelationalConstraint dcConstraint = (RelationalConstraint)QueryUtil.createConstraintForRelationship(clusterRef, "dc", Datacenter.class.getSimpleName());
      RelationalConstraint dsConstraint = QueryUtil.createRelationalConstraint("datastore", dcConstraint, true, Datastore.class.getSimpleName());
      PropertyConstraint isDsVsanConstraint = QueryUtil.createPropertyConstraint(Datastore.class.getSimpleName(), "summary.type", Comparator.EQUALS, "vsan");
      Constraint compositeConstraint = QueryUtil.combineIntoSingleConstraint(new Constraint[]{dsConstraint, isDsVsanConstraint}, Conjoiner.AND);
      QuerySpec query = QueryUtil.buildQuerySpec(compositeConstraint, CsdUtils.SHAREABLE_DATASTORE_QUERY_PROPERTIES);
      ResultSet resultSet = QueryUtil.getData(query);
      return QueryUtil.getDataServiceResponse(resultSet, CsdUtils.SHAREABLE_DATASTORE_QUERY_PROPERTIES);
   }

   @TsService
   public List runCsdMountPrecheck(ManagedObjectReference clusterRef, ManagedObjectReference datastoreRef) {
      MountPrecheckResult precheckResponse;
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            Measure measure = new Measure("Run mount remote datastore prechecks: vsanRemoteDatastoreSystem.mountPrecheck");
            Throwable var7 = null;

            try {
               VsanRemoteDatastoreSystem vsanRemoteDatastoreSystem = conn.getVsanRemoteDatastoreSystem();
               precheckResponse = vsanRemoteDatastoreSystem.mountPrecheck(clusterRef, datastoreRef);
            } catch (Throwable var32) {
               var7 = var32;
               throw var32;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var31) {
                        var7.addSuppressed(var31);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            if (precheckResponse == null || precheckResponse.result == null) {
               throw new Exception("Empty precheck response");
            }
         } catch (Throwable var34) {
            var5 = var34;
            throw var34;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var30) {
                     var5.addSuppressed(var30);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } catch (Exception var36) {
         throw new VsanUiLocalizableException("vsan.csd.mountPrecheck.error", var36);
      }

      return (List)Arrays.stream(precheckResponse.result).map(CsdMountService::createMountPrecheckTest).collect(Collectors.toList());
   }

   @TsService
   public ManagedObjectReference getMountRemoteDatastore(ManagedObjectReference clusterRef, ManagedObjectReference datastoreRef) {
      try {
         Set remoteDatastores = this.csdService.getRemoteDatastores(clusterRef);
         remoteDatastores.add(datastoreRef);
         return this.csdService.setRemoteDatastores(clusterRef, remoteDatastores);
      } catch (Exception var4) {
         throw new VsanUiLocalizableException("vsan.csd.mount.error", var4);
      }
   }

   private Set getDatastoresAvailableForSharing(DataServiceResponse datastoresProperties, ManagedObjectReference clusterRef) {
      Set mountedDatastoresSet = (Set)this.csdService.getMountedDatastores(clusterRef).stream().map((d) -> {
         return d.shareableDatastore.datastore.moRef;
      }).collect(Collectors.toSet());
      Set result = new HashSet();
      Iterator var5 = datastoresProperties.getResourceObjects().iterator();

      while(var5.hasNext()) {
         ManagedObjectReference ds = (ManagedObjectReference)var5.next();
         if (!mountedDatastoresSet.contains(ds)) {
            result.add(ds);
         }
      }

      return result;
   }

   private static MountPrecheckTest createMountPrecheckTest(MountPrecheckItem item) {
      MountPrecheckTest test = new MountPrecheckTest();
      test.status = VsanHealthStatus.valueOf(item.status);
      test.description = item.description.getMessage();
      if (item.reason != null) {
         test.reasons = (List)Arrays.stream(item.reason).map(LocalizableMessage::getMessage).collect(Collectors.toList());
      }

      return test;
   }
}
