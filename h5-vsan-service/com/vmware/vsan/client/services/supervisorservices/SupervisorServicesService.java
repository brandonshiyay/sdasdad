package com.vmware.vsan.client.services.supervisorservices;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vsan.client.services.supervisorservices.model.SupervisorService;
import com.vmware.vsan.client.util.dataservice.query.QueryBuilder;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutor;
import com.vmware.vsan.client.util.dataservice.query.QueryExecutorResult;
import com.vmware.vsan.client.util.dataservice.query.QueryModel;
import com.vmware.vsan.client.util.dataservice.query.QueryResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupervisorServicesService {
   private static final Log logger = LogFactory.getLog(SupervisorServicesService.class);
   @Autowired
   private QueryExecutor queryExecutor;
   private static final String MODEL_SUPERVISOR_SERVICE = "com.vmware.wcp.SupervisorService";
   private static final String[] SUPERVISOR_SERVICE_PROPERTIES = new String[]{"name", "label", "description", "eula", "status_enabled", "status_version", "desired_version", "versions"};

   @TsService
   public List list(ManagedObjectReference clusterRef) {
      QueryBuilder queryExecutor = new QueryBuilder();
      RequestSpec requestSpec = queryExecutor.newQuery().select(SUPERVISOR_SERVICE_PROPERTIES).from("com.vmware.wcp.SupervisorService").where().propertyEquals("cluster", clusterRef).end().build();
      QueryExecutorResult result = this.queryExecutor.execute(requestSpec);
      QueryResult supervisorServicesResult = result.getQueryResult();
      List supervisorServices = (List)supervisorServicesResult.items.stream().map((r) -> {
         return this.createSupervisorService(r);
      }).collect(Collectors.toList());
      return supervisorServices;
   }

   private SupervisorService createSupervisorService(QueryModel queryModel) {
      if (queryModel != null && queryModel.properties != null) {
         Map props = queryModel.properties;
         SupervisorService service = new SupervisorService();
         service.modelId = queryModel.id.toString();
         service.serviceId = (String)props.get("name");
         service.name = (String)props.get("label");
         service.description = (String)props.get("description");
         service.eula = (String)props.get("eula");
         service.version = (String)props.get("status_version");
         service.desiredVersion = (String)props.get("desired_version");
         service.availableVersions = (List)props.get("versions");
         service.isEnabled = Boolean.parseBoolean((String)props.get("status_enabled"));
         return service;
      } else {
         return null;
      }
   }

   private interface SupervisorServiceProperty {
      String NAME = "name";
      String LABEL = "label";
      String CLUSTER = "cluster";
      String DESCRIPTION = "description";
      String EULA = "eula";
      String STATUS_ENABLED = "status_enabled";
      String STATUS_VERSION = "status_version";
      String DESIRED_VERSION = "desired_version";
      String VERSIONS = "versions";
   }
}
