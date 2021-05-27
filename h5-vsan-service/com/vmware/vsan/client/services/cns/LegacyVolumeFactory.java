package com.vmware.vsan.client.services.cns;

import com.vmware.vim.vsan.binding.vim.cns.ContainerCluster;
import com.vmware.vim.vsan.binding.vim.cns.KubernetesEntityMetadata;
import com.vmware.vim.vsan.binding.vim.cns.Volume;
import com.vmware.vsan.client.services.cns.model.ClusterFlavor;
import com.vmware.vsan.client.services.cns.model.CnsDatastoreAccessibilityStatus;
import com.vmware.vsan.client.services.cns.model.CnsHealthStatus;
import com.vmware.vsan.client.services.cns.model.KubernetesEntity;
import com.vmware.vsan.client.services.cns.model.KubernetesEntityType;
import com.vmware.vsan.client.services.cns.model.VolumeContainerCluster;
import com.vmware.vsan.client.services.common.data.LabelData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

public class LegacyVolumeFactory extends BaseVolumeFactory {
   protected void updateVolume(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume) {
      if (cnsVolume.metadata != null && cnsVolume.metadata.containerCluster != null) {
         ContainerCluster containerCluster = cnsVolume.metadata.containerCluster;
         VolumeContainerCluster cluster = new VolumeContainerCluster();
         cluster.name = containerCluster.clusterId;
         cluster.type = containerCluster.clusterType;
         cluster.flavor = ClusterFlavor.fromString(containerCluster.clusterFlavor);
         volume.containerClusters.add(cluster);
         List labels = new ArrayList();
         List pvs = new ArrayList();
         List pvcs = new ArrayList();
         List pods = new ArrayList();
         if (!ArrayUtils.isEmpty(cnsVolume.metadata.entityMetadata)) {
            this.distributeEntitiesByType(pvs, pvcs, pods, labels, cnsVolume.metadata.entityMetadata);
            boolean hasPV = CollectionUtils.isNotEmpty(pvs);
            boolean hasPVC = CollectionUtils.isNotEmpty(pvcs);
            List podNames = (List)pods.stream().map((pod) -> {
               return pod.entityName;
            }).collect(Collectors.toList());
            if (hasPV) {
               KubernetesEntity persistentVolume = this.createClusterPersistentVolume((KubernetesEntityMetadata)pvs.get(0), KubernetesEntityType.PERSISTENT_VOLUME);
               cluster.persistentVolumes.add(persistentVolume);
               if (hasPVC) {
                  KubernetesEntity persistentVolumeClaim = this.createClusterPersistentVolume((KubernetesEntityMetadata)pvcs.get(0), KubernetesEntityType.PERSISTENT_VOLUME_CLAIM);
                  persistentVolume.persistentVolumeClaim = persistentVolumeClaim;
                  persistentVolumeClaim.podNames = podNames;
               }
            }

            if (CollectionUtils.isNotEmpty(podNames) && (!hasPV || !hasPVC)) {
               volume.podNames = podNames;
            }

            volume.labels = LabelData.fromKeyValue((List)labels);
         }
      }
   }

   protected void updateHealthStatus(Volume cnsVolume, com.vmware.vsan.client.services.cns.model.Volume volume) {
      CnsDatastoreAccessibilityStatus datastoreAccessibility = CnsDatastoreAccessibilityStatus.fromName(cnsVolume.datastoreAccessibilityStatus);
      volume.healthStatus = CnsHealthStatus.fromDatastoreAccessibility(datastoreAccessibility);
   }
}
