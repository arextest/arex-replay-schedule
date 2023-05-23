package com.arextest.schedule.model.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomProtocolConfig {
    private String protocolName;

    private String jarFilePath;
}
