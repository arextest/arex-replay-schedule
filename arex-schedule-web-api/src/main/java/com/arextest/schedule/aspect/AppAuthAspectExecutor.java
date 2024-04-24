package com.arextest.schedule.aspect;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.context.ArexContext;
import com.arextest.common.exceptions.ArexException;
import com.arextest.common.jwt.JWTService;
import com.arextest.common.model.response.ResponseCode;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.schedule.dao.mongodb.ApplicationRepository;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RequiredArgsConstructor
public class AppAuthAspectExecutor {

  public static final String POINT_CONTENT = "@annotation(com.arextest.common.annotation.AppAuth)";

  public static final String AROUND_CONTENT = "appAuth() && @annotation(auth)";

  private static final String NO_PERMISSION = "No permission";
  private static final String NO_APPID = "No appId";
  private static final String ERROR_APPID = "Error appId";
  private static Boolean authSwitch = null;

  private final ApplicationRepository applicationRepository;

  private final MongoTemplate mongoTemplate;

  private final JWTService jwtService;


  public Object doAround(ProceedingJoinPoint point, AppAuth auth) throws Throwable {

    try {
      if (!judgeByAuth()) {
        return point.proceed();
      }

      ArexContext context = ArexContext.getContext();
      setContext();

      if (context.getAppId() == null) {
        LOGGER.error("header has no appId");
        return reject(point, auth, NO_APPID);
      }

      OwnerExistResult ownerExistResult = getOwnerExistResult();
      if (ownerExistResult.getExist()) {
        context.setPassAuth(true);
        return point.proceed();
      } else {
        context.setPassAuth(false);
        return reject(point, auth, ownerExistResult.getRemark());
      }

    } finally {
      ArexContext.removeContext();
    }

  }

  protected boolean judgeByAuth() {
    if (authSwitch == null) {
      init();
    }
    return authSwitch;
  }

  protected boolean setContext() {
    ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = requestAttributes.getRequest();
    String appId = request.getHeader("appId");
    String accessToken = request.getHeader("access-token");
    String userName = jwtService.getUserName(accessToken);
    ArexContext context = ArexContext.getContext();
    context.setAppId(appId);
    context.setOperator(userName);
    return true;
  }

  protected OwnerExistResult getOwnerExistResult() {
    ArexContext context = ArexContext.getContext();
    String userName = context.getOperator();

    AppCollection application = applicationRepository.query(context.getAppId());
    if (application == null) {
      LOGGER.error("error appId, appId: {}", context.getAppId());
      return new OwnerExistResult(false, ERROR_APPID);
    }
    Set<String> owners = application.getOwners();
    if (CollectionUtils.isEmpty(owners) || owners.contains(userName)) {
      return new OwnerExistResult(true, null);
    } else {
      return new OwnerExistResult(false, NO_PERMISSION);
    }
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
    Query query = new Query(
        Criteria.where(SystemConfigurationCollection.Fields.key).is(KeySummary.AUTH_SWITCH));
    SystemConfigurationCollection collection = mongoTemplate.findOne(query,
        SystemConfigurationCollection.class, SystemConfigurationCollection.DOCUMENT_NAME);
    authSwitch = Optional.ofNullable(collection)
        .map(SystemConfigurationCollection::getAuthSwitch)
        .orElse(null);
    if (authSwitch == null) {
      throw new ArexException(ResponseCode.AUTHENTICATION_FAILED.getCodeValue(),
          "get authSwitch failed, please update storage version");
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OwnerExistResult {

    private Boolean exist;
    private String remark;
  }

}
