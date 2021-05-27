package com.vmware.vsphere.client.vsan.iscsi.providers;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.fault.VsanFault;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiLUN;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTarget;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetSystem;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.ProfileUtils;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.data.IscsiLun;
import com.vmware.vsphere.client.vsan.base.data.IscsiTarget;
import com.vmware.vsphere.client.vsan.base.impl.PbmDataProvider;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.iscsi.models.target.TargetListData;
import com.vmware.vsphere.client.vsan.iscsi.models.target.TargetModificationData;
import com.vmware.vsphere.client.vsan.iscsi.models.target.initiator.TargetInitiatorSpec;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterService;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanIscsiTargetPropertyProvider {
   @Autowired
   PbmDataProvider pbmDataProvider;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private ObjectReferenceService objectReferenceService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanStretchedClusterService stretchedClusterService;
   @Autowired
   private VsanIscsiPropertyProvider iscsiPropertyProvider;
   private static final Log _logger = LogFactory.getLog(VsanIscsiTargetPropertyProvider.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanIscsiTargetPropertyProvider.class);
   private static final String HOST_TYPE = "HostSystem";
   private static final String MANAGED_OBJECT_PREFIX = "urn:vmomi:";
   private static final String COLON = ":";
   private static final String[] HOST_PROPERTIES = new String[]{"name", "config.vsanHostConfig.faultDomainInfo.name", "preferredFaultDomain", "isWitnessHost"};

   @TsService
   public TargetListData getTargetListData(ManagedObjectReference clusterRef) {
      try {
         Measure measure = new Measure("Gets iSCSI targets, LUNs and storage profiles");
         Throwable var3 = null;

         TargetListData var8;
         try {
            VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadIscsiTargets().loadIscsiLuns().loadStoragePolicies();
            DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "allVsanHosts", ClusterComputeResource.class.getSimpleName(), HOST_PROPERTIES);
            boolean isIscsiStretchedClusterSupported = VsanCapabilityUtils.isIscsiStretchedClusterSupportedOnCluster(clusterRef);
            TargetListData data = this.getTargetListDataFromHostProperties(response, isIscsiStretchedClusterSupported);
            data.targets = this.addLunsToTargets(clusterRef, dataRetriever.getIscsiTargets(), dataRetriever.getIscsiLuns(), ProfileUtils.getPoliciesIdNamePairs(dataRetriever.getStoragePolicies()));
            var8 = data;
         } catch (Throwable var18) {
            var3 = var18;
            throw var18;
         } finally {
            if (measure != null) {
               if (var3 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var17) {
                     var3.addSuppressed(var17);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var8;
      } catch (Exception var20) {
         throw new VsanUiLocalizableException("vsan.error.target.list.data.error", "Failed to extract target list data for cluster " + clusterRef, var20, new Object[0]);
      }
   }

   private TargetListData getTargetListDataFromHostProperties(DataServiceResponse response, boolean isIscsiStretchedClusterSupported) {
      TargetListData data = new TargetListData();
      boolean isStretchedCluster = false;
      Map domainNameToHostUids = new HashMap();
      Iterator var6 = response.getResourceObjects().iterator();

      while(var6.hasNext()) {
         Object hostRef = var6.next();
         String hostUid = this.objectReferenceService.getUid(hostRef);
         String hostIp = (String)response.getProperty(hostRef, "name");
         data.hostUidToHostIpMap.put(hostUid, hostIp);
         if (isIscsiStretchedClusterSupported) {
            boolean isWitnessHost = Boolean.valueOf(response.getProperty(hostRef, "isWitnessHost") + "");
            if (isWitnessHost) {
               isStretchedCluster = true;
               data.preferredDomainName = (String)response.getProperty(hostRef, "preferredFaultDomain");
            } else {
               String domainName = (String)response.getProperty(hostRef, "config.vsanHostConfig.faultDomainInfo.name");
               if (StringUtils.isNotEmpty(domainName)) {
                  if (domainNameToHostUids.containsKey(domainName)) {
                     ((List)domainNameToHostUids.get(domainName)).add(hostUid);
                  } else {
                     List hostUids = new ArrayList();
                     hostUids.add(hostUid);
                     domainNameToHostUids.put(domainName, hostUids);
                  }
               }
            }
         }
      }

      domainNameToHostUids.forEach((domainNamex, hostUidsx) -> {
         if (domainNamex.equals(data.preferredDomainName)) {
            data.preferredDomainHostUids = hostUidsx;
         } else {
            data.secondaryDomainName = domainNamex;
            data.secondaryDomainHostUids = hostUidsx;
         }

      });
      data.isTargetLocationSupported = isStretchedCluster && isIscsiStretchedClusterSupported;
      return data;
   }

   @TsService
   public IscsiTarget getIscsiTarget(ManagedObjectReference clusterRef, String targetAlias) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanIscsiTarget target = null;
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();

            try {
               VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiTarget");
               Throwable var8 = null;

               try {
                  target = vsanIscsiSystem.getIscsiTarget(clusterRef, targetAlias);
               } catch (Throwable var36) {
                  var8 = var36;
                  throw var36;
               } finally {
                  if (p != null) {
                     if (var8 != null) {
                        try {
                           p.close();
                        } catch (Throwable var34) {
                           var8.addSuppressed(var34);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var38) {
               throw new VsanUiLocalizableException("vsan.error.target.get.fail", "Cannot get iSCSI target", var38, new Object[]{targetAlias});
            }
         } catch (Throwable var39) {
            var5 = var39;
            throw var39;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var33) {
                     var5.addSuppressed(var33);
                  }
               } else {
                  conn.close();
               }
            }

         }

         if (target != null) {
            target.ioOwnerHost = this.buildHostMor(target.ioOwnerHost, clusterRef.getServerGuid());
         }

         boolean isIscsiStretchedClusterSupported = VsanCapabilityUtils.isIscsiStretchedClusterSupportedOnCluster(clusterRef);

         Map profiles;
         try {
            profiles = this.pbmDataProvider.getStoragePolicyIdNameMap(clusterRef);
         } catch (Exception var35) {
            throw new VsanUiLocalizableException("vsan.error.target.get.fail", "Failed to load storage policies", var35, new Object[]{targetAlias});
         }

         return new IscsiTarget(target, (List)null, profiles, isIscsiStretchedClusterSupported);
      }
   }

   @TsService
   public IscsiLun[] getVsanIscsiTargetLunList(ManagedObjectReference clusterRef, String targetAlias) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanIscsiLUN[] iscsiLuns = null;
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();

            try {
               VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiLUNs");
               Throwable var8 = null;

               try {
                  iscsiLuns = vsanIscsiSystem.getIscsiLUNs(clusterRef, new String[]{targetAlias});
               } catch (Throwable var36) {
                  var8 = var36;
                  throw var36;
               } finally {
                  if (p != null) {
                     if (var8 != null) {
                        try {
                           p.close();
                        } catch (Throwable var34) {
                           var8.addSuppressed(var34);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (VsanFault var38) {
               throw new VsanUiLocalizableException("vsan.error.target.get.luns.fail", "Failed to get LUNs", var38, new Object[]{targetAlias});
            }
         } catch (Throwable var39) {
            var5 = var39;
            throw var39;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var33) {
                     var5.addSuppressed(var33);
                  }
               } else {
                  conn.close();
               }
            }

         }

         if (iscsiLuns == null) {
            return null;
         } else {
            int i = 0;
            IscsiLun[] luns = new IscsiLun[iscsiLuns.length];

            Map profiles;
            try {
               profiles = this.pbmDataProvider.getStoragePolicyIdNameMap(clusterRef);
            } catch (Exception var35) {
               throw new VsanUiLocalizableException("vsan.error.target.get.luns.fail", "Failed to load storage policies", var35, new Object[]{targetAlias});
            }

            VsanIscsiLUN[] var44 = iscsiLuns;
            int var45 = iscsiLuns.length;

            for(int var9 = 0; var9 < var45; ++var9) {
               VsanIscsiLUN lun = var44[var9];
               luns[i++] = new IscsiLun(lun, profiles);
            }

            return luns;
         }
      }
   }

   @TsService
   public TargetInitiatorSpec[] getVsanIscsiTargetInitiatorList(ManagedObjectReference clusterRef, String targetAlias) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return null;
      } else {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         TargetInitiatorSpec[] var43;
         try {
            VsanIscsiTargetSystem vsanIscsiSystem = conn.getVsanIscsiSystem();
            VsanIscsiTarget vsanIscsiTarget = null;

            try {
               VsanProfiler.Point p = _profiler.point("vsanIscsiSystem.getIscsiTarget");
               Throwable var8 = null;

               try {
                  vsanIscsiTarget = vsanIscsiSystem.getIscsiTarget(clusterRef, targetAlias);
               } catch (Throwable var36) {
                  var8 = var36;
                  throw var36;
               } finally {
                  if (p != null) {
                     if (var8 != null) {
                        try {
                           p.close();
                        } catch (Throwable var35) {
                           var8.addSuppressed(var35);
                        }
                     } else {
                        p.close();
                     }
                  }

               }
            } catch (Exception var38) {
               throw new VsanUiLocalizableException("vsan.error.target.get.fail", "Failed to get iSCSI target", var38, new Object[]{targetAlias});
            }

            List targetInitiatorList = new ArrayList();
            if (vsanIscsiTarget != null) {
               String[] initiators = vsanIscsiTarget.getInitiators();
               String[] initiatorGroups;
               int var11;
               if (initiators != null) {
                  initiatorGroups = initiators;
                  int var10 = initiators.length;

                  for(var11 = 0; var11 < var10; ++var11) {
                     String initiator = initiatorGroups[var11];
                     TargetInitiatorSpec TargetInitiatorSpec = new TargetInitiatorSpec();
                     TargetInitiatorSpec.name = initiator;
                     targetInitiatorList.add(TargetInitiatorSpec);
                  }
               }

               initiatorGroups = vsanIscsiTarget.getInitiatorGroups();
               if (initiatorGroups != null) {
                  String[] var44 = initiatorGroups;
                  var11 = initiatorGroups.length;

                  for(int var45 = 0; var45 < var11; ++var45) {
                     String initiatorGroup = var44[var45];
                     TargetInitiatorSpec TargetInitiatorSpec = new TargetInitiatorSpec();
                     TargetInitiatorSpec.name = initiatorGroup;
                     TargetInitiatorSpec.isGroup = true;
                     targetInitiatorList.add(TargetInitiatorSpec);
                  }
               }
            }

            var43 = (TargetInitiatorSpec[])targetInitiatorList.toArray(new TargetInitiatorSpec[0]);
         } catch (Throwable var39) {
            var4 = var39;
            throw var39;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var34) {
                     var4.addSuppressed(var34);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var43;
      }
   }

   @TsService
   public TargetModificationData getTargetModificationData(ManagedObjectReference clusterRef, boolean isIsciServiceConfigNeeded) {
      TargetModificationData operationData = new TargetModificationData();

      try {
         operationData.policies = this.pbmDataProvider.getStoragePolicies(clusterRef);
         operationData.domains = this.stretchedClusterService.getAvailableDomains(clusterRef);
         operationData.networks = this.iscsiPropertyProvider.getHostsCommonVnicList(clusterRef);
         if (isIsciServiceConfigNeeded) {
            operationData.iscsiTargetConfig = this.iscsiPropertyProvider.getVsanIscsiTargetConfig(clusterRef);
         }

         boolean isIscsiStretchedClusterSupported = VsanCapabilityUtils.isIscsiStretchedClusterSupportedOnCluster(clusterRef);
         if (isIscsiStretchedClusterSupported && this.stretchedClusterService.getIsVsanStretchedCluster(clusterRef)) {
            operationData.isTargetLocationSupported = true;
         }

         return operationData;
      } catch (Exception var5) {
         throw new VsanUiLocalizableException("vsan.iscsi.target.modification.related.data.error", "Failed to extract data related to target modification for cluster " + clusterRef, var5, new Object[0]);
      }
   }

   @TsService
   public IscsiTarget[] getIscsiTargets(ManagedObjectReference clusterRef) {
      if (!VsanCapabilityUtils.isIscsiTargetsSupportedOnVc(clusterRef)) {
         return new IscsiTarget[0];
      } else {
         try {
            Measure measure = new Measure("Gets iSCSI targets, LUNs and storage profiles");
            Throwable var3 = null;

            IscsiTarget[] var5;
            try {
               VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadIscsiTargets().loadIscsiLuns();
               var5 = this.addLunsToTargets(clusterRef, dataRetriever.getIscsiTargets(), dataRetriever.getIscsiLuns(), Collections.emptyMap());
            } catch (Throwable var15) {
               var3 = var15;
               throw var15;
            } finally {
               if (measure != null) {
                  if (var3 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var14) {
                        var3.addSuppressed(var14);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            return var5;
         } catch (Exception var17) {
            throw new VsanUiLocalizableException("vsan.error.targets.get.fail", var17);
         }
      }
   }

   private IscsiTarget[] addLunsToTargets(ManagedObjectReference clusterRef, VsanIscsiTarget[] targets, VsanIscsiLUN[] luns, Map storageProfiles) {
      if (ArrayUtils.isEmpty(targets)) {
         return new IscsiTarget[0];
      } else {
         Map targetToLuns = (Map)((Stream)Optional.ofNullable(luns).map(Arrays::stream).orElseGet(Stream::empty)).collect(Collectors.groupingBy(VsanIscsiLUN::getTargetAlias, Collectors.toList()));
         boolean isIscsiStretchedClusterSupported = VsanCapabilityUtils.isIscsiStretchedClusterSupportedOnCluster(clusterRef);
         return (IscsiTarget[])Arrays.stream(targets).map((target) -> {
            IscsiTarget item = new IscsiTarget(target, (List)targetToLuns.get(target.alias), storageProfiles, isIscsiStretchedClusterSupported);
            item.ioOwnerHost = this.buildHostMor(item.ioOwnerHost, clusterRef.getServerGuid());
            return item;
         }).toArray((x$0) -> {
            return new IscsiTarget[x$0];
         });
      }
   }

   private String buildHostMor(String hostStr, String vcGuid) {
      if (!StringUtils.isEmpty(hostStr) && hostStr.split(":").length != 1) {
         String[] values = hostStr.split(":");
         String hostValue = values[values.length - 1];
         return "urn:vmomi:HostSystem:" + hostValue + ":" + vcGuid;
      } else {
         return null;
      }
   }
}
