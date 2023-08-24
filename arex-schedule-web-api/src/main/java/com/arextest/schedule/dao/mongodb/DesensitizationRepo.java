package com.arextest.schedule.dao.mongodb;

import com.arextest.desensitization.extension.DataDesensitization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class DesensitizationRepo {
    @Autowired
    private DataDesensitization dataDesensitizationService;

    String encrypt(String in) {
        try {
            return dataDesensitizationService.encrypt(in);
        } catch (Exception e) {
            LOGGER.error("Data encrypt failed", e);
        }
        return in;
    }

    String decrypt(String in) {
        try {
            return dataDesensitizationService.decrypt(in);
        } catch (Exception e) {
            LOGGER.error("Data decrypt failed", e);
        }
        return in;
    }
}
