package com.arextest.schedule.serialization;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;


@Slf4j
@RequiredArgsConstructor
public class DesensitizationProvider {

  private static final String SYSTEM_CONFIGURATION = "SystemConfiguration";
  private static final String DESENSITIZATION_JAR = "desensitizationJar";
  private static final String JAR_URL = "jarUrl";

  private volatile DataDesensitization desensitizationService;

  private final MongoDatabase mongoDatabase;

  public DataDesensitization get() {
    if (desensitizationService == null) {
      synchronized (DesensitizationProvider.class) {
        if (desensitizationService == null) {
          try {
            String jarUrl = getJarUrl();
            desensitizationService = loadDesensitization(jarUrl);
            LOGGER.info("load desensitization success, className:{}",
                desensitizationService.getClass().getName());
          } catch (Exception runtimeException) {
            LOGGER.error("load desensitization error", runtimeException);
            throw new RuntimeException(runtimeException.getMessage());
          }
        }
      }
    }
    return desensitizationService;
  }

  protected String getJarUrl() {
    MongoCollection<Document> collection = mongoDatabase.getCollection(SYSTEM_CONFIGURATION);
    Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key,
        KeySummary.DESERIALIZATION_JAR);
    Document document = collection.find(filter).first();
    if (document != null && document.get(DESENSITIZATION_JAR) != null) {
      return document.get(DESENSITIZATION_JAR, Document.class).getString(JAR_URL);
    }
    return null;
  }

  protected DataDesensitization loadDesensitization(String remoteJarUrl)
      throws Exception {
    DataDesensitization dataDesensitization = new DefaultDataDesensitization();
    if (StringUtils.isEmpty(remoteJarUrl)) {
      return dataDesensitization;
    }
    RemoteJarClassLoader remoteJarClassLoader = RemoteJarLoaderUtils.loadJar(remoteJarUrl);
    dataDesensitization = RemoteJarLoaderUtils
        .loadService(DataDesensitization.class, remoteJarClassLoader)
        .get(0);
    return dataDesensitization;
  }

}