package com.vmware.vsphere.client.vsan.iscsi.providers;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.pbm.profile.Profile;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.EnvironmentBrowser;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.host.VirtualNic;
import com.vmware.vim.binding.vim.vm.ConfigTarget;
import com.vmware.vim.binding.vim.vm.DatastoreInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.Comparator;
import com.vmware.vise.data.query.Conjoiner;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.ProfileUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.util.Version;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiConfig;
import com.vmware.vsphere.client.vsan.iscsi.models.config.VsanIscsiTargetConfig;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIscsiPropertyProvider {
   private static final VsanProfiler _profiler = new VsanProfiler(VsanIscsiPropertyProvider.class);
   private static final String HOST_KEY = "hostKey";
   private static final Version HOST_VERSION_2015 = new Version("6.0.0");
   private VcClient vcClient;
   @Autowired
   private VsanConfigService vsanConfigService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;

   public void setVcClient(VcClient vcClient) {
      this.vcClient = vcClient;
   }

   @TsService
   public VsanIscsiTargetConfig getVsanIscsiTargetConfig(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(clusterRef);
      return this.getVsanIscsiTargetConfig(clusterRef, configInfoEx);
   }

   public VsanIscsiTargetConfig getVsanIscsiTargetConfig(ManagedObjectReference clusterRef, ConfigInfoEx configInfoEx) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         boolean isIscsiSupportedOnCluster = VsanCapabilityUtils.isIscsiTargetsSupportedOnCluster(clusterRef);
         if (!BooleanUtils.isNotTrue(configInfoEx.enabled) && configInfoEx.defaultConfig != null && configInfoEx.iscsiConfig != null && !BooleanUtils.isNotTrue(configInfoEx.iscsiConfig.enabled) && isIscsiSupportedOnCluster) {
            try {
               Measure measure = new Measure("getVsanIscsiTargetConfig");
               Throwable var43 = null;

               VsanIscsiTargetConfig var11;
               try {
                  VsanAsyncDataRetriever retriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadStoragePolicies().loadIscsiHomeObject();
                  VsanIscsiConfig config = VsanIscsiConfig.from(configInfoEx, retriever.getIscsiHomeObject());
                  boolean isEmptyCluster = this.isEmptyClusterForIscsi(clusterRef);
                  boolean isHostVersionsValid = this.getIsHostsVersionValid(clusterRef);
                  Profile storagePolicy = ProfileUtils.findProfileById(retriever.getStoragePolicies(), config.vsanObjectInformation.spbmProfileUuid);
                  var11 = new VsanIscsiTargetConfig(config, isEmptyCluster, isHostVersionsValid, storagePolicy);
               } catch (Throwable var36) {
                  var43 = var36;
                  throw var36;
               } finally {
                  if (measure != null) {
                     if (var43 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var35) {
                           var43.addSuppressed(var35);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }

               return var11;
            } catch (Exception var41) {
               throw new VsanUiLocalizableException(var41);
            }
         } else {
            VsanIscsiTargetConfig iscsiTargetConfig = new VsanIscsiTargetConfig();

            try {
               Measure measure = new Measure("isEmptyClusterForIscsi");
               Throwable var6 = null;

               try {
                  iscsiTargetConfig.emptyCluster = this.isEmptyClusterForIscsi(clusterRef);
               } catch (Throwable var37) {
                  var6 = var37;
                  throw var37;
               } finally {
                  if (measure != null) {
                     if (var6 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var34) {
                           var6.addSuppressed(var34);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }

               return iscsiTargetConfig;
            } catch (Exception var39) {
               throw new VsanUiLocalizableException(var39);
            }
         }
      }
   }

   @TsService
   public boolean getIsVsanIscsiEnabledOnHost(ManagedObjectReference hostRef) throws Exception {
      Boolean result = false;
      PropertyValue[] values = QueryUtil.getPropertyForRelatedObjects(hostRef, "parent", "ClusterComputeResource", "isVsanIscsiEnabled").getPropertyValues();
      if (values.length > 0) {
         result = (Boolean)values[0].value;
      }

      return result;
   }

   public boolean isEmptyClusterForIscsi(ManagedObjectReference clusterRef) throws Exception {
      List vsanDatastoresByCluster = this.getVsanDatastoresByCluster(clusterRef);
      if (CollectionUtils.isEmpty(vsanDatastoresByCluster)) {
         return true;
      } else {
         Iterator var3 = vsanDatastoresByCluster.iterator();

         ManagedObjectReference hostRef;
         do {
            if (!var3.hasNext()) {
               return false;
            }

            DatastoreInfo datastoreInfo = (DatastoreInfo)var3.next();
            ManagedObjectReference vsanDatastoreRef = datastoreInfo.datastore.datastore;
            if (vsanDatastoreRef == null) {
               return true;
            }

            hostRef = this.getConnectedHost(vsanDatastoreRef);
         } while(hostRef != null);

         return true;
      }
   }

   private List getVsanDatastoresByCluster(ManagedObjectReference clusterRef) {
      EnvironmentBrowser eBrowser = null;
      VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         ClusterComputeResource cluster = (ClusterComputeResource)vcConnection.createStub(ClusterComputeResource.class, clusterRef);
         ManagedObjectReference envBrowserRef = cluster.getEnvironmentBrowser();
         if (envBrowserRef == null) {
            Object var7 = null;
            return (List)var7;
         }

         eBrowser = (EnvironmentBrowser)vcConnection.createStub(EnvironmentBrowser.class, envBrowserRef);
      } catch (Throwable var17) {
         var4 = var17;
         throw var17;
      } finally {
         if (vcConnection != null) {
            if (var4 != null) {
               try {
                  vcConnection.close();
               } catch (Throwable var16) {
                  var4.addSuppressed(var16);
               }
            } else {
               vcConnection.close();
            }
         }

      }

      if (eBrowser == null) {
         return null;
      } else {
         ConfigTarget configTarget = eBrowser.queryConfigTarget((ManagedObjectReference)null);
         DatastoreInfo[] datastoreInfos = configTarget.datastore;
         if (ArrayUtils.isEmpty(datastoreInfos)) {
            return null;
         } else {
            List vsanDatastores = new ArrayList();
            DatastoreInfo[] var22 = datastoreInfos;
            int var23 = datastoreInfos.length;

            for(int var8 = 0; var8 < var23; ++var8) {
               DatastoreInfo datastoreInfo = var22[var8];
               if (datastoreInfo != null && datastoreInfo.datastore != null && datastoreInfo.datastore.type != null && datastoreInfo.datastore.type.toLowerCase().equals("vsan") && datastoreInfo.datastore.datastore != null) {
                  VmodlHelper.assignServerGuid(datastoreInfo.datastore.datastore, clusterRef.getServerGuid());
                  vsanDatastores.add(datastoreInfo);
               }
            }

            return vsanDatastores;
         }
      }
   }

   private List getClusterConnectedHosts(ManagedObjectReference clusterRef) {
      if (clusterRef == null) {
         return null;
      } else {
         ResultSet resultSet;
         try {
            resultSet = this.queryConnectedHosts(clusterRef, "host");
         } catch (Exception var8) {
            throw new VsanUiLocalizableException(var8);
         }

         if (resultSet != null && resultSet.items != null) {
            List connectedHosts = new ArrayList();
            ResultItem[] var4 = resultSet.items;
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               ResultItem resultItem = var4[var6];
               if (!ArrayUtils.isEmpty(resultItem.properties)) {
                  connectedHosts.add((ManagedObjectReference)resultItem.resourceObject);
               }
            }

            return connectedHosts;
         } else {
            return null;
         }
      }
   }

   @TsService
   private boolean getIsHostsVersionValid(ManagedObjectReference clusterRef) {
      List hosts = this.getClusterConnectedHosts(clusterRef);
      if (CollectionUtils.isEmpty(hosts)) {
         return false;
      } else {
         Iterator var3 = hosts.iterator();

         boolean iscsiSupportedOnHost;
         do {
            if (!var3.hasNext()) {
               return true;
            }

            ManagedObjectReference hostRef = (ManagedObjectReference)var3.next();
            iscsiSupportedOnHost = VsanCapabilityUtils.isIscsiTargetsSupportedOnHost(hostRef);
         } while(iscsiSupportedOnHost);

         return false;
      }
   }

   @TsService
   public String[] getHostsCommonVnicList(ManagedObjectReference clusterRef) {
      PropertyValue[] vnicQueryData = this.queryVnicData(clusterRef);
      if (ArrayUtils.isEmpty(vnicQueryData)) {
         return new String[0];
      } else {
         Set vnicSet = new HashSet();

         for(int i = 0; i < vnicQueryData.length; ++i) {
            PropertyValue propertyValue = vnicQueryData[i];
            if (propertyValue != null) {
               Set currentVnicSet = new HashSet();
               if (propertyValue.value instanceof VirtualNic) {
                  VirtualNic vnic = (VirtualNic)propertyValue.value;
                  currentVnicSet = this.extractVnics(new VirtualNic[]{vnic});
               } else if (propertyValue.value instanceof VirtualNic[]) {
                  VirtualNic[] vnicArray = (VirtualNic[])((VirtualNic[])propertyValue.value);
                  currentVnicSet = this.extractVnics(vnicArray);
               }

               if (i == 0) {
                  vnicSet.addAll((Collection)currentVnicSet);
               } else {
                  vnicSet.retainAll((Collection)currentVnicSet);
               }
            }
         }

         return (String[])vnicSet.toArray(new String[0]);
      }
   }

   private PropertyValue[] queryVnicData(ManagedObjectReference clusterRef) {
      try {
         return QueryUtil.getPropertyForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), "config.network.vnic").getPropertyValues();
      } catch (Exception var3) {
         throw new VsanUiLocalizableException(var3);
      }
   }

   private Set extractVnics(VirtualNic[] vnics) {
      Set currentVnicSet = new HashSet();
      VirtualNic[] var3 = vnics;
      int var4 = vnics.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         VirtualNic everyVnic = var3[var5];
         if (everyVnic != null && !StringUtils.isWhitespace(everyVnic.device)) {
            currentVnicSet.add(everyVnic.device);
         }
      }

      return currentVnicSet;
   }

   private ManagedObjectReference getConnectedHost(ManagedObjectReference datastore) throws Exception {
      if (datastore == null) {
         return null;
      } else {
         ResultSet resultSet = this.queryConnectedHosts(datastore, "hostKey");
         if (resultSet != null && resultSet.items != null) {
            ResultItem[] var3 = resultSet.items;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               ResultItem resultItem = var3[var5];
               if (!ArrayUtils.isEmpty(resultItem.properties)) {
                  String version = (String)resultItem.properties[0].value;
                  Version esxVersion = new Version(version);
                  if (esxVersion.compareTo(HOST_VERSION_2015) >= 0) {
                     ManagedObjectReference connectedHostRef = (ManagedObjectReference)resultSet.items[0].resourceObject;
                     return connectedHostRef;
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   private ResultSet queryConnectedHosts(ManagedObjectReference mor, String relationShip) throws Exception {
      if (mor == null) {
         return null;
      } else {
         Constraint dsHostsConstraint = QueryUtil.createConstraintForRelationship(mor, relationShip, HostSystem.class.getSimpleName());
         Constraint connectedHostsConstraint = QueryUtil.createPropertyConstraint(HostSystem.class.getSimpleName(), "runtime.connectionState", Comparator.EQUALS, ConnectionState.connected.name());
         Constraint dsConnectedHosts = QueryUtil.combineIntoSingleConstraint(new Constraint[]{dsHostsConstraint, connectedHostsConstraint}, Conjoiner.AND);
         QuerySpec qSpec = QueryUtil.buildQuerySpec(dsConnectedHosts, new String[]{"config.product.version"});
         qSpec.name = mor.getValue();
         ResultSet resultSet = QueryUtil.getData(qSpec);
         return resultSet;
      }
   }
}
