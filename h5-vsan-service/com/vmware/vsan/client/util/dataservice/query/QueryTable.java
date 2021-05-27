package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.Conjoiner;

class QueryTable {
   public String type;
   public String onField;
   public Constraint constraint;
   public Conjoiner tmpConjoiner;

   public QueryTable(String type) {
      this.type = type;
   }

   public QueryTable(String type, Constraint constraint) {
      this.type = type;
      this.constraint = constraint;
   }
}
