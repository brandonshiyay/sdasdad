package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.ResourceSpec;
import com.vmware.vise.data.query.CompositeConstraint;
import com.vmware.vise.data.query.Conjoiner;
import com.vmware.vise.data.query.OrderingCriteria;
import com.vmware.vise.data.query.OrderingPropertySpec;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.ResultSpec;
import com.vmware.vise.data.query.SortType;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryBuilder {
   private static final String QUERY_NAME_FORMAT = "query-%tQ-%d";
   private static final int SEED_RANGE = 10000;
   private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
   private QueryBuilderContext context;
   private List queries = new ArrayList();

   public List getQueries() {
      return this.queries;
   }

   public QueryBuilder addQuery(QuerySpec query) {
      this.queries.add(query);
      return this;
   }

   public QueryBuilder addQueries(List queries) {
      this.queries.addAll(queries);
      return this;
   }

   public StartStatement newQuery(String name) {
      this.context = new QueryBuilderContext();
      this.context.name = name;
      return new StartStatement(this);
   }

   public StartStatement newQuery() {
      String name = this.generateName();
      return this.newQuery(name);
   }

   private String generateName() {
      Date now = new Date();
      int randomSeed = (int)(Math.random() * 10000.0D);
      return String.format("query-%tQ-%d", now, randomSeed);
   }

   QueryBuilderContext getContext() {
      return this.context;
   }

   void buildQuery() {
      QuerySpec querySpec = this.prepareQuery();
      this.queries.add(querySpec);
      this.context = new QueryBuilderContext();
   }

   public RequestSpec build() {
      RequestSpec requestSpec = new RequestSpec();
      requestSpec.querySpec = (QuerySpec[])this.queries.toArray(new QuerySpec[0]);
      logger.debug("Created RequestSpec: " + Utils.toString(requestSpec));
      return requestSpec;
   }

   private QuerySpec prepareQuery() {
      List propertySpecs = (List)this.context.tables.stream().map(this::createPropertySpecFromTable).collect(Collectors.toList());
      ResourceSpec resourceSpec = new ResourceSpec();
      resourceSpec.propertySpecs = (PropertySpec[])propertySpecs.toArray(new PropertySpec[0]);
      resourceSpec.constraint = this.createConstraint();
      QuerySpec querySpec = new QuerySpec();
      querySpec.name = this.context.name;
      querySpec.resourceSpec = resourceSpec;
      querySpec.resultSpec = this.createResultSpec();
      logger.debug("Prepared a query spec: " + Utils.toString(querySpec));
      return querySpec;
   }

   private Constraint createConstraint() {
      QueryTable firstTable = (QueryTable)this.context.tables.pollFirst();
      Object mainConstraint;
      if (firstTable.constraint != null) {
         mainConstraint = firstTable.constraint;
      } else {
         mainConstraint = new Constraint();
         ((Constraint)mainConstraint).targetType = firstTable.type;
      }

      Iterator var3 = this.context.tables.iterator();

      while(var3.hasNext()) {
         QueryTable table = (QueryTable)var3.next();
         mainConstraint = ConstraintUtils.createRelationalConstraint((Constraint)mainConstraint, table.type, table.onField);
         if (table.constraint != null) {
            CompositeConstraint compositeConstraint = ConstraintUtils.createCompositeConstraint(Conjoiner.AND, (Constraint)mainConstraint, table.constraint);
            mainConstraint = compositeConstraint;
         }
      }

      return ConstraintUtils.convertConstraint((Constraint)mainConstraint);
   }

   private ResultSpec createResultSpec() {
      ResultSpec resultSpec = new ResultSpec();
      resultSpec.offset = this.context.startFrom;
      resultSpec.maxResultCount = this.context.maxResultsCount;
      if (StringUtils.isNotEmpty(this.context.orderBy)) {
         resultSpec.order = new OrderingCriteria();
         OrderingPropertySpec orderingPropertySpec = new OrderingPropertySpec();
         orderingPropertySpec.propertyNames = new String[]{this.context.orderBy};
         orderingPropertySpec.orderingType = this.context.isAscending ? SortType.ASCENDING : SortType.DESCENDING;
         resultSpec.order.orderingProperties = new OrderingPropertySpec[]{orderingPropertySpec};
      }

      return resultSpec;
   }

   private PropertySpec createPropertySpecFromTable(QueryTable table) {
      PropertySpec propertySpec = new PropertySpec();
      propertySpec.propertyNames = (String[])this.context.properties.toArray(new String[0]);
      propertySpec.type = table.type;
      return propertySpec;
   }
}
