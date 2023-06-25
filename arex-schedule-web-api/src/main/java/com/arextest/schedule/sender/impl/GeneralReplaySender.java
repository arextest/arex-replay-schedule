package com.arextest.schedule.sender.impl;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.converter.ReplayActionCaseItemConverter;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.SenderParameters;
import com.arextest.schedule.spi.ReplayInvokerExtension;
import com.arextest.schedule.spi.model.BaseRequest;
import com.arextest.schedule.spi.model.ReplayInvokeResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.ServiceLoader;

@Slf4j
@Component
public class GeneralReplaySender extends AbstractReplaySender {
    private static final String EXTENSION_NAME = System.getProperty("replay.sender.extension.name");

    @Override
    public boolean isSupported(String categoryType) {
        return !MockCategoryType.DUBBO_PROVIDER.getName().equals(categoryType)
                && !MockCategoryType.SERVLET.getName().equals(categoryType);
    }

    @Override
    public boolean send(ReplayActionCaseItem caseItem) {
        Map<String, String> headers = createHeaders(caseItem);
        ReplayActionItem replayActionItem = caseItem.getParent();
        before(caseItem.getRecordId(), replayActionItem.getParent().getReplayPlanType());

        BaseRequest baseRequest = generateBaseRequest(caseItem);
        baseRequest.setHeaders(headers);

        ReplayInvokeResult replayInvokeResult = null;
        ServiceLoader<ReplayInvokerExtension> loader = ServiceLoader.load(ReplayInvokerExtension.class);
        for (ReplayInvokerExtension sender : loader) {
            if (sender.getName().equalsIgnoreCase(EXTENSION_NAME)) {
                replayInvokeResult = sender.invoke(baseRequest);
            }
        }
        if (replayInvokeResult == null) {
            return false;
        }

        ReplaySendResult targetSendResult = fromResult(baseRequest, replayInvokeResult);
        caseItem.setSendErrorMessage(targetSendResult.getRemark());
        caseItem.setTargetResultId(targetSendResult.getTraceId());
        caseItem.setSendStatus(targetSendResult.getStatusType().getValue());
        return targetSendResult.success();
    }

    private ReplaySendResult fromResult(BaseRequest request, ReplayInvokeResult result) {
        String url = request.getUrl();
        if (MapUtils.isEmpty(result.getResponseHeaders())) {
            return ReplaySendResult.failed("replay post error,review log find more details", url);
        }
        String traceId = result.getResponseHeaders().get(CommonConstant.AREX_REPLAY_ID);
        if (StringUtils.isEmpty(traceId)) {
            return ReplaySendResult.failed("Could not fetch replay result id from the headers of dubbo response", url);
        }
        return ReplaySendResult.success(traceId, StringUtils.EMPTY, url);
    }

    @Override
    public ReplaySendResult send(SenderParameters senderParameters) {
        return null;
    }

    BaseRequest generateBaseRequest(ReplayActionCaseItem caseItem) {
        BaseRequest baseRequest = ReplayActionCaseItemConverter.INSTANCE.convertBaseRequest(caseItem);
        ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(), caseItem.getParent().getTargetInstance());
        if (instanceRunner == null) {
            return null;
        }
        String url = instanceRunner.getUrl();
        baseRequest.setUrl(url);
        return baseRequest;
    }
}
