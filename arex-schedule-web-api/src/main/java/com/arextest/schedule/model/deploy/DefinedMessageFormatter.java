package com.arextest.schedule.model.deploy;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class DefinedMessageFormatter {
    @JsonAlias("Xml")
    private String xml;
    @JsonAlias("Json")
    private String json;
}