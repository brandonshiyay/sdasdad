package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.query.ObjectIdentityConstraint;
import com.vmware.vsan.client.util.VapiUtils;
import java.net.URI;
import org.apache.commons.lang3.Validate;

public class IdConditionStatement extends ConditionStatement {
   IdConditionStatement(QueryBuilder queryBuilder, ManagedObjectReference moRef) {
      super(queryBuilder);
      Validate.notNull(moRef);
      this.createIdentityConstraint(moRef);
   }

   IdConditionStatement(QueryBuilder queryBuilder, URI uri) {
      super(queryBuilder);
      Validate.notNull(uri);
      this.createIdentityConstraint(uri);
   }

   IdConditionStatement(QueryBuilder queryBuilder, String id, String serverGuid) {
      super(queryBuilder);
      Validate.notEmpty(id);
      Validate.notEmpty(serverGuid);
      URI vapiUri = VapiUtils.createVapiUri(this.getActiveTable().type, id, serverGuid);
      this.createIdentityConstraint(vapiUri);
   }

   private void createIdentityConstraint(Object target) {
      ObjectIdentityConstraint constraint = ConstraintUtils.createObjectIdentityConstraint(this.getActiveTable().type, target);
      Constraint combinedConstraint = ConstraintUtils.combineConstraints(this.getActiveTable().constraint, constraint, this.getActiveTable().tmpConjoiner);
      this.getActiveTable().constraint = combinedConstraint;
   }
}
