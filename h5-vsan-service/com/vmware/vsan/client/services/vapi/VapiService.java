package com.vmware.vsan.client.services.vapi;

import com.vmware.vapi.bindings.StubFactory;
import com.vmware.vapi.bindings.client.InvocationConfig;
import com.vmware.vapi.core.ApiProvider;
import com.vmware.vapi.core.ExecutionContext;
import com.vmware.vapi.core.ExecutionContext.ApplicationData;
import com.vmware.vapi.security.SessionSecurityContext;
import com.vmware.vise.usersession.UserSession;
import com.vmware.vsan.client.services.async.AsyncUserSessionService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VapiService {
   private static final String PROP_NODE_UUID = "nodeUuid";
   private static final String PROP_ACCEPT_LANGUAGE = "accept-language";
   @Autowired
   private ApiProvider provider;
   @Autowired
   private AsyncUserSessionService userSessionService;

   public Object createStub(Class clazz) {
      return (new StubFactory(this.provider)).createStub(clazz);
   }

   public InvocationConfig createConfig(String vcGuid) {
      UserSession session = this.userSessionService.getUserSession();
      Map data = new HashMap();
      data.put("nodeUuid", vcGuid);
      data.put("accept-language", session.locale);
      ApplicationData appData = new ApplicationData(data);
      SessionSecurityContext securityContext = new SessionSecurityContext(session.clientId.toCharArray());
      ExecutionContext executionContext = new ExecutionContext(appData, securityContext);
      return new InvocationConfig(executionContext);
   }
}
