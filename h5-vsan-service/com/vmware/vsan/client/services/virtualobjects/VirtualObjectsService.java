package com.vmware.vsan.client.services.virtualobjects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import com.vmware.vim.binding.vim.vm.SnapshotInfo;
import com.vmware.vim.binding.vim.vm.SnapshotTree;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentity;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectIdentityAndHealth;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectInformation;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.diskmanagement.managedstorage.ManagedStorageObjectsService;
import com.vmware.vsan.client.services.diskmanagement.pmem.PmemService;
import com.vmware.vsan.client.services.fileservice.VsanFileServiceConfigService;
import com.vmware.vsan.client.services.fileservice.model.FileServiceFeature;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceCommonConfig;
import com.vmware.vsan.client.services.stretchedcluster.model.VsanHostsResult;
import com.vmware.vsan.client.services.virtualobjects.data.PmemObjectPlacementFactory;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectBasicModel;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectModelFactory;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectPlacementModel;
import com.vmware.vsan.client.services.virtualobjects.data.VsanDirectObjectPlacementFactory;
import com.vmware.vsan.client.services.virtualobjects.data.VsanObjectPlacementFactory;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.ProfileUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.impl.VsanJsonParser;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterService;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class VirtualObjectsService {
   private static final Log logger = LogFactory.getLog(VirtualObjectsService.class);
   private static final int QUERY_VSAN_OBJECTS_CHUNK_SIZE = 500;
   private static final String PROP_SNAPSHOT = "snapshot";
   private static final String PROP_CONFIG = "config";
   private static final String[] PHYSICAL_PLACEMENT_HOST_PROPERTIES = new String[]{"name", "primaryIconId", "config.vsanHostConfig.clusterInfo.nodeUuid", "config.vsanHostConfig.faultDomainInfo.name"};
   @Autowired
   private VmodlHelper vmodlHelper;
   @Autowired
   private VcClient vcClient;
   @Autowired
   private VirtualObjectModelFactory voModelFactory;
   @Autowired
   private VsanStretchedClusterService stretchedClusterService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private VsanFileServiceConfigService fileService;
   @Autowired
   private PmemService pmemService;
   @Autowired
   private ManagedStorageObjectsService managedStorageObjectsService;

   @TsService
   public List listVirtualObjects(ManagedObjectReference clusterRef, String[] objectUuids) {
      Measure measure = new Measure("Collect Virtual Objects for cluster");
      Throwable var5 = null;

      List virtualObjectModels;
      try {
         VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadIscsiTargets().loadIscsiLuns().loadObjectIdentitiesAndHealth(Sets.newHashSet(ArrayUtils.nullToEmpty(objectUuids))).loadClusterUuids().loadStoragePolicies();
         boolean fileServiceEnabled = false;
         if (VsanCapabilityUtils.isFileServiceSupported(clusterRef)) {
            VsanFileServiceCommonConfig fileServiceConfig = this.fileService.getConfig(clusterRef);
            fileServiceEnabled = fileServiceConfig != null && fileServiceConfig.domainConfig != null;
            if (fileServiceEnabled) {
               Map featureMap = this.fileService.isFeatureSupportedOnRuntime(clusterRef, new FileServiceFeature[]{FileServiceFeature.PAGINATION});
               if (BooleanUtils.isTrue((Boolean)featureMap.get(FileServiceFeature.PAGINATION))) {
                  dataRetriever.loadFileShares();
               } else {
                  FileSharesPaginationSpec spec = new FileSharesPaginationSpec();
                  spec.pageSize = 32;
                  dataRetriever.loadFileShares(spec);
               }
            }
         }

         Set vsanUuids = dataRetriever.getClusterUuids();
         VsanObjectIdentityAndHealth identities = dataRetriever.getObjectIdentities();
         Set vmRefs = new HashSet();
         if (!ArrayUtils.isEmpty(identities.identities)) {
            VsanObjectIdentity[] var11 = identities.identities;
            int var12 = var11.length;

            for(int var13 = 0; var13 < var12; ++var13) {
               VsanObjectIdentity id = var11[var13];
               if (id.vm != null) {
                  vmRefs.add(id.vm);
                  vsanUuids.add(id.uuid);
               }
            }
         }

         boolean isObjectsHealthV2Supported = VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(clusterRef);
         if (!isObjectsHealthV2Supported) {
            dataRetriever.loadObjectInformation(vsanUuids);
         }

         virtualObjectModels = this.listVirtualObjects(clusterRef, vsanUuids, dataRetriever, vmRefs, measure);
         Map storagePolicies = ProfileUtils.getPoliciesIdNamePairs(dataRetriever.getStoragePolicies());

         try {
            virtualObjectModels.addAll(this.voModelFactory.buildIscsiTargets(dataRetriever.getIscsiTargets(), dataRetriever.getIscsiLuns(), storagePolicies));
         } catch (Exception var25) {
            logger.warn("Failed to list iSCSI targets. Returning partial results.");
         }

         if (fileServiceEnabled) {
            try {
               VsanObjectInformation[] objectInformation = isObjectsHealthV2Supported ? null : dataRetriever.getObjectInformation();
               List shares = VsanCapabilityUtils.isFileServiceSupported(clusterRef) ? dataRetriever.getFileShares() : Collections.emptyList();
               virtualObjectModels.addAll(this.voModelFactory.buildFileShares(shares, identities, objectInformation, storagePolicies, isObjectsHealthV2Supported));
            } catch (Exception var24) {
               logger.warn("Failed to list File Shares. Returning partial results.");
            }
         }
      } catch (Throwable var26) {
         var5 = var26;
         throw var26;
      } finally {
         if (measure != null) {
            if (var5 != null) {
               try {
                  measure.close();
               } catch (Throwable var23) {
                  var5.addSuppressed(var23);
               }
            } else {
               measure.close();
            }
         }

      }

      return virtualObjectModels;
   }

   public List listVirtualObjects(ManagedObjectReference clusterRef) throws Exception {
      return this.listVirtualObjects(clusterRef, (String[])null);
   }

   public List listVmVirtualObjects(ManagedObjectReference clusterRef, ManagedObjectReference vmRef, Set vmObjectUuids) throws Exception {
      Measure measure = new Measure("Collect Virtual Objects for specified VM");
      Throwable var5 = null;

      List var9;
      try {
         VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadObjectIdentities(vmObjectUuids).loadStoragePolicies();
         if (!VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(clusterRef)) {
            dataRetriever.loadObjectInformation(vmObjectUuids);
         }

         Set vmRefs = new HashSet();
         vmRefs.add(vmRef);
         List vmObjects = this.listVirtualObjects(clusterRef, vmObjectUuids, dataRetriever, vmRefs, measure, false);
         var9 = vmObjects;
      } catch (Throwable var18) {
         var5 = var18;
         throw var18;
      } finally {
         if (measure != null) {
            if (var5 != null) {
               try {
                  measure.close();
               } catch (Throwable var17) {
                  var5.addSuppressed(var17);
               }
            } else {
               measure.close();
            }
         }

      }

      return var9;
   }

   private List listVirtualObjects(ManagedObjectReference clusterRef, Set vsanUuids, VsanAsyncDataRetriever dataRetriever, Set vmRefs, Measure measure) {
      return this.listVirtualObjects(clusterRef, vsanUuids, dataRetriever, vmRefs, measure, true);
   }

   private List listVirtualObjects(ManagedObjectReference clusterRef, Set vsanUuids, VsanAsyncDataRetriever dataRetriever, Set vmRefs, Measure measure, boolean buildAllVirtualObjects) {
      Map vmProperties = new HashMap();
      Multimap vmSnapshots = HashMultimap.create();
      if (vmRefs.isEmpty()) {
         vmProperties = Collections.emptyMap();
      } else {
         vmSnapshots = this.listVmSnapshots((ManagedObjectReference[])vmRefs.toArray(new ManagedObjectReference[0]), measure);

         try {
            Measure vmProps = measure.start("ds(" + (vmRefs.size() == 1 ? ((ManagedObjectReference)vmRefs.iterator().next()).getValue() : vmRefs.size() + "vms") + ")");
            Throwable var10 = null;

            try {
               vmProperties = QueryUtil.getProperties((ManagedObjectReference[])vmRefs.toArray(new ManagedObjectReference[0]), new String[]{"name", "primaryIconId", "config.hardware.device"}).getMap();
            } catch (Throwable var20) {
               var10 = var20;
               throw var20;
            } finally {
               if (vmProps != null) {
                  if (var10 != null) {
                     try {
                        vmProps.close();
                     } catch (Throwable var19) {
                        var10.addSuppressed(var19);
                     }
                  } else {
                     vmProps.close();
                  }
               }

            }
         } catch (Exception var22) {
            logger.error("Unable to retrieve details for VMs", var22);
         }
      }

      VsanObjectIdentityAndHealth identities = dataRetriever.getObjectIdentities();
      boolean isObjectHealthV2Supported = VsanCapabilityUtils.isObjectsHealthV2SupportedOnVc(clusterRef);
      VsanObjectInformation[] objInfos;
      if (!isObjectHealthV2Supported) {
         objInfos = dataRetriever.getObjectInformation();
      } else {
         objInfos = new VsanObjectInformation[0];
      }

      Map storagePolicies = ProfileUtils.getPoliciesIdNamePairs(dataRetriever.getStoragePolicies());
      List models = new ArrayList();
      if (!vmRefs.isEmpty()) {
         models.addAll(this.voModelFactory.buildVms(identities, vmRefs, objInfos, (Map)vmProperties, (Multimap)vmSnapshots, storagePolicies, isObjectHealthV2Supported));
      }

      if (buildAllVirtualObjects) {
         models.addAll(this.voModelFactory.buildFcds(identities, objInfos, storagePolicies));
         models.addAll(this.voModelFactory.buildVrObjects(identities, objInfos, storagePolicies, isObjectHealthV2Supported));
         models.addAll(this.voModelFactory.buildOthers(vsanUuids, identities, objInfos, storagePolicies));
         models.addAll(this.voModelFactory.buildExtensionObjectsWithoutVm(identities, storagePolicies));
      }

      return models;
   }

   public Collection listVmSnapshots(ManagedObjectReference vmRef, Measure measure) {
      return this.listVmSnapshots(new ManagedObjectReference[]{vmRef}, measure).get(vmRef);
   }

   public Multimap listVmSnapshots(ManagedObjectReference[] vmsArray, Measure measure) {
      Multimap vmToSnapshotConfig = HashMultimap.create();
      if (ArrayUtils.isEmpty(vmsArray)) {
         logger.warn("No VMs given.");
         return vmToSnapshotConfig;
      } else {
         HashMap vmToSnapshot = new HashMap();

         try {
            Measure m = measure.start("ds(vm->snapshot)[" + vmsArray.length + "]");
            Throwable var6 = null;

            try {
               DataServiceResponse response = QueryUtil.getProperties(vmsArray, new String[]{"snapshot"});
               Map resultMap = response.getMap();
               ManagedObjectReference[] var9 = vmsArray;
               int var10 = vmsArray.length;

               for(int var11 = 0; var11 < var10; ++var11) {
                  ManagedObjectReference vm = var9[var11];
                  Map result = (Map)resultMap.get(vm);
                  if (result != null) {
                     SnapshotInfo snapshotInfo = (SnapshotInfo)result.get("snapshot");
                     if (snapshotInfo != null) {
                        vmToSnapshot.put(vm, snapshotInfo);
                     }
                  }
               }
            } catch (Throwable var43) {
               var6 = var43;
               throw var43;
            } finally {
               if (m != null) {
                  if (var6 != null) {
                     try {
                        m.close();
                     } catch (Throwable var38) {
                        var6.addSuppressed(var38);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Exception var45) {
            logger.error("Cannot retrieve snapshots for VMs: ", var45);
            return vmToSnapshotConfig;
         }

         if (vmToSnapshot.isEmpty()) {
            logger.debug("None of the VMs has snapshots");
            return vmToSnapshotConfig;
         } else {
            Map snapshotToVm = new HashMap();
            Iterator var47 = vmToSnapshot.keySet().iterator();

            while(true) {
               ManagedObjectReference vm;
               SnapshotInfo snapshotInfo;
               do {
                  if (!var47.hasNext()) {
                     if (snapshotToVm.isEmpty()) {
                        logger.debug("None of the VMs has snapshots");
                        return vmToSnapshotConfig;
                     }

                     ManagedObjectReference[] snapshots = (ManagedObjectReference[])snapshotToVm.keySet().toArray(new ManagedObjectReference[0]);

                     try {
                        Measure m = measure.start("ds(snapshot->config)[" + snapshots.length + "]");
                        Throwable var52 = null;

                        try {
                           DataServiceResponse response = QueryUtil.getProperties(snapshots, new String[]{"config"});
                           Map resultMap = response.getMap();
                           Iterator var57 = resultMap.entrySet().iterator();

                           while(var57.hasNext()) {
                              Entry entry = (Entry)var57.next();
                              ManagedObjectReference snapshot = (ManagedObjectReference)entry.getKey();
                              Map propssss = (Map)entry.getValue();
                              if (propssss != null) {
                                 ConfigInfo configInfo = (ConfigInfo)propssss.get("config");
                                 if (configInfo != null) {
                                    ManagedObjectReference vm = (ManagedObjectReference)snapshotToVm.get(snapshot);
                                    vmToSnapshotConfig.put(vm, configInfo);
                                 }
                              }
                           }
                        } catch (Throwable var40) {
                           var52 = var40;
                           throw var40;
                        } finally {
                           if (m != null) {
                              if (var52 != null) {
                                 try {
                                    m.close();
                                 } catch (Throwable var39) {
                                    var52.addSuppressed(var39);
                                 }
                              } else {
                                 m.close();
                              }
                           }

                        }
                     } catch (Exception var42) {
                        var42.printStackTrace();
                     }

                     return vmToSnapshotConfig;
                  }

                  vm = (ManagedObjectReference)var47.next();
                  snapshotInfo = (SnapshotInfo)vmToSnapshot.get(vm);
               } while(snapshotInfo.rootSnapshotList == null);

               LinkedList snapshotTrees = new LinkedList(Arrays.asList(snapshotInfo.rootSnapshotList));

               while(!snapshotTrees.isEmpty()) {
                  SnapshotTree tree = (SnapshotTree)snapshotTrees.poll();
                  snapshotToVm.put(tree.snapshot, vm);
                  if (ArrayUtils.isNotEmpty(tree.childSnapshotList)) {
                     snapshotTrees.addAll(Arrays.asList(tree.childSnapshotList));
                  }
               }
            }
         }
      }
   }

   @TsService
   public Map getVsanDirectPhysicalPlacement(ManagedObjectReference clusterRef, String[] objectIds) {
      VirtualObjectBasicModel[] vsanDirectObjects;
      try {
         Measure measure = new Measure("Retrieving vSAN Direct virtual objects");
         Throwable var5 = null;

         try {
            VsanObjectIdentityAndHealth identityAndHealth = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadObjectIdentities(Sets.newHashSet(ArrayUtils.nullToEmpty(objectIds))).getObjectIdentities();
            vsanDirectObjects = (VirtualObjectBasicModel[])Arrays.stream(identityAndHealth.identities).map(VirtualObjectBasicModel::fromVmodl).filter((vob) -> {
               return StringUtils.isNotEmpty(vob.diskUuid);
            }).toArray((x$0) -> {
               return new VirtualObjectBasicModel[x$0];
            });
         } catch (Throwable var15) {
            var5 = var15;
            throw var15;
         } finally {
            if (measure != null) {
               if (var5 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var14) {
                     var5.addSuppressed(var14);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Exception var17) {
         logger.error("Cannot load physical placement details: ", var17);
         throw new VsanUiLocalizableException("vsan.virtualObjects.error.physicalPlacement", var17);
      }

      return this.getPhysicalPlacement(clusterRef, new String[0], vsanDirectObjects);
   }

   @TsService
   public Map getVsanPhysicalPlacement(ManagedObjectReference clusterRef, String[] objectIds) {
      return this.getPhysicalPlacement(clusterRef, objectIds, new VirtualObjectBasicModel[0]);
   }

   @TsService
   public Map getPhysicalPlacement(ManagedObjectReference clusterRef, String[] vsanObjectIds, VirtualObjectBasicModel[] vsanDirectObjects) {
      Set hostRefs;
      DataServiceResponse hostProps;
      Map hostToDisks;
      Map hostToPmemStorage;
      Map hostToStorageObjUuidMapping;
      List vsanObjects;
      try {
         label228: {
            Measure measure = new Measure("Collecting placement details (" + vsanObjectIds.length + " objects)");
            Throwable var11 = null;

            HashMap var14;
            try {
               VsanHostsResult vsanHostsResult = this.stretchedClusterService.collectVsanHosts(clusterRef, measure);
               Set connectedVcClusterHosts = vsanHostsResult.connectedMembers;
               hostRefs = vsanHostsResult.getAll();
               if (!CollectionUtils.isEmpty(connectedVcClusterHosts)) {
                  VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadDisks((List)(new ArrayList(hostRefs)));
                  CompletableFuture hostToStorageObjUuidMappingFuture = this.managedStorageObjectsService.getHostToStorageObjUuidMapping(clusterRef, new ArrayList(hostRefs));
                  vsanObjects = this.getVsanVirtualObjectsFromInternalSystem(vsanObjectIds, measure, connectedVcClusterHosts);
                  Measure dsProps = measure.start("host(props)");
                  Throwable var17 = null;

                  try {
                     hostProps = QueryUtil.getProperties((ManagedObjectReference[])hostRefs.toArray(new ManagedObjectReference[0]), PHYSICAL_PLACEMENT_HOST_PROPERTIES);
                  } catch (Throwable var43) {
                     var17 = var43;
                     throw var43;
                  } finally {
                     if (dsProps != null) {
                        if (var17 != null) {
                           try {
                              dsProps.close();
                           } catch (Throwable var42) {
                              var17.addSuppressed(var42);
                           }
                        } else {
                           dsProps.close();
                        }
                     }

                  }

                  hostToDisks = dataRetriever.getDisks();
                  hostToPmemStorage = this.pmemService.getPmemStorage(clusterRef, true);
                  hostToStorageObjUuidMapping = (Map)hostToStorageObjUuidMappingFuture.get();
                  break label228;
               }

               var14 = new HashMap();
            } catch (Throwable var45) {
               var11 = var45;
               throw var45;
            } finally {
               if (measure != null) {
                  if (var11 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var41) {
                        var11.addSuppressed(var41);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            return var14;
         }
      } catch (Exception var47) {
         logger.error("Cannot load physical placement details: ", var47);
         throw new VsanUiLocalizableException("vsan.virtualObjects.error.physicalPlacement", var47);
      }

      Multimap objectPlacements = HashMultimap.create();
      this.populateVsanPhysicalPlacement(objectPlacements, hostProps, hostToDisks, vsanObjects);
      this.populateVsanDirectPhysicalPlacement(objectPlacements, hostRefs, hostProps, hostToDisks, vsanDirectObjects);
      this.populatePmemPhysicalPlacement(objectPlacements, hostRefs, hostProps, hostToPmemStorage, hostToStorageObjUuidMapping, vsanObjectIds);
      return objectPlacements.asMap();
   }

   private void populateVsanPhysicalPlacement(Multimap objectPlacements, DataServiceResponse hostProps, Map disks, List vsanObjects) {
      VsanObjectPlacementFactory factory = new VsanObjectPlacementFactory(hostProps, disks);
      Iterator var6 = vsanObjects.iterator();

      while(var6.hasNext()) {
         VsanObject vsanObject = (VsanObject)var6.next();
         List placements = factory.create(vsanObject);
         if (placements != null && !placements.isEmpty()) {
            objectPlacements.putAll(vsanObject.vsanObjectUuid, placements);
         }
      }

   }

   private void populateVsanDirectPhysicalPlacement(Multimap objectPlacements, Set hostRefs, DataServiceResponse hostProps, Map hostToDisks, VirtualObjectBasicModel[] vsanDirectObjects) {
      VsanDirectObjectPlacementFactory factory = new VsanDirectObjectPlacementFactory(hostRefs, hostProps, hostToDisks);
      VirtualObjectBasicModel[] var7 = vsanDirectObjects;
      int var8 = vsanDirectObjects.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         VirtualObjectBasicModel vsanDirectObject = var7[var9];
         VirtualObjectPlacementModel placement = factory.create(vsanDirectObject);
         if (placement != null) {
            objectPlacements.put(vsanDirectObject.uid, placement);
         }
      }

   }

   private void populatePmemPhysicalPlacement(Multimap objectPlacements, Set hostRefs, DataServiceResponse hostProps, Map hostToPmemStorage, Map hostToStorageObjUuidMapping, String[] vsanObjectIds) {
      PmemObjectPlacementFactory factory = new PmemObjectPlacementFactory(hostRefs, hostProps, hostToPmemStorage, hostToStorageObjUuidMapping);
      String[] var8 = vsanObjectIds;
      int var9 = vsanObjectIds.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         String objectId = var8[var10];
         VirtualObjectPlacementModel placement = factory.create(objectId);
         if (placement != null) {
            objectPlacements.put(objectId, placement);
         }
      }

   }

   private List getVsanVirtualObjectsFromInternalSystem(String[] objectIds, Measure measure, Set hosts) throws Exception {
      List virtualObjects = new ArrayList();
      Queue chunks = this.chunkify(objectIds);

      while(!chunks.isEmpty()) {
         Map futures = new HashMap();
         Iterator var7 = hosts.iterator();

         Future future;
         List chunk;
         while(var7.hasNext()) {
            ManagedObjectReference host = (ManagedObjectReference)var7.next();
            future = measure.newFuture("VsanInternalSystem.queryVsanObjects[" + host + "]");
            chunk = (List)chunks.poll();
            if (chunk == null) {
               break;
            }

            futures.put(future, chunk);
            logger.debug("Query UUIDs on " + host);
            VcConnection vc = this.vcClient.getConnection(host.getServerGuid());
            Throwable var12 = null;

            try {
               VsanInternalSystem internalSystem = (VsanInternalSystem)vc.createStub(VsanInternalSystem.class, this.vmodlHelper.getVsanInternalSystem(host));
               internalSystem.queryVsanObjects((String[])chunk.toArray(new String[chunk.size()]), future);
            } catch (Throwable var21) {
               var12 = var21;
               throw var21;
            } finally {
               if (vc != null) {
                  if (var12 != null) {
                     try {
                        vc.close();
                     } catch (Throwable var20) {
                        var12.addSuppressed(var20);
                     }
                  } else {
                     vc.close();
                  }
               }

            }
         }

         logger.debug("Waiting for the started requests to finish.");
         var7 = futures.entrySet().iterator();

         while(var7.hasNext()) {
            Entry entry = (Entry)var7.next();
            future = (Future)entry.getKey();
            chunk = (List)entry.getValue();
            String json = (String)future.get();
            List vsanObjects = VsanJsonParser.parseVsanObjects(json, chunk);
            virtualObjects.addAll(vsanObjects);
         }
      }

      return virtualObjects;
   }

   private Queue chunkify(String[] allUuids) {
      Queue chunks = new LinkedList();
      List uuids = Arrays.asList(allUuids);
      int chunksCount = uuids.size() / 500;

      for(int i = 0; i < chunksCount; ++i) {
         int startingIndex = i * 500;
         List subUuids = uuids.subList(startingIndex, startingIndex + 500);
         chunks.add(subUuids);
      }

      List subUuids = uuids.subList(chunksCount * 500, uuids.size());
      chunks.add(subUuids);
      logger.debug("Splitting the UUIDs into " + chunks.size() + " chunks.");
      return chunks;
   }

   @TsService
   public VirtualDisk getDiskDetails(ManagedObjectReference vmRef, String diskId) throws Exception {
      VirtualDevice[] virtualDevices = (VirtualDevice[])QueryUtil.getProperty(vmRef, "config.hardware.device", (Object)null);
      VirtualDisk result = VirtualObjectsUtil.findDisk(virtualDevices, diskId);
      if (result == null) {
         throw new IllegalArgumentException("Disk not found: " + diskId);
      } else {
         return result;
      }
   }
}
