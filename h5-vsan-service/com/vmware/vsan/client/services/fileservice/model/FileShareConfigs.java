package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;

public class FileShareConfigs {
   public String[] version;
   public String[] sharenames;
   public String[] shareuuids;
   public Boolean[] fileIndexingEnabled;
   public List protocols;

   public List toShareList() {
      List shares = new ArrayList();
      if (ArrayUtils.isEmpty(this.sharenames)) {
         return shares;
      } else {
         for(int i = 0; i < this.sharenames.length; ++i) {
            VsanFileServiceShare share = new VsanFileServiceShare();
            share.config = new VsanFileServiceShareConfig();
            share.config.name = this.sharenames[i];
            if (ArrayUtils.isNotEmpty(this.fileIndexingEnabled)) {
               share.config.isFileAnalyticsEnabled = BooleanUtils.isTrue(this.fileIndexingEnabled[i]);
            }

            if (!CollectionUtils.isEmpty(this.protocols)) {
               share.config.protocol = VsanFileShareProtocol.parse((String[])this.protocols.get(i));
            }

            if (ArrayUtils.isNotEmpty(this.shareuuids)) {
               share.uuid = this.shareuuids[i];
            }

            shares.add(share);
         }

         return shares;
      }
   }

   public String toString() {
      return Utils.toString(this);
   }
}
