package io.arex.replay.schedule.client;


import io.arex.replay.schedule.serialization.ZstdJacksonSerializer;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author jmo
 * @since 2021/11/15
 */
@Component
final class ZstdJacksonMessageConverter extends AbstractHttpMessageConverter<Object> {
    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;

    public static final String ZSTD_JSON_MEDIA_TYPE = "application/zstd-json;charset=UTF-8";

    public ZstdJacksonMessageConverter() {
        super(MediaType.parseMediaType(ZSTD_JSON_MEDIA_TYPE));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        return zstdJacksonSerializer.deserialize(inputMessage.getBody(), clazz);
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {
        zstdJacksonSerializer.serializeTo(o, outputMessage.getBody());
    }
}
