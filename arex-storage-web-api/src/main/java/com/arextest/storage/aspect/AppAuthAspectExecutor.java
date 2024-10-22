package com.arextest.storage.aspect;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.context.ArexContext;
import com.arextest.common.jwt.JWTService;
import com.arextest.common.utils.ResponseUtils_New;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import com.arextest.storage.model.ArexStorageResponseCode;
import com.arextest.storage.service.config.ApplicationService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
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

  private final ApplicationConfigurationRepositoryImpl applicationConfigurationRepository;

  private final ApplicationService applicationService;

  private final SystemConfigurationRepositoryImpl systemConfigurationRepository;

  private final JWTService jwtService;

  public Object doAround(ProceedingJoinPoint point, AppAuth auth) throws Throwable {
    if (!judgeByAuth()) {
      return point.proceed();
    }

    ArexContext context = ArexContext.getContext();

    try {

      // set context
      setContext();

      // do aspect by appId
      if (context.getAppId() == null) {
        LOGGER.error("header has no appId");
        return reject(point, auth, NO_APPID, ArexStorageResponseCode.APP_AUTH_NO_APP_ID);
      }

      // do aspect by owner exist
      OwnerExistResult ownerExistResult = getOwnerExistResult();
      return processOwnerExistVerify(ownerExistResult, context, point, auth);

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

  protected void setContext() {
    ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return;
    }
    HttpServletRequest request = requestAttributes.getRequest();
    String appId = request.getHeader("appId");
    String accessToken = request.getHeader("access-token");
    String userName = jwtService.getUserName(accessToken);
    ArexContext context = ArexContext.getContext();
    context.setAppId(appId);
    context.setOperator(userName);
  }

  protected OwnerExistResult getOwnerExistResult() {
    ArexContext context = ArexContext.getContext();
    String appId = context.getAppId();
    String userName = context.getOperator();

    Set<String> owners = applicationService.getAppOwnersCache(appId);
    if (owners == null) {
      List<ApplicationConfiguration> applications = applicationConfigurationRepository.listBy(
          context.getAppId());
      if (CollectionUtils.isEmpty(applications)) {
        LOGGER.error("error appId, appId: {}", context.getAppId());
        return new OwnerExistResult(false, ERROR_APPID,
            ArexStorageResponseCode.APP_AUTH_ERROR_APP_ID);
      }
      ApplicationConfiguration application = applications.get(0);
      owners = application.getOwners();
    }
    if (CollectionUtils.isEmpty(owners) || owners.contains(userName)) {
      return new OwnerExistResult(true, null, null);
    } else {
      return new OwnerExistResult(false, NO_PERMISSION,
          ArexStorageResponseCode.APP_AUTH_NO_PERMISSION);
    }
  }

  private Object processOwnerExistVerify(OwnerExistResult ownerExistResult, ArexContext context,
      ProceedingJoinPoint point, AppAuth auth)
      throws Throwable {
    if (ownerExistResult.getExist()) {
      context.setPassAuth(true);
      return point.proceed();
    } else {
      context.setPassAuth(false);
      return reject(point, auth, ownerExistResult.getRemark(), ownerExistResult.getResponseCode());
    }
  }


  private Object reject(ProceedingJoinPoint point, AppAuth auth, String remark, int responseCode)
      throws Throwable {
    switch (auth.rejectStrategy()) {
      case FAIL_RESPONSE:
        return ResponseUtils_New.errorResponse(remark, responseCode);
      case DOWNGRADE:
        ArexContext.getContext().setPassAuth(false);
        return point.proceed();
      default:
        return point.proceed();
    }
  }


  private void init() {
    authSwitch = Optional.ofNullable(
            systemConfigurationRepository.getSystemConfigByKey(KeySummary.AUTH_SWITCH))
        .map(SystemConfiguration::getAuthSwitch)
        .orElse(false);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OwnerExistResult {

    private Boolean exist;
    private String remark;
    private Integer responseCode;
  }
}