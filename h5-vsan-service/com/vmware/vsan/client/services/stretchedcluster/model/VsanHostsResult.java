package com.vmware.vsan.client.services.stretchedcluster.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vise.data.query.PropertyValue;
import java.util.HashSet;
import java.util.Set;

@TsModel
public class VsanHostsResult {
   private final PropertyValue[] hostData;
   private final VSANWitnessHostInfo[] witnessHostInfos;
   public final Set members;
   public final Set connectedMembers;
   public final Set witnesses;
   public final Set metadataOnlyNodes;

   public VsanHostsResult() {
      this(new PropertyValue[0], new VSANWitnessHostInfo[0]);
   }

   public VsanHostsResult(PropertyValue[] hostData, VSANWitnessHostInfo[] witnessHostInfos) {
      this.hostData = hostData;
      this.witnessHostInfos = witnessHostInfos;
      Set members = new HashSet();
      Set connectedMembers = new HashSet();
      Set witnesses = new HashSet();
      Set metadataOnlyNodes = new HashSet();
      PropertyValue[] var7 = hostData;
      int var8 = hostData.length;

      int var9;
      for(var9 = 0; var9 < var8; ++var9) {
         PropertyValue val = var7[var9];
         if (val.propertyName.equals("runtime.connectionState")) {
            ManagedObjectReference hostRef = (ManagedObjectReference)val.resourceObject;
            members.add(hostRef);
            if (ConnectionState.connected.equals(val.value)) {
               connectedMembers.add(hostRef);
            }
         }
      }

      if (witnessHostInfos != null) {
         VSANWitnessHostInfo[] var13 = witnessHostInfos;
         var8 = witnessHostInfos.length;

         for(var9 = 0; var9 < var8; ++var9) {
            VSANWitnessHostInfo witnessHostInfo = var13[var9];
            witnesses.add(witnessHostInfo.host);
            if (witnessHostInfo.metadataMode != null && witnessHostInfo.metadataMode) {
               metadataOnlyNodes.add(witnessHostInfo.host);
            }
         }
      }

      this.members = members;
      this.connectedMembers = connectedMembers;
      this.witnesses = witnesses;
      this.metadataOnlyNodes = metadataOnlyNodes;
   }

   public Set getAll() {
      Set result = new HashSet();
      result.addAll(this.members);
      result.addAll(this.witnesses);
      return result;
   }
}
