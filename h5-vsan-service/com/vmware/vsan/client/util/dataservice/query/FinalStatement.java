package com.vmware.vsan.client.util.dataservice.query;

public abstract class FinalStatement extends AbstractStatement {
   FinalStatement(QueryBuilder queryBuilder) {
      super(queryBuilder);
   }

   public QueryBuilder end() {
      this.queryBuilder.buildQuery();
      return this.queryBuilder;
   }
}
