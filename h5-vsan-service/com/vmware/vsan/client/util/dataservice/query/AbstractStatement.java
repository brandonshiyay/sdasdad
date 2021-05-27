package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vise.data.Constraint;

abstract class AbstractStatement {
   protected QueryBuilder queryBuilder;

   protected AbstractStatement(QueryBuilder queryBuilder) {
      this.queryBuilder = queryBuilder;
   }

   protected QueryBuilderContext getContext() {
      return this.queryBuilder.getContext();
   }

   protected QueryTable addTable(String name) {
      QueryTable table = new QueryTable(name);
      this.queryBuilder.getContext().tables.add(table);
      return table;
   }

   protected QueryTable addTable(String name, Constraint constraint) {
      QueryTable table = new QueryTable(name, constraint);
      this.queryBuilder.getContext().tables.add(table);
      return table;
   }

   protected QueryTable getActiveTable() {
      return (QueryTable)this.queryBuilder.getContext().tables.getLast();
   }
}
