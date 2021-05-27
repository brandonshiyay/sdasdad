package com.vmware.vsan.client.sessionmanager.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.vmware.vim.binding.vim.version.unstable;
import com.vmware.vim.binding.vim.version.versions;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.service;
import com.vmware.vim.binding.vmodl.versionId;
import com.vmware.vim.vsan.binding.vsan.version.version10;
import com.vmware.vim.vsan.binding.vsan.version.version11;
import com.vmware.vim.vsan.binding.vsan.version.version12;
import com.vmware.vim.vsan.binding.vsan.version.version13;
import com.vmware.vim.vsan.binding.vsan.version.version14;
import com.vmware.vim.vsan.binding.vsan.version.version15;
import com.vmware.vim.vsan.binding.vsan.version.version16;
import com.vmware.vim.vsan.binding.vsan.version.version17;
import com.vmware.vim.vsan.binding.vsan.version.version18;
import com.vmware.vim.vsan.binding.vsan.version.version3;
import com.vmware.vim.vsan.binding.vsan.version.version4;
import com.vmware.vim.vsan.binding.vsan.version.version5;
import com.vmware.vim.vsan.binding.vsan.version.version6;
import com.vmware.vim.vsan.binding.vsan.version.version7;
import com.vmware.vim.vsan.binding.vsan.version.version8;
import com.vmware.vim.vsan.binding.vsan.version.version9;
import com.vmware.vise.usersession.ServerInfo;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcService;
import com.vmware.vsphere.client.vsan.base.util.NetUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class VersionService {
   @Autowired
   private VcService vcService;
   private static Logger logger = LoggerFactory.getLogger(VersionService.class);
   public static final String VSAN_VERSIONING_FILE = "/vsanServiceVersions.xml";
   public static final String VIM_VERSIONING_FILE = "/vimServiceVersions.xml";
   private static final String PROP_NAMESPACES = "namespaces";
   private static final String PROP_NAME = "name";
   private static final String PROP_VERSION = "version";
   private static final String PROP_PRIOR_VERSIONS = "priorVersions";
   private static final String VERSION_PATTERN = "urn:%s:%s";
   private static final String UNSTABLE_VERSION_REGEX = "urn:\\w+:u\\w+";
   private static final String UNSTABLE_VERSION_PREFIX = "u";
   private static final ImmutableList SUPPORTED_VERSIONS = ImmutableList.of(version18.class, version17.class, version16.class, version15.class, version14.class, version13.class, version12.class, version11.class, version10.class, version9.class, version8.class, version7.class, new Class[]{version6.class, version5.class, version4.class, version3.class, unstable.class, com.vmware.vim.binding.vim.version.version13.class, com.vmware.vim.binding.vim.version.version11.class, com.vmware.vim.binding.vim.version.version10.class});

   public Class getVimVmodlVersion(String vcEndpoint) {
      Class result = null;

      try {
         result = this.getVmodlVersion(vcEndpoint, "/vimServiceVersions.xml", (String)null);
      } catch (Exception var4) {
         result = versions.VIM_VERSION_LTS;
      }

      return result;
   }

   public Class getVsanVmodlVersion(String vcEndpoint) {
      Class result = null;

      try {
         result = this.getVmodlVersion(vcEndpoint, "/vsanServiceVersions.xml", "/vimServiceVersions.xml");
      } catch (Exception var4) {
         result = com.vmware.vim.vsan.binding.vsan.version.versions.VSAN_VERSION_LTS;
      }

      return result;
   }

   public Class getVmodlVersion(String vcEndpoint, String primaryVersionFile, String secondaryVersionFile) throws Exception {
      Set versionKeys = null;

      try {
         versionKeys = this.readVmodlVersionKeys(vcEndpoint, primaryVersionFile, secondaryVersionFile);
      } catch (Exception var11) {
         throw new IllegalStateException("Failed to read VMODL version: " + vcEndpoint);
      }

      String unstableVersion = this.getMatchingVersion(versionKeys, "urn:\\w+:u\\w+");
      UnmodifiableIterator var6 = SUPPORTED_VERSIONS.iterator();

      while(var6.hasNext()) {
         Class version = (Class)var6.next();
         String namespace = ((service)version.getAnnotation(service.class)).namespace();
         String versionId = ((versionId)version.getAnnotation(versionId.class)).value();
         String versionString = String.format("urn:%s:%s", namespace, versionId);
         if (versionId.startsWith("u") && unstableVersion != null) {
            return version;
         }

         if (versionKeys.contains(versionString)) {
            return version;
         }

         logger.warn("Version '" + versionString + "' not supported by the server.");
      }

      throw new Exception("No matching VMODL version found");
   }

   public boolean isVsanVmodlVersionHigherThan(ManagedObjectReference mor, Class specifiedVersion) {
      ServerInfo serverInfo = this.vcService.findServerInfo(mor.getServerGuid());
      Class version = this.getVsanVmodlVersion(serverInfo.serviceUrl);
      if (SUPPORTED_VERSIONS.indexOf(version) == -1) {
         return false;
      } else {
         return SUPPORTED_VERSIONS.indexOf(version) <= SUPPORTED_VERSIONS.indexOf(specifiedVersion);
      }
   }

   private String getMatchingVersion(Set versionKeys, String regex) {
      Iterator var3 = versionKeys.iterator();

      String versionKey;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         versionKey = (String)var3.next();
      } while(!versionKey.matches(regex));

      return versionKey;
   }

   public Class getVmodlVersion(String vcGuid, String primaryVersionFile) throws Exception {
      return this.getVmodlVersion(vcGuid, primaryVersionFile, (String)null);
   }

   private Set readVmodlVersionKeys(String vcEndpoint, String primaryVersionFile, String secondaryVersionFile) throws ParserConfigurationException, IOException, SAXException, KeyManagementException, NoSuchAlgorithmException {
      Set versions = new HashSet();
      String versionXml = readVmodVersionXml(vcEndpoint, primaryVersionFile, secondaryVersionFile);
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder xmlBuilder = documentBuilderFactory.newDocumentBuilder();
      Document xml = xmlBuilder.parse(new ByteArrayInputStream(versionXml.getBytes()));
      NodeList namespaces = xml.getElementsByTagName("namespaces").item(0).getChildNodes();
      if (namespaces.getLength() == 0) {
         throw new IllegalStateException("No namespaces found!");
      } else {
         NodeList namespace = namespaces.item(0).getChildNodes();
         String name = null;
         String version = null;
         NodeList priorVersions = null;

         int i;
         Node property;
         String propName;
         for(i = 0; i < namespace.getLength(); ++i) {
            property = namespace.item(i);
            propName = property.getNodeName();
            if (StringUtils.isEmpty(propName)) {
               logger.warn("Empty property met... strange but let's continue with the next element");
            } else {
               byte var18 = -1;
               switch(propName.hashCode()) {
               case 3373707:
                  if (propName.equals("name")) {
                     var18 = 0;
                  }
                  break;
               case 351608024:
                  if (propName.equals("version")) {
                     var18 = 1;
                  }
                  break;
               case 1477847941:
                  if (propName.equals("priorVersions")) {
                     var18 = 2;
                  }
               }

               switch(var18) {
               case 0:
                  name = property.getTextContent();
                  break;
               case 1:
                  version = property.getTextContent();
                  break;
               case 2:
                  priorVersions = property.getChildNodes();
                  break;
               default:
                  logger.warn("Unknown property met: " + propName);
               }
            }
         }

         if (name != null && version != null) {
            versions.add(createVersion(name, version));
            if (priorVersions != null && priorVersions.getLength() != 0) {
               for(i = 0; i < priorVersions.getLength(); ++i) {
                  property = priorVersions.item(i);
                  propName = property.getNodeName();
                  if (StringUtils.isEmpty(propName)) {
                     logger.warn("Empty property met... strange but let's continue with the next element");
                  } else {
                     String v = null;
                     byte var19 = -1;
                     switch(propName.hashCode()) {
                     case 351608024:
                        if (propName.equals("version")) {
                           var19 = 0;
                        }
                     default:
                        switch(var19) {
                        case 0:
                           v = property.getTextContent();
                           break;
                        default:
                           logger.warn("Unknonw property met: " + propName);
                        }

                        if (StringUtils.isNotEmpty(v)) {
                           versions.add(createVersion(name, v));
                        }
                     }
                  }
               }
            }

            return versions;
         } else {
            throw new IllegalStateException("Could not find primary name and version in namespace.");
         }
      }
   }

   private static String createVersion(String name, String versionNumber) {
      return name + ":" + versionNumber;
   }

   private static String readVmodVersionXml(String vcEndpoint, String primaryVersionFile, String secondaryVersionFile) throws NoSuchAlgorithmException, KeyManagementException, IOException {
      String versionXml = readVersions(vcEndpoint + primaryVersionFile);
      if (versionXml == null && secondaryVersionFile != null) {
         versionXml = readVersions(vcEndpoint + secondaryVersionFile);
      }

      if (versionXml == null) {
         throw new IllegalStateException("Failed to read VMODL version: " + vcEndpoint);
      } else {
         return versionXml;
      }
   }

   private static String readVersions(String serviceUrl) throws NoSuchAlgorithmException, KeyManagementException, IOException {
      HttpsURLConnection conn = null;

      try {
         conn = NetUtils.createUntrustedConnection(serviceUrl);
         conn.setRequestMethod("GET");
         conn.setUseCaches(false);
         int responseCode = conn.getResponseCode();
         if (NetUtils.isSuccess(responseCode)) {
            StringBuilder result = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            Throwable var5 = null;

            try {
               String line;
               try {
                  while((line = in.readLine()) != null) {
                     result.append(line);
                  }
               } catch (Throwable var21) {
                  var5 = var21;
                  throw var21;
               }
            } finally {
               if (in != null) {
                  if (var5 != null) {
                     try {
                        in.close();
                     } catch (Throwable var20) {
                        var5.addSuppressed(var20);
                     }
                  } else {
                     in.close();
                  }
               }

            }

            String var24 = result.toString();
            return var24;
         }
      } finally {
         conn.disconnect();
      }

      return null;
   }
}
