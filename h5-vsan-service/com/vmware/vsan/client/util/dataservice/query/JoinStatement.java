package com.vmware.vsan.client.util.dataservice.query;

import org.apache.commons.lang3.Validate;

public class JoinStatement extends AbstractStatement {
   public JoinStatement(QueryBuilder queryBuilder, String type) {
      super(queryBuilder);
      Validate.notEmpty(type);
      this.getContext().tables.add(new QueryTable(type));
   }

   public JoinStatement(QueryBuilder queryBuilder, Class clazz) {
      super(queryBuilder);
      Validate.notNull(clazz);
      this.getContext().tables.add(new QueryTable(clazz.getSimpleName()));
   }

   public OnStatement on(String field) {
      return new OnStatement(this.queryBuilder, field);
   }
}
