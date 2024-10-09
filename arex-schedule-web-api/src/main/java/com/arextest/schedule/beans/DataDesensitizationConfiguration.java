package com.arextest.schedule.beans;

import com.arextest.common.desensitization.DesensitizationProvider;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.extension.desensitization.DataDesensitization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;


@Configuration
@Slf4j
public class DataDesensitizationConfiguration {

  @Bean
  @ConditionalOnMissingBean(DesensitizationProvider.class)
  DesensitizationProvider desensitizationProvider(MongoDatabaseFactory factory) {
    String desensitizationJarUrl = DataDesensitizationUtils.getDesensitizationJarUrl(
        factory.getMongoDatabase());
    return new DesensitizationProvider(desensitizationJarUrl);
  }

  @Bean
  DataDesensitization dataDesensitization(DesensitizationProvider desensitizationProvider) {
    return desensitizationProvider.get();
  }


  private static class DataDesensitizationUtils {

    private static final String SYSTEM_CONFIGURATION = "SystemConfiguration";
    private static final String DESENSITIZATION_JAR = "desensitizationJar";
    private static final String JAR_URL = "jarUrl";

    private static String getDesensitizationJarUrl(MongoDatabase database) {
      MongoCollection<Document> collection = database.getCollection(SYSTEM_CONFIGURATION);
      Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key,
          KeySummary.DESERIALIZATION_JAR);
      Document document = collection.find(filter).first();
      if (document != null && document.get(DESENSITIZATION_JAR) != null) {
        return document.get(DESENSITIZATION_JAR, Document.class).getString(JAR_URL);
      }
      return null;
    }
  }
}
