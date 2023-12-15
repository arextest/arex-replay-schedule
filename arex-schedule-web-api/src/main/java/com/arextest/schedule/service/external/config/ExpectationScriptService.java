package com.arextest.schedule.service.external.config;

import com.arextest.common.model.response.GenericResponseType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.web.model.contract.contracts.config.expectation.ExpectationScriptModel;
import com.arextest.web.model.contract.contracts.config.expectation.ExpectationScriptQueryRequest;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/**
 * @since 2023/11/27
 */
@Service
public class ExpectationScriptService {

    @Value("${arex.api.service.api}")
    private String apiServiceUrl;
    @Resource
    private HttpWepServiceApiClient httpClient;

    public List<ExpectationScriptModel> query(String appId) {
        ExpectationScriptQueryRequest queryRequest = new ExpectationScriptQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setValid(true);

        String queryUrl = apiServiceUrl + "/api/config/expectation/query";
        GenericResponseType<List<ExpectationScriptModel>> responseType = httpClient.jsonPost(queryUrl, queryRequest,
            new ParameterizedTypeReference<GenericResponseType<List<ExpectationScriptModel>>>() {});
        if (responseType == null || responseType.getBody() == null) {
            return Collections.emptyList();
        }
        return responseType.getBody();
    }
}
