package com.vmware.vsan.client.services.fileservice;

import com.google.gson.Gson;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceDomain;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceDomainConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceDomainQuerySpec;
import com.vmware.vim.vsan.binding.vim.vsan.FileShare;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQueryResult;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQuerySpec;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceOvfSpec;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServicePreflightCheckResult;
import com.vmware.vim.vsan.binding.vim.vsan.VsanFileServiceSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.ConfigInfoService;
import com.vmware.vsan.client.services.fileservice.model.FileServiceFeature;
import com.vmware.vsan.client.services.fileservice.model.FileShareConfigs;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationResult;
import com.vmware.vsan.client.services.fileservice.model.FileSharesPaginationSpec;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceCommonConfig;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceDomain;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceOvf;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceOvfProps;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServicePrecheckResult;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceShare;
import com.vmware.vsan.client.services.fileservice.model.VsanFileServiceShareConfig;
import com.vmware.vsan.client.sessionmanager.vlsi.client.http.HttpSettings;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.VsanInventoryHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.data.VsanCapabilityData;
import com.vmware.vsphere.client.vsan.base.util.NetUtils;
import com.vmware.vsphere.client.vsan.base.util.Version;
import com.vmware.vsphere.client.vsan.impl.ConfigureVsanClusterMutationProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

@Component
public class VsanFileServiceConfigService {
   public static final String OVF_VER_SPACE_SEPARATOR = " ";
   public static final String OVF_VER_DASH_SEPARATOR = "-";
   private static final String VDFS_UPLOAD_URL_TEMPLATE = "%s://%s:%s/vsanHealth/fileService/upload/%s";
   private static final String COOKIE_TEMPLATE = "vmware_soap_session=%s; Path=/; HttpOnly; Secure;";
   private static final String COOKIE_HEADER = "Cookie";
   private static final String API_VERSION_KEY = "version";
   private final Log logger = LogFactory.getLog(this.getClass());
   private static final Version FILE_SERVICE_70U1_VERSION = new Version("1.1.0");
   private static final Version FILE_SERVICE_70U2_VERSION = new Version("1.2.0");
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VsanInventoryHelper vsanInventoryHelper;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private ConfigureVsanClusterMutationProvider configureClusterService;
   @Autowired
   private ConfigInfoService configInfoService;

   @TsService
   public int getNumberOfHosts(ManagedObjectReference clusterRef) {
      return this.vsanInventoryHelper.getNumberOfClusterHosts(clusterRef);
   }

   @TsService
   public VsanFileServicePrecheckResult getPrecheckResult(ManagedObjectReference clusterRef, VsanFileServiceDomain domainConfig) {
      Validate.notNull(clusterRef);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      VsanFileServicePrecheckResult var7;
      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         VsanFileServicePreflightCheckResult result = fileServiceSystem.performFileServicePreflightCheck(clusterRef, domainConfig == null ? null : domainConfig.toVmodl(), (ManagedObjectReference)null);
         var7 = VsanFileServicePrecheckResult.fromVmodl(result);
      } catch (Throwable var16) {
         var4 = var16;
         throw var16;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var15) {
                  var4.addSuppressed(var15);
               }
            } else {
               conn.close();
            }
         }

      }

      return var7;
   }

   @TsService
   public ManagedObjectReference upgradeFsvm(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         ManagedObjectReference var6;
         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            ManagedObjectReference task = fileServiceSystem.upgradeFsvm(clusterRef);
            var6 = VmodlHelper.assignServerGuid(task, clusterRef.getServerGuid());
         } catch (Throwable var16) {
            var3 = var16;
            throw var16;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var15) {
                     var3.addSuppressed(var15);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var6;
      } catch (Exception var18) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.configure", "Cannot upgrade fsvm on cluster " + clusterRef, var18, new Object[0]);
      }
   }

   @TsService
   public List getRegisteredOvfs(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);
      ArrayList result = new ArrayList();

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure measure = new Measure("Retrieving VDFS OVFs");
            Throwable var7 = null;

            try {
               Future ovfsFuture = measure.newFuture("VsanFileServiceSystem.queryFileServiceOvfs");
               Future precheckFuture = measure.newFuture("VsanFileServiceSystem.performFileServicePreflightCheck");
               fileServiceSystem.queryFileServiceOvfs(ovfsFuture);
               fileServiceSystem.performFileServicePreflightCheck(clusterRef, (FileServiceDomainConfig)null, (ManagedObjectReference)null, precheckFuture);
               VsanFileServiceOvfSpec[] ovfSpecs = (VsanFileServiceOvfSpec[])ovfsFuture.get();
               VsanFileServicePreflightCheckResult precheckResult = (VsanFileServicePreflightCheckResult)precheckFuture.get();
               if (ArrayUtils.isNotEmpty(ovfSpecs)) {
                  VsanFileServiceOvfSpec[] var12 = ovfSpecs;
                  int var13 = ovfSpecs.length;

                  for(int var14 = 0; var14 < var13; ++var14) {
                     VsanFileServiceOvfSpec ovfSpec = var12[var14];
                     VsanFileServiceOvf ovf = VsanFileServiceOvf.fromVmodl(ovfSpec, clusterRef);
                     result.add(ovf);
                     if (!StringUtils.isEmpty(precheckResult.ovfInstalled) && !StringUtils.isEmpty(ovf.version)) {
                        VsanFileServiceConfigService.BuildInfo ovfBuildInfo = VsanFileServiceConfigService.BuildInfo.fromOvfVersionString(ovf.version);
                        VsanFileServiceConfigService.BuildInfo preflightCheckOvfInfo = VsanFileServiceConfigService.BuildInfo.fromOvfVersionString(precheckResult.ovfInstalled);
                        ovf.isCompatible = ovfBuildInfo.version.equals(preflightCheckOvfInfo.version);
                     }
                  }
               }
            } catch (Throwable var42) {
               var7 = var42;
               throw var42;
            } finally {
               if (measure != null) {
                  if (var7 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var41) {
                        var7.addSuppressed(var41);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Throwable var44) {
            var4 = var44;
            throw var44;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var40) {
                     var4.addSuppressed(var40);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return result;
      } catch (Exception var46) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.loadOvfs", "Cannot load registered OVFS for cluster: " + clusterRef, var46, new Object[0]);
      }
   }

   @TsService
   public ManagedObjectReference downloadPublicOvf(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var3 = null;

         ManagedObjectReference var9;
         try {
            VsanFileServiceSystem vsanVcFileServiceSystem = conn.getVsanFileServiceSystem();
            String ovfUrl = this.getPublicOvfUrl(clusterRef);
            this.logger.debug("Download OVF: " + ovfUrl);
            Measure m = new Measure("VsanFileServiceSystem.downloadFileServiceOvf");
            Throwable var7 = null;

            try {
               ManagedObjectReference taskRef = vsanVcFileServiceSystem.downloadFileServiceOvf(ovfUrl);
               var9 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var7 = var34;
               throw var34;
            } finally {
               if (m != null) {
                  if (var7 != null) {
                     try {
                        m.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var36) {
            var3 = var36;
            throw var36;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var32) {
                     var3.addSuppressed(var32);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var9;
      } catch (Exception var38) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.downloadOvf", "Cannot download OVF for " + clusterRef, var38, new Object[0]);
      }
   }

   @TsService
   public ManagedObjectReference uploadLocalOvf(ManagedObjectReference clusterRef, MultipartFile[] files) {
      Validate.notNull(clusterRef);
      Validate.notEmpty(files);
      boolean var3 = false;

      int responseStatusCode;
      try {
         VsanConnection vsanConnection = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();
            String ovfFilename = null;
            MultipartFile[] var8 = files;
            int var9 = files.length;
            int var10 = 0;

            while(true) {
               if (var10 >= var9) {
                  if (ovfFilename == null) {
                     throw new IllegalArgumentException("No OVF file found!");
                  }

                  String uploadUrl = this.createUploadUrl(vsanConnection, ovfFilename);
                  HttpPost httpPost = new HttpPost(uploadUrl);
                  mpeBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);
                  HttpEntity httpEntity = mpeBuilder.build();
                  httpPost.setEntity(httpEntity);
                  Header cookieHeader = this.createCookieHeader(vsanConnection);
                  httpPost.addHeader(cookieHeader);
                  HttpClient httpClient = NetUtils.createTrustAllHttpClient();
                  HttpResponse response = httpClient.execute(httpPost);
                  responseStatusCode = response.getStatusLine().getStatusCode();
                  this.logger.debug("Status code: " + response.getStatusLine());
                  break;
               }

               MultipartFile file = var8[var10];
               FileItem fileItem = ((CommonsMultipartFile)file).getFileItem();
               String filename = fileItem.getName();
               String contentType = fileItem.getContentType();
               if (isOvf(fileItem)) {
                  if (ovfFilename != null) {
                     throw new IllegalArgumentException("Multiple OVF files found!");
                  }

                  ovfFilename = filename;
               }

               mpeBuilder.addBinaryBody(filename, file.getInputStream(), ContentType.create(contentType), filename);
               ++var10;
            }
         } catch (Throwable var23) {
            var5 = var23;
            throw var23;
         } finally {
            if (vsanConnection != null) {
               if (var5 != null) {
                  try {
                     vsanConnection.close();
                  } catch (Throwable var22) {
                     var5.addSuppressed(var22);
                  }
               } else {
                  vsanConnection.close();
               }
            }

         }
      } catch (Exception var25) {
         this.logger.error("The files are not uploaded successfully, error is " + var25.getMessage());
         throw new VsanUiLocalizableException("vsan.fileservice.error.uploadOvfs", var25);
      }

      if (responseStatusCode != 200) {
         this.logger.error("The files are not uploaded successfully, status: " + responseStatusCode);
         throw new VsanUiLocalizableException("vsan.fileservice.error.uploadOvfs.validate");
      } else {
         return null;
      }
   }

   private static boolean isOvf(FileItem fileItem) {
      return fileItem.getName().trim().toLowerCase().endsWith(".ovf");
   }

   private Header createCookieHeader(VsanConnection conn) {
      String sessionCookie = conn.getSettings().getSessionCookie();
      return new BasicHeader("Cookie", String.format("vmware_soap_session=%s; Path=/; HttpOnly; Secure;", sessionCookie));
   }

   private String createUploadUrl(VsanConnection conn, String fileName) {
      HttpSettings settings = conn.getSettings().getHttpSettings();
      return String.format("%s://%s:%s/vsanHealth/fileService/upload/%s", settings.getProto(), settings.getHost(), settings.getPort(), fileName);
   }

   @TsService
   public VsanFileServiceOvfProps getPublicOvfProps(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);
      VsanFileServiceOvfProps result = new VsanFileServiceOvfProps();
      result.url = this.getPublicOvfUrl(clusterRef);
      return result;
   }

   @TsService
   public ManagedObjectReference configureFileService(ManagedObjectReference clusterRef, VsanFileServiceCommonConfig fileServiceConfig) {
      Validate.notNull(clusterRef);
      Validate.notNull(fileServiceConfig);
      return this.reconfigureFileService(clusterRef, fileServiceConfig.toVmodl());
   }

   @TsService
   public ManagedObjectReference disableFileService(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);
      FileServiceConfig fileServiceConfig = this.getFileServiceConfig(clusterRef);
      if (fileServiceConfig != null) {
         fileServiceConfig.enabled = false;
      } else {
         fileServiceConfig = VsanFileServiceCommonConfig.createDisabledConfig();
      }

      return this.reconfigureFileService(clusterRef, fileServiceConfig);
   }

   @TsService
   public ManagedObjectReference[] editFileServiceConfig(ManagedObjectReference clusterRef, VsanFileServiceCommonConfig fileServiceConfig, String originalDomainName, boolean isRemovingActiveDirectory) {
      ManagedObjectReference domainEditTask = this.editFileServiceDomain(clusterRef, fileServiceConfig, originalDomainName, isRemovingActiveDirectory);
      fileServiceConfig.domainConfig = null;
      ManagedObjectReference fileServiceReconfigTask = this.reconfigureFileService(clusterRef, fileServiceConfig.toVmodl());
      return new ManagedObjectReference[]{domainEditTask, fileServiceReconfigTask};
   }

   private ManagedObjectReference editFileServiceDomain(ManagedObjectReference clusterRef, VsanFileServiceCommonConfig fileServiceCommonConfig, String originalDomainName, boolean isRemovingActiveDirectory) {
      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var6 = null;

         ManagedObjectReference var25;
         try {
            ManagedObjectReference taskRef = null;
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            FileServiceDomainConfig newDomainConfig = fileServiceCommonConfig.domainConfig.toVmodl();
            FileServiceConfig existingFileServiceConfig = this.getFileServiceConfig(clusterRef);
            if (existingFileServiceConfig == null) {
               this.logger.error("The file service config could not be found!");
               throw new VsanUiLocalizableException();
            }

            if (ArrayUtils.isEmpty(existingFileServiceConfig.domains)) {
               taskRef = fileServiceSystem.createFileServiceDomain(newDomainConfig, clusterRef);
            } else {
               FileServiceDomainQuerySpec querySpec = new FileServiceDomainQuerySpec();
               querySpec.names = new String[]{originalDomainName};
               FileServiceDomain[] domains = fileServiceSystem.queryFileServiceDomains(querySpec, clusterRef);
               if (!ArrayUtils.isEmpty(domains)) {
                  String domainUuid = this.findDomainUuidByName(domains, originalDomainName);
                  if (domainUuid != null) {
                     taskRef = fileServiceSystem.reconfigureFileServiceDomain(domainUuid, newDomainConfig, clusterRef, isRemovingActiveDirectory ? new String[]{"directoryServerConfig"} : null);
                  } else {
                     this.logger.warn("The domainUuid is missing, domainName = " + originalDomainName);
                  }
               }
            }

            if (taskRef != null) {
               VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            }

            var25 = taskRef;
         } catch (Throwable var22) {
            var6 = var22;
            throw var22;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var21) {
                     var6.addSuppressed(var21);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var25;
      } catch (Exception var24) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.reconfigDomain", "Cannot reconfigure the file service domain", var24, new Object[0]);
      }
   }

   private String findDomainUuidByName(FileServiceDomain[] domains, String domainName) {
      FileServiceDomain[] var3 = domains;
      int var4 = domains.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         FileServiceDomain domain = var3[var5];
         if (domainName.equals(domain.config.name)) {
            return domain.uuid;
         }
      }

      return null;
   }

   private ManagedObjectReference reconfigureFileService(ManagedObjectReference clusterRef, FileServiceConfig fileServiceConfig) {
      ReconfigSpec reconfigSpec = new ReconfigSpec();
      reconfigSpec.modify = true;
      reconfigSpec.fileServiceConfig = fileServiceConfig;
      return this.configureClusterService.startReconfigureTask(clusterRef, reconfigSpec);
   }

   @TsService
   public FileSharesPaginationResult listSharesPerDomain(ManagedObjectReference clusterRef, FileSharesPaginationSpec spec, boolean paginationSupported) {
      Validate.notNull(clusterRef);
      Validate.notNull(spec);

      try {
         Measure measure = new Measure("Retrieve the specified page of file shares");
         Throwable var5 = null;

         FileSharesPaginationResult var8;
         try {
            VsanAsyncDataRetriever fileShareRetriver = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadFileShares(spec);
            if (paginationSupported) {
               fileShareRetriver.loadFileSharesCount(spec);
            }

            FileSharesPaginationResult result = new FileSharesPaginationResult();
            result.shares = this.toVsanFileShareList(fileShareRetriver.getFileShares());
            result.total = paginationSupported ? fileShareRetriver.getFileSharesCountResult() : result.shares.size();
            var8 = result;
         } catch (Throwable var18) {
            var5 = var18;
            throw var18;
         } finally {
            if (measure != null) {
               if (var5 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var17) {
                     var5.addSuppressed(var17);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var8;
      } catch (Exception var20) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.loadShares", "Cannot load file shares for cluster '" + clusterRef + "' and domain '" + spec.domainName + "'", var20, new Object[0]);
      }
   }

   private List toVsanFileShareList(List shares) {
      List result = new ArrayList();
      if (!CollectionUtils.isEmpty(shares)) {
         Iterator var3 = shares.iterator();

         while(var3.hasNext()) {
            FileShare share = (FileShare)var3.next();
            result.add(VsanFileServiceShare.fromVmodl(share));
         }
      }

      return result;
   }

   @TsService
   public List listAllShares(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);

      try {
         Measure measure = new Measure("Retrieving all file shares");
         Throwable var3 = null;

         List var5;
         try {
            VsanAsyncDataRetriever fileShareRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadFileShares();
            var5 = this.toVsanFileShareList(fileShareRetriever.getFileShares());
         } catch (Throwable var15) {
            var3 = var15;
            throw var15;
         } finally {
            if (measure != null) {
               if (var3 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var14) {
                     var3.addSuppressed(var14);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var5;
      } catch (Exception var17) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.loadShares", "Cannot load file shares for cluster " + clusterRef, var17, new Object[0]);
      }
   }

   @TsService
   public List getFileShareAttributes(ManagedObjectReference clusterRef, String[] keys) {
      try {
         FileShareConfigs shareConfigs = this.getFileServiceConfigs(clusterRef, keys);
         return shareConfigs.toShareList();
      } catch (Exception var4) {
         this.logger.error("Failed to load file share properties", var4);
         throw new VsanUiLocalizableException("vsan.fileservice.error.parseJson", "Cannot load file shares for cluster " + clusterRef, var4, new Object[0]);
      }
   }

   @TsService
   public Map isFeatureSupportedOnRuntime(ManagedObjectReference clusterRef, FileServiceFeature[] features) {
      Validate.notNull(clusterRef);
      HashMap result = new HashMap();

      try {
         FileShareConfigs fileServiceConfig = this.getFileServiceConfigs(clusterRef, new String[]{"version"});
         if (ArrayUtils.isNotEmpty(fileServiceConfig.version)) {
            String apiVersion = fileServiceConfig.version[0];
            Arrays.stream(features).forEach((feature) -> {
               result.put(feature, this.getRuntimeEnableStatus(apiVersion, feature) && this.getFeatureVcCapability(clusterRef, feature));
            });
            return result;
         }
      } catch (Exception var6) {
         this.logger.warn("Failed to query file service runtime API version.", var6);
      }

      return result;
   }

   private boolean getRuntimeEnableStatus(String runtimeApiVersion, FileServiceFeature feature) {
      Version runtimeVersion = new Version(runtimeApiVersion);
      switch(feature) {
      case SMB:
      case KERBEROS:
      case PAGINATION:
         return runtimeVersion.compareTo(FILE_SERVICE_70U1_VERSION) >= 0;
      case OWE:
      case SNAPSHOT:
      case AFFINITY_SITE:
      case SMB_PERFORMANCE:
         return runtimeVersion.compareTo(FILE_SERVICE_70U2_VERSION) >= 0;
      default:
         this.logger.error("Unsupported feature: " + feature);
         return false;
      }
   }

   private boolean getFeatureVcCapability(ManagedObjectReference clusterRef, FileServiceFeature feature) {
      VsanCapabilityData vcCapabilities = VsanCapabilityUtils.getVcCapabilities(clusterRef);
      switch(feature) {
      case SMB:
         return vcCapabilities.isSmbProtocolSupported;
      case KERBEROS:
         return vcCapabilities.isFileServiceKerberosSupported;
      case PAGINATION:
         return vcCapabilities.isFileSharePaginationSupported;
      case OWE:
         return vcCapabilities.isFileServiceOweSupported;
      case SNAPSHOT:
         return vcCapabilities.isFileServiceSnapshotSupported;
      case AFFINITY_SITE:
         return vcCapabilities.isFileServiceStretchedClusterSupported;
      case SMB_PERFORMANCE:
         return vcCapabilities.isSmbPerformanceSupported;
      default:
         return false;
      }
   }

   private FileShareConfigs getFileServiceConfigs(ManagedObjectReference clusterRef, String[] keys) {
      String resultJson = "";

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure m = new Measure("VsanFileServiceSystem.queryFileServiceConfigs");
            Throwable var8 = null;

            try {
               resultJson = fileServiceSystem.queryFileServiceConfigs(keys, clusterRef);
            } catch (Throwable var33) {
               var8 = var33;
               throw var33;
            } finally {
               if (m != null) {
                  if (var8 != null) {
                     try {
                        m.close();
                     } catch (Throwable var32) {
                        var8.addSuppressed(var32);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var35) {
            var5 = var35;
            throw var35;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var31) {
                     var5.addSuppressed(var31);
                  }
               } else {
                  conn.close();
               }
            }

         }
      } catch (Exception var37) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.loadShares", "Cannot load file service configuration for cluster " + clusterRef, var37, new Object[0]);
      }

      Gson g = new Gson();
      return (FileShareConfigs)g.fromJson(resultJson, FileShareConfigs.class);
   }

   @TsService
   public ManagedObjectReference createShare(ManagedObjectReference clusterRef, VsanFileServiceShareConfig share) {
      Validate.notNull(clusterRef);
      Validate.notNull(share);
      FileShareConfig shareConfig = share.toVmodl();

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         ManagedObjectReference var10;
         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure m = new Measure("VsanFileServiceSystem.createFileShare");
            Throwable var8 = null;

            try {
               ManagedObjectReference taskRef = fileServiceSystem.createFileShare(shareConfig, clusterRef);
               var10 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            } catch (Throwable var35) {
               var8 = var35;
               throw var35;
            } finally {
               if (m != null) {
                  if (var8 != null) {
                     try {
                        m.close();
                     } catch (Throwable var34) {
                        var8.addSuppressed(var34);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var37) {
            var5 = var37;
            throw var37;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var33) {
                     var5.addSuppressed(var33);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var10;
      } catch (Exception var39) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.createShare", "Cannot create a share: " + shareConfig, var39, new Object[0]);
      }
   }

   @TsService
   public ManagedObjectReference updateShare(ManagedObjectReference clusterRef, String shareUuid, VsanFileServiceShareConfig share, String[] removedLabelKeys) {
      Validate.notNull(clusterRef);
      Validate.notNull(share);
      FileShareConfig shareConfig = share.toVmodl();

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var7 = null;

         ManagedObjectReference var12;
         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure m = new Measure("VsanFileServiceSystem.reconfigureFileShare");
            Throwable var10 = null;

            try {
               ManagedObjectReference taskRef = fileServiceSystem.reconfigureFileShare(shareUuid, shareConfig, clusterRef, removedLabelKeys, (Boolean)null);
               var12 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
            } catch (Throwable var37) {
               var10 = var37;
               throw var37;
            } finally {
               if (m != null) {
                  if (var10 != null) {
                     try {
                        m.close();
                     } catch (Throwable var36) {
                        var10.addSuppressed(var36);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var39) {
            var7 = var39;
            throw var39;
         } finally {
            if (conn != null) {
               if (var7 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var35) {
                     var7.addSuppressed(var35);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var12;
      } catch (Exception var41) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.editShare", "Cannot update share: " + shareConfig, var41, new Object[0]);
      }
   }

   @TsService
   public List queryShare(ManagedObjectReference clusterRef, String shareName) {
      Validate.notNull(clusterRef);
      FileShareQuerySpec querySpec = new FileShareQuerySpec();
      querySpec.names = new String[]{shareName};

      try {
         VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
         Throwable var5 = null;

         ArrayList var45;
         try {
            VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
            Measure m = new Measure("VsanFileServiceSystem.queryFileShares");
            Throwable var8 = null;

            try {
               FileShareQueryResult ret = fileServiceSystem.queryFileShares(querySpec, clusterRef);
               FileShare[] shares = null;
               if (ret != null) {
                  shares = ret.fileShares;
               }

               List results = new ArrayList();
               if (ArrayUtils.isNotEmpty(shares)) {
                  FileShare[] var12 = shares;
                  int var13 = shares.length;

                  for(int var14 = 0; var14 < var13; ++var14) {
                     FileShare fileShare = var12[var14];
                     VsanFileServiceShare share = VsanFileServiceShare.fromVmodl(fileShare);
                     results.add(share);
                  }
               }

               var45 = results;
            } catch (Throwable var40) {
               var8 = var40;
               throw var40;
            } finally {
               if (m != null) {
                  if (var8 != null) {
                     try {
                        m.close();
                     } catch (Throwable var39) {
                        var8.addSuppressed(var39);
                     }
                  } else {
                     m.close();
                  }
               }

            }
         } catch (Throwable var42) {
            var5 = var42;
            throw var42;
         } finally {
            if (conn != null) {
               if (var5 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var38) {
                     var5.addSuppressed(var38);
                  }
               } else {
                  conn.close();
               }
            }

         }

         return var45;
      } catch (Exception var44) {
         throw new VsanUiLocalizableException("vsan.fileservice.error.queryShare", "Cannot query a share: " + shareName, var44, new Object[0]);
      }
   }

   @TsService
   public List deleteShare(ManagedObjectReference clusterRef, List shareUuids) {
      Validate.notNull(clusterRef);
      Validate.notNull(shareUuids);
      List taskRefs = new ArrayList();
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         Iterator var7 = shareUuids.iterator();

         while(var7.hasNext()) {
            String shareUuid = (String)var7.next();

            try {
               Measure m = new Measure("VsanFileServiceSystem.removeFileShare");
               Throwable var10 = null;

               try {
                  ManagedObjectReference taskRef = fileServiceSystem.removeFileShare(shareUuid, clusterRef, (Boolean)null);
                  taskRefs.add(VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid()));
               } catch (Throwable var35) {
                  var10 = var35;
                  throw var35;
               } finally {
                  if (m != null) {
                     if (var10 != null) {
                        try {
                           m.close();
                        } catch (Throwable var34) {
                           var10.addSuppressed(var34);
                        }
                     } else {
                        m.close();
                     }
                  }

               }
            } catch (Exception var37) {
               throw new VsanUiLocalizableException("vsan.fileservice.error.deleteShare", "Cannot delete share with UUID: " + shareUuid, var37, new Object[0]);
            }
         }

         ArrayList var40 = taskRefs;
         return var40;
      } catch (Throwable var38) {
         var5 = var38;
         throw var38;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var33) {
                  var5.addSuppressed(var33);
               }
            } else {
               conn.close();
            }
         }

      }
   }

   @TsService
   public List getDnsNames(ManagedObjectReference clusterRef, List ipAddresses, List dnsServers) {
      Validate.notNull(clusterRef);
      Validate.notEmpty(ipAddresses);
      Validate.notEmpty(dnsServers);
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      List var11;
      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();

         try {
            Measure measure = new Measure("Retrieving DNS names");
            Throwable var8 = null;

            try {
               Future dnsNamesFuture = measure.newFuture("fileServiceSystem.queryDnsNamesFromIps");
               fileServiceSystem.queryDnsNamesFromIps((String[])ipAddresses.toArray(new String[0]), (String[])dnsServers.toArray(new String[0]), dnsNamesFuture);
               String[] dnsNames = (String[])dnsNamesFuture.get();
               var11 = Arrays.asList(dnsNames);
            } catch (Throwable var36) {
               var8 = var36;
               throw var36;
            } finally {
               if (measure != null) {
                  if (var8 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var35) {
                        var8.addSuppressed(var35);
                     }
                  } else {
                     measure.close();
                  }
               }

            }
         } catch (Exception var38) {
            throw new VsanUiLocalizableException("vsan.fileservice.error.queryDNS", "Cannot query DNS names for IP addresses: " + ipAddresses + " from DNS server: " + dnsServers, var38, new Object[0]);
         }
      } catch (Throwable var39) {
         var5 = var39;
         throw var39;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var34) {
                  var5.addSuppressed(var34);
               }
            } else {
               conn.close();
            }
         }

      }

      return var11;
   }

   @TsService
   public VsanFileServiceCommonConfig getConfig(ManagedObjectReference clusterRef) {
      Validate.notNull(clusterRef);
      if (!VsanCapabilityUtils.getCapabilities(clusterRef).isFileServiceSupported) {
         throw new UnsupportedOperationException("This cluster does not support File Services");
      } else {
         VsanFileServiceCommonConfig fileServiceCommonConfig = VsanFileServiceCommonConfig.fromVmodl(this.configInfoService.getVsanConfigInfo(clusterRef), clusterRef);
         return fileServiceCommonConfig;
      }
   }

   private FileServiceConfig getFileServiceConfig(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.configInfoService.getVsanConfigInfo(clusterRef);
      return configInfoEx != null && configInfoEx.fileServiceConfig != null ? configInfoEx.fileServiceConfig : null;
   }

   private String getPublicOvfUrl(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      String var5;
      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         var5 = fileServiceSystem.findOvfDownloadUrl(clusterRef);
      } catch (Throwable var14) {
         var3 = var14;
         throw var14;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var13) {
                  var3.addSuppressed(var13);
               }
            } else {
               conn.close();
            }
         }

      }

      return var5;
   }

   @TsService
   public ManagedObjectReference rebalanceFileService(ManagedObjectReference clusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      ManagedObjectReference var8;
      try {
         VsanFileServiceSystem fileServiceSystem = conn.getVsanFileServiceSystem();
         Measure measure = new Measure("fileServiceSystem.rebalanceFileService");
         Throwable var6 = null;

         try {
            ManagedObjectReference task = fileServiceSystem.rebalanceFileService(clusterRef);
            var8 = VmodlHelper.assignServerGuid(task, clusterRef.getServerGuid());
         } catch (Throwable var31) {
            var6 = var31;
            throw var31;
         } finally {
            if (measure != null) {
               if (var6 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var30) {
                     var6.addSuppressed(var30);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Throwable var33) {
         var3 = var33;
         throw var33;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var29) {
                  var3.addSuppressed(var29);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   private static class BuildInfo {
      private String build;
      private String version;
      private boolean isSandbox;

      public static VsanFileServiceConfigService.BuildInfo fromOvfVersionString(String ovfVersion) {
         Validate.notEmpty(ovfVersion);
         VsanFileServiceConfigService.BuildInfo buildInfo = new VsanFileServiceConfigService.BuildInfo();
         String[] chunks;
         if (ovfVersion.contains("-")) {
            chunks = ovfVersion.split("-");
            if (chunks.length < 2) {
               throw new IllegalArgumentException("Invalid version format: " + ovfVersion);
            }

            buildInfo.version = chunks[0];
            buildInfo.build = chunks[1];
         } else if (ovfVersion.contains(" ")) {
            chunks = ovfVersion.split(" ");
            if (chunks.length < 3) {
               throw new IllegalArgumentException("Invalid version format: " + ovfVersion);
            }

            buildInfo.version = chunks[0];
            buildInfo.build = chunks[2];
         } else {
            buildInfo.version = ovfVersion;
         }

         return buildInfo;
      }

      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            VsanFileServiceConfigService.BuildInfo buildInfo = (VsanFileServiceConfigService.BuildInfo)o;
            return this.isSandbox == buildInfo.isSandbox && Objects.equals(this.build, buildInfo.build) && Objects.equals(this.version, buildInfo.version);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.build, this.version, this.isSandbox});
      }
   }
}
