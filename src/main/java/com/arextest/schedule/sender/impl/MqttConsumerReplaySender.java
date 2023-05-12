package com.arextest.schedule.sender.impl;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.sender.SenderParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * @author : MentosL
 * @date : 2023/5/11 23:16
 */
@Slf4j
@Component
public class MqttConsumerReplaySender extends AbstractReplaySender{
    @Override
    public boolean isSupported(String categoryType) {
        return "MqttMessageConsumer".equals(categoryType);
    }

    @Override
    public boolean send(ReplayActionCaseItem caseItem) {
        Map<String, String> headers = newHeadersIfEmpty(caseItem.requestHeaders());
        ReplayActionItem replayActionItem = caseItem.getParent();
        before(caseItem.getRecordId(), BuildReplayPlanType.BY_ROLLING_CASE.getValue());
        headers.remove(CommonConstant.AREX_REPLAY_WARM_UP);
        headers.put(CommonConstant.AREX_RECORD_ID, caseItem.getRecordId());
        String exclusionOperationConfig = replayActionItem.getExclusionOperationConfig();
        if (StringUtils.isNotEmpty(exclusionOperationConfig)) {
            headers.put(CommonConstant.X_AREX_EXCLUSION_OPERATIONS, exclusionOperationConfig);
        }
        return doSend(caseItem, headers);
    }

    private boolean doSend(ReplayActionCaseItem caseItem, Map<String, String> headers) {
        try {
            ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(), caseItem.getParent().getTargetInstance());
            if (instanceRunner == null) {
                return false;
            }
            String url = instanceRunner.getUrl();
            MqttClient client = new MqttClient(url, getClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            String body = caseItem.getTargetRequest().getBody();
            if (StringUtils.isEmpty(body)){
                LOGGER.warn("body is empty,don't send msg");
                return false;
            }
            MqttMessage mqttMessage = new MqttMessage(Base64.getDecoder().decode(body));
            Object mqttReceivedQos = headers.getOrDefault("mqtt_receivedQos", "0");
            mqttMessage.setQos(Integer.valueOf(mqttReceivedQos.toString()));
            Object mqttReceivedTopic = headers.get("mqtt_receivedTopic");
            if (mqttReceivedTopic ==null ||  StringUtils.isEmpty(mqttReceivedTopic.toString())){
                LOGGER.error("mqttReceivedTopic is empty,pls check record data");
                return false;
            }
            client.publish(mqttReceivedTopic.toString(), mqttMessage);
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return true;
    }

    private String getClientId(){
        return String.format("arex-mqtt-replay-sender-%s",System.currentTimeMillis());
    }



    @Override
    public ReplaySendResult send(SenderParameters senderParameters) {
        return null;
    }

    @Override
    public boolean prepareRemoteDependency(ReplayActionCaseItem caseItem) {
        return false;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        ReplaySenderFactory.put("MqttMessageConsumer",this);
    }
}
