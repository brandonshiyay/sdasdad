package com.vmware.vsan.client.util.dataservice.query;

import org.apache.commons.lang3.Validate;

public class OrderByStatement extends FinalStatement {
   OrderByStatement(QueryBuilder queryBuilder, String property, boolean isAscending) {
      super(queryBuilder);
      Validate.notEmpty(property);
      this.getContext().orderBy = property;
      this.getContext().isAscending = isAscending;
   }

   public LimitStatement limit(int from, int maxCount) {
      return new LimitStatement(this.queryBuilder, from, maxCount);
   }

   public LimitStatement limit(int from) {
      return new LimitStatement(this.queryBuilder, from, (Integer)null);
   }
}
