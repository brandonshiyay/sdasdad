package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShare;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQueryResult;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

@TsModel
public class FileSharesPaginationResult {
   public List shares;
   public int total;

   public static FileSharesPaginationResult fromVmodl(FileShareQueryResult queryResult) {
      FileSharesPaginationResult result = new FileSharesPaginationResult();
      result.shares = new ArrayList();
      if (ArrayUtils.isNotEmpty(queryResult.fileShares)) {
         FileShare[] var2 = queryResult.fileShares;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            FileShare share = var2[var4];
            result.shares.add(VsanFileServiceShare.fromVmodl(share));
         }
      }

      if (queryResult.totalShareCount != null) {
         result.total = queryResult.totalShareCount.intValue();
      } else {
         result.total = 0;
      }

      return result;
   }
}
