package com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiInitiatorGroup;
import com.vmware.vim.vsan.binding.vim.cluster.VsanIscsiTargetBasicInfo;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.initiator.InitiatorGroupInitiator;
import com.vmware.vsphere.client.vsan.iscsi.models.initiatorgroup.target.InitiatorGroupTarget;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class InitiatorGroup {
   public String name;
   public List initiators = new ArrayList();
   public List targets = new ArrayList();

   public InitiatorGroup(VsanIscsiInitiatorGroup initiatorGroup) {
      String[] groupInitiators = initiatorGroup.getInitiators();
      VsanIscsiTargetBasicInfo[] targetBasicInfos = initiatorGroup.getTargets();
      this.name = initiatorGroup.getName();
      int var5;
      int var6;
      if (ArrayUtils.isNotEmpty(groupInitiators)) {
         String[] var4 = groupInitiators;
         var5 = groupInitiators.length;

         for(var6 = 0; var6 < var5; ++var6) {
            String initiatorName = var4[var6];
            InitiatorGroupInitiator initiator = new InitiatorGroupInitiator();
            initiator.name = initiatorName;
            this.initiators.add(initiator);
         }
      }

      if (ArrayUtils.isNotEmpty(targetBasicInfos)) {
         VsanIscsiTargetBasicInfo[] var9 = targetBasicInfos;
         var5 = targetBasicInfos.length;

         for(var6 = 0; var6 < var5; ++var6) {
            VsanIscsiTargetBasicInfo target = var9[var6];
            this.targets.add(new InitiatorGroupTarget(target.getAlias(), target.getIqn()));
         }
      }

   }
}
