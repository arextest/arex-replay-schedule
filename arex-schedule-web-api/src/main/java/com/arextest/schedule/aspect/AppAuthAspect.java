package com.arextest.schedule.aspect;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.context.ArexContext;
import com.arextest.common.model.response.ResponseCode;
import com.arextest.common.model.response.ResponseStatusType;
import com.arextest.common.utils.JwtUtil;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.schedule.dao.mongodb.ApplicationRepository;
import com.arextest.schedule.model.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wildeslam.
 * @create 2023/10/8 17:12
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(value = "arex.app.auth.switch", havingValue = "true")
public class AppAuthAspect {
    private static final String NO_PERMISSION = "No permission";
    private static final String NO_APPID = "No appId";
    private static final String ERROR_APPID = "Error appId";

    @Autowired
    private ApplicationRepository applicationRepository;

    @Pointcut("@annotation(com.arextest.common.annotation.AppAuth)")
    public void appAuth(){}

    @Around("appAuth() && @annotation(auth)")
    public Object doAround(ProceedingJoinPoint point, AppAuth auth) throws Throwable {
        ArexContext context = ArexContext.getContext();
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String appId = request.getHeader("appId");
        String accessToken = request.getHeader("access-token");
        String userName = JwtUtil.getUserName(accessToken);
        context.setAppId(appId);
        context.setOperator(userName);
        if (appId == null) {
            LOGGER.error("header has no appId");
            return reject(point, auth, NO_APPID);
        }
        AppCollection application = applicationRepository.query(context.getAppId());
        if (application == null) {
            LOGGER.error("error appId");
            return reject(point, auth, ERROR_APPID);
        }
        if (application.getOwners() == null) {
            LOGGER.error("The app:{} has no owners", appId);
            return reject(point, auth, NO_PERMISSION);
        }
        Object result;
        if (application.getOwners().contains(userName)) {
            context.setPassAuth(true);
            result = point.proceed();
        } else {
            context.setPassAuth(false);
            return reject(point, auth, NO_PERMISSION);
        }
        ArexContext.removeContext();
        return result;
    }

    private Object reject(ProceedingJoinPoint point, AppAuth auth, String remark) throws Throwable {
        ArexContext.removeContext();
        switch (auth.rejectStrategy()) {
            case FAIL_RESPONSE:
                CommonResponse commonResponse = new CommonResponse();
                commonResponse.setDesc(remark);
                commonResponse.setResult(2);
                ResponseStatusType responseStatusType = new ResponseStatusType();
                responseStatusType.setTimestamp(System.currentTimeMillis());
                responseStatusType.setResponseDesc(remark);
                responseStatusType.setResponseCode(ResponseCode.AUTHENTICATION_FAILED.getCodeValue());
                commonResponse.setResponseStatusType(responseStatusType);
                return commonResponse;
            case DOWNGRADE:
                ArexContext.getContext().setPassAuth(false);
            default:
                return point.proceed();
        }
    }

}
