package com.arextest.storage.web.controller.config;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.config.model.vo.AddApplicationRequest;
import com.arextest.config.model.vo.AddApplicationResponse;
import com.arextest.config.model.vo.UpdateApplicationRequest;
import com.arextest.storage.service.config.ApplicationService;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:20
 */
@Slf4j
@Controller
@RequestMapping(path = "/api/config/app", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApplicationController {

  @Autowired
  private ApplicationService applicationService;

  @PostMapping("/add")
  @ResponseBody
  public Response load(@RequestBody @Valid AddApplicationRequest request) {
    AddApplicationResponse response = applicationService.addApplication(request);
    return ResponseUtils.successResponse(response);
  }

  @PostMapping("/modify")
  @ResponseBody
  @AppAuth
  public Response modify(@RequestBody @Valid UpdateApplicationRequest request) {
    return ResponseUtils.successResponse(applicationService.modifyApplication(request));
  }
}
