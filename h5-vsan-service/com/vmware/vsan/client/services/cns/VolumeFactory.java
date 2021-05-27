package com.vmware.vsan.client.services.cns;

import com.vmware.vsan.client.services.cns.model.Volume;
import java.util.ArrayList;

public interface VolumeFactory {
   Volume createVolume(ArrayList var1, String var2, com.vmware.vim.vsan.binding.vim.cns.Volume var3);
}
