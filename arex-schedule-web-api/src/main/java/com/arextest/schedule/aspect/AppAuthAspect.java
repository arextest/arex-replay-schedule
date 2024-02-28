package com.arextest.schedule.aspect;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.context.ArexContext;
import com.arextest.common.model.response.ResponseCode;
import com.arextest.common.utils.JwtUtil;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.config.model.dto.SystemConfiguration;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import com.arextest.schedule.dao.mongodb.ApplicationRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import java.util.Optional;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author wildeslam.
 * @create 2023/10/8 17:12
 */
@Slf4j
@Aspect
@Component
public class AppAuthAspect {

  private static final String NO_PERMISSION = "No permission";
  private static final String NO_APPID = "No appId";
  private static final String ERROR_APPID = "Error appId";
  private static Boolean authSwitch;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Resource
  private MongoTemplate mongoTemplate;

  @Pointcut("@annotation(com.arextest.common.annotation.AppAuth)")
  public void appAuth() {
  }

  @Around("appAuth() && @annotation(auth)")
  public Object doAround(ProceedingJoinPoint point, AppAuth auth) throws Throwable {
    if (authSwitch == null) {
      init();
    }
    if (!authSwitch) {
      return point.proceed();
    }
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
    Object result;
    if (CollectionUtils.isEmpty(application.getOwners()) || application.getOwners()
        .contains(userName)) {
      context.setPassAuth(true);
      result = point.proceed();
    } else {
      context.setPassAuth(false);
      result = reject(point, auth, NO_PERMISSION);
    }
    ArexContext.removeContext();
    return result;
  }

  private Object reject(ProceedingJoinPoint point, AppAuth auth, String remark) throws Throwable {
    switch (auth.rejectStrategy()) {
      case FAIL_RESPONSE:
        return ResponseUtils.errorResponse(remark, ResponseCode.AUTHENTICATION_FAILED);
      case DOWNGRADE:
        ArexContext.getContext().setPassAuth(false);
      default:
        return point.proceed();
    }
  }

  private void init() {
    Query query = new Query(Criteria.where(SystemConfigurationCollection.Fields.key).is(KeySummary.DESERIALIZATION_JAR));
    SystemConfigurationCollection collection = mongoTemplate.findOne(query, SystemConfigurationCollection.class);
    authSwitch = Optional.ofNullable(collection)
        .map(SystemConfigurationCollection::getAuthSwitch)
        .orElse(null);
    if (authSwitch == null) {
      throw new RuntimeException("get authSwitch failed, please update storage version");
    }
  }

}
