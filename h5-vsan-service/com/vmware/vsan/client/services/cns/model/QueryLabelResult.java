package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.cns.SearchLabelResult;
import com.vmware.vsan.client.services.common.data.LabelData;
import java.util.List;

@TsModel
public class QueryLabelResult {
   public List labels;
   public boolean hasMore;

   public static QueryLabelResult fromVmodl(SearchLabelResult vmodl) {
      if (vmodl == null) {
         return null;
      } else {
         QueryLabelResult result = new QueryLabelResult();
         result.hasMore = vmodl.hasMoreResults;
         result.labels = LabelData.fromKeyValue(vmodl.labels);
         return result;
      }
   }
}
