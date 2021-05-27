package com.vmware.vsan.client.util.dataservice.query;

import org.apache.commons.lang3.Validate;

public class LimitStatement extends FinalStatement {
   LimitStatement(QueryBuilder queryBuilder, int from, Integer maxCount) {
      super(queryBuilder);
      Validate.isTrue(from >= 0);
      Validate.isTrue(maxCount == null || maxCount >= 0);
      this.getContext().startFrom = from;
      this.getContext().maxResultsCount = maxCount;
   }
}
