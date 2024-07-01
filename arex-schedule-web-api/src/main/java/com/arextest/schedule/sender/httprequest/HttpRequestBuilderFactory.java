package com.arextest.schedule.sender.httprequest;

import com.arextest.schedule.sender.SenderParameters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HttpRequestBuilderFactory {

  final List<AbstractHttpRequestBuilder> httpRequestBuilders;

  public AbstractHttpRequestBuilder getHttpRequestBuilder(SenderParameters senderParameters) {
    for (AbstractHttpRequestBuilder httpRequestBuilder : httpRequestBuilders) {
      if (httpRequestBuilder.supportBuild(senderParameters)) {
        return httpRequestBuilder;
      }
    }
    return null;
  }

}
