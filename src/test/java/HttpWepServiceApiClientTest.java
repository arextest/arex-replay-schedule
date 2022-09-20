import com.arextest.replay.schedule.WebSpringBootServletInitializer;
import com.arextest.replay.schedule.client.HttpWepServiceApiClient;
import com.arextest.replay.schedule.model.config.CompareExclusionsConfig;
import lombok.Data;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by rchen9 on 2022/9/19.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {WebSpringBootServletInitializer.class})
public class HttpWepServiceApiClientTest {

    @Autowired
    private HttpWepServiceApiClient httpWepServiceApiClient;

    private String configComparisonExclusionsUrl = "http://10.32.183.158:8080/api/config/comparison/exclusions/useResultAsList/appId/{appId}";

    @Test
    public void testHttp() {
        Map<String, String> urlVariables = Collections.singletonMap("appId", "arex.1.20220909A11");
        ResponseEntity<GenericResponseType<CompareExclusionsConfig>> genericResponseTypeResponseEntity =
                httpWepServiceApiClient.get(configComparisonExclusionsUrl, urlVariables, new ParameterizedTypeReference<GenericResponseType<CompareExclusionsConfig>>() {
                });

        System.out.println();
    }

    @Data
    private final static class GenericResponseType<T> {
        private List<T> body;
    }


}
