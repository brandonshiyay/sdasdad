package com.vmware.vsan.client.util.dataservice.query;

import org.apache.commons.lang3.Validate;

public class OnStatement extends FinalStatement {
   public OnStatement(QueryBuilder queryBuilder, String field) {
      super(queryBuilder);
      Validate.notEmpty(field);
      ((QueryTable)this.getContext().tables.getLast()).onField = field;
   }

   public WhereStatement where() {
      return new WhereStatement(this.queryBuilder);
   }

   public JoinStatement join(String type) {
      return new JoinStatement(this.queryBuilder, type);
   }

   public JoinStatement join(Class clazz) {
      return new JoinStatement(this.queryBuilder, clazz);
   }
}
