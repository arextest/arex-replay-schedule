package com.arextest.schedule.model.converter;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class DesensitizationConverter {
    @Autowired
    private DataDesensitization dataDesensitizationService;

    @Named("encrypt")
    String encrypt(String in) {
        try {
            return dataDesensitizationService.encrypt(in);
        } catch (Exception e) {
            LOGGER.error("Data encrypt failed", e);
        }
        return in;
    }

    @Named("decrypt")
    String decrypt(String in) {
        try {
            return dataDesensitizationService.decrypt(in);
        } catch (Exception e) {
            LOGGER.error("Data decrypt failed", e);
        }
        return in;
    }

    @Named("compress")
    String compress(String decompressString) {
        return CompressionUtils.useZstdCompress(decompressString);
    }

    @Named("decompress")
    String decompress(String compressString) {
        return CompressionUtils.useZstdDecompress(compressString);
    }

    @Named("compressAndEncrypt")
    String compressAndEncrypt(String in) {
        return encrypt(compress(in));
    }

    @Named("decryptAndDecompress")
    String decryptAndDecompress(String in) {
        return decompress(decrypt(in));
    }
}
