package com.vmware.vsan.client.util.dataservice.query;

import java.util.Arrays;
import java.util.List;

public class StartStatement extends AbstractStatement {
   StartStatement(QueryBuilder queryBuilder) {
      super(queryBuilder);
   }

   public SelectStatement select(List properties) {
      return new SelectStatement(this.queryBuilder, properties);
   }

   public SelectStatement select(String... properties) {
      return this.select(Arrays.asList(properties));
   }

   public SelectStatement select(String property) {
      return this.select(property);
   }

   public SelectStatement select() {
      return this.select();
   }
}
