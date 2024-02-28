package com.arextest.schedule.beans;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import com.arextest.model.mock.Mocker;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;


@Slf4j
@Configuration(proxyBeanMethods = false)
public class MongodbConfiguration {

  @Value("${arex.mongo.uri}")
  private String mongoUrl;

  @Bean
  @ConditionalOnMissingBean
  public MongoDatabaseFactory mongoDbFactory() {
    try {
      CompressionMongoClientDatabaseFactory fac = new CompressionMongoClientDatabaseFactory(
          mongoUrl);
      MongoDatabase db = fac.getMongoDatabase();
      ensureIndex(db);
      return fac;
    } catch (Exception e) {
      LOGGER.error("cannot connect mongodb {}", e.getMessage(), e);
      throw e;
    }
  }


  private void ensureIndex(MongoDatabase db) {
    Document index = new Document();
    // run details
    index.append(ReplayRunDetailsCollection.Fields.PLAN_ID, 1);
    index.append(ReplayRunDetailsCollection.Fields.SEND_STATUS, 1);
    db.getCollection(ReplayRunDetailsCollection.COLLECTION_NAME).createIndex(index);

    // biz log
    index = new Document();
    index.append(BizLog.Fields.PLAN_ID, 1);
    db.getCollection(ReplayBizLogCollection.COLLECTION_NAME).createIndex(index);
  }

  @Bean
  @ConditionalOnMissingBean
  public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver,
        new MongoMappingContext());
    converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    converter.afterPropertiesSet();
    return new MongoTemplate(mongoDatabaseFactory, converter);
  }

  @Bean
  public SystemConfigurationRepositoryImpl systemConfigurationRepository (
      MongoTemplate mongoTemplate) {
    return new SystemConfigurationRepositoryImpl(mongoTemplate.getDb());
  }

  public static class CompressionMongoClientDatabaseFactory extends
      SimpleMongoClientDatabaseFactory {

    public CompressionMongoClientDatabaseFactory(String connectionString) {
      super(connectionString);
    }

    @Override
    public CodecRegistry getCodecRegistry() {
      CodecRegistry compressionCodecRegistry =
          CodecRegistries.fromCodecs(new CompressionCodecImpl<>(Mocker.Target.class));
      final CodecRegistry customPojo = CodecRegistries.fromProviders(compressionCodecRegistry,
          PojoCodecProvider
              .builder().automatic(true).build());
      return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
          customPojo);

    }

    private static final class CompressionCodecImpl<T> implements Codec<T> {

      private final Class<T> target;

      CompressionCodecImpl(Class<T> target) {
        this.target = target;
      }

      @Override
      public T decode(BsonReader reader, DecoderContext decoderContext) {
        return SerializationUtils.useZstdDeserialize(reader.readString(), this.target);
      }

      @Override
      public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        String base64Result = SerializationUtils.useZstdSerializeToBase64(value);
        writer.writeString(base64Result);
      }

      @Override
      public Class<T> getEncoderClass() {
        return target;
      }
    }
  }
}