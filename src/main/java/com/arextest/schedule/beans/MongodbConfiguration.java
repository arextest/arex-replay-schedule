package com.arextest.schedule.beans;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Configuration
public class MongodbConfiguration {
    @Value("${mongo.uri}")
    private String mongoUrl;


    @Bean
    public MongoDatabaseFactory mongoDbFactory() {
        try {

            return new SimpleMongoClientDatabaseFactory(mongoUrl);
        } catch (Exception e) {
            LOGGER.error("cannot connect mongodb", e);
        }
        return null;
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(this.mongoDbFactory());
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        MongoTemplate template = new MongoTemplate(this.mongoDbFactory(), converter);
        return template;
    }
}