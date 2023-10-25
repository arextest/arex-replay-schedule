package com.arextest.schedule.serialization;


import com.arextest.common.serialization.SerializationProvider;
import com.arextest.common.serialization.SerializationProviders;
import com.arextest.common.utils.SerializationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2021/11/8
 */
@Component
@Slf4j
public final class ZstdJacksonSerializer {

  @Resource
  private ObjectMapper objectMapper;
  private SerializationProvider serializationProvider;

  @PostConstruct
  void initSerializationProvider() {
    this.serializationProvider = SerializationProviders.jacksonProvider(this.objectMapper);
  }

  public <T> void serializeTo(T value, OutputStream outputStream) {
    if (value == null) {
      return;
    }
    SerializationUtils.useZstdSerializeTo(this.serializationProvider, outputStream, value);
  }

  public <T> T deserialize(InputStream inputStream, Class<T> clazz) {
    if (inputStream == null) {
      return null;
    }
    return SerializationUtils.useZstdDeserialize(this.serializationProvider, inputStream, clazz);
  }

  public <T> T deserialize(String base64Text, Class<T> clazz) {
    if (StringUtils.isEmpty(base64Text)) {
      return null;
    }
    return deserialize(Base64.getDecoder().decode(base64Text), clazz);
  }

  public <T> T deserialize(byte[] zstdValues, Class<T> clazz) {
    if (zstdValues == null) {
      return null;
    }
    return SerializationUtils.useZstdDeserialize(this.serializationProvider, zstdValues, clazz);
  }
}