package com.arextest.replay.schedule.dao.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/12/21
 */
@Slf4j
@MappedTypes(HashMap.class)
public class StringToHashMapHandler extends BaseTypeHandler<Map<String, String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
            throws SQLException {
        if (MapUtils.isEmpty(parameter)) {
            ps.setString(i, StringUtils.EMPTY);
            return;
        }
        ps.setString(i, writeValue(parameter));
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String source = rs.getString(columnName);
        return spiltToCollection(source);
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String source = rs.getString(columnIndex);
        return spiltToCollection(source);
    }

    @Override
    public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String source = cs.getString(columnIndex);
        return spiltToCollection(source);
    }

    private Map<String, String> spiltToCollection(String source) {
        if (StringUtils.isEmpty(source)) {
            return Collections.emptyMap();
        }
        return readValue(source);
    }

    private Map<String, String> readValue(String source) {
        try {
            return OBJECT_MAPPER.readValue(source, MAP_TYPE);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    private String writeValue(Map<String, String> source) {
        try {
            return OBJECT_MAPPER.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private static final class MapTypeReference extends TypeReference<Map<String, String>> {
        private MapTypeReference() {
        }
    }

    private static final MapTypeReference MAP_TYPE = new MapTypeReference();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

}
