package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class PrecheckPersistentInstanceData {
   private static final Log logger = LogFactory.getLog(PrecheckPersistentInstanceData.class);
   private static final String PERSISTENCE_SPLIT_COLON_REGEX_PATTERN = "(.*):(.*):(.*)";
   private static final Pattern pattern = Pattern.compile("(.*):(.*):(.*)");
   public String name;
   public String namespace;
   public String service;
   public PrecheckPersistentInstanceState state;

   public PrecheckPersistentInstanceData() {
   }

   public PrecheckPersistentInstanceData(String name, String namespace, String service, PrecheckPersistentInstanceState state) {
      this.name = name;
      this.namespace = namespace;
      this.service = service;
      this.state = state;
   }

   public static PrecheckPersistentInstanceData createPrecheckPersistentInstance(String instance, PrecheckPersistentInstanceState predictedState) {
      if (StringUtils.isEmpty(instance)) {
         logger.error(String.format("Empty instance data found for predicted instance state: %s", predictedState.toString()));
         return null;
      } else {
         Matcher matcher = pattern.matcher(instance);
         if (matcher.matches() && matcher.groupCount() == 3) {
            return new PrecheckPersistentInstanceData(matcher.group(3), matcher.group(2), matcher.group(1), predictedState);
         } else {
            logger.error(String.format("Unexpected format for persistence data found at: %s", instance));
            return null;
         }
      }
   }
}
