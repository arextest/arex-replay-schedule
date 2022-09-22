package com.arextest.replay.schedule.model.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wang_yc on 2021/10/14
 */
@Data
public class ReplayComparisonConfig {
    // ignore according to type
    private List<String> ignoreTypeList;
    // ignore according to interface
    private List<String> ignoreKeyList;

    private Set<List<String>> exclusionList;
    private Set<List<String>> inclusionList;

    @JsonDeserialize(keyUsing = MapKeyDeserializerUtils.class)
    @JsonSerialize(keyUsing = MapKeySerializerUtils.class)
    private Map<List<String>, List<String>> referenceMap;

    @JsonDeserialize(keyUsing = MapKeyDeserializerUtils.class)
    @JsonSerialize(keyUsing = MapKeySerializerUtils.class)
    private Map<List<String>, List<List<String>>> listSortMap;
    private Map<String, List<List<String>>> decompressConfig;

    public final boolean checkIgnoreMockMessageType(String type) {
        return ignoreTypeList != null && ignoreTypeList.contains(type);
    }

    private static class MapKeyDeserializerUtils extends KeyDeserializer {

        @Override
        public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(s, new TypeReference<List<String>>() {
            });
        }
    }

    private static class MapKeySerializerUtils extends JsonSerializer<List<String>> {

        @Override
        public void serialize(List<String> stringList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            String string = objectMapper.writeValueAsString(stringList);
            jsonGenerator.writeFieldName(string);
        }
    }

}
