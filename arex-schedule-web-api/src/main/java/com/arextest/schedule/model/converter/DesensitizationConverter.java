package com.arextest.schedule.model.converter;

import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.schedule.utils.ZstdUtils;
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
        return ZstdUtils.compressString(decompressString);
    }

    @Named("decompress")
    String decompress(String compressString) {
        return ZstdUtils.uncompressString(compressString);
    }

    @Named("encryptAndCompress")
    String encryptAndCompress(String in) {
        return encrypt(compress(in));
    }

    @Named("decompressAndDecrypt")
    String decompressAndDecrypt(String in) {
        return decompress(decrypt(in));
    }
}
