package com.arextest.storage.web.controller;

import com.arextest.model.replay.result.PostProcessResultRequestType;
import com.arextest.model.response.Response;
import com.arextest.storage.service.ResultProcessService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping(path = "/api/storage/replay/", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ScheduleResultProcessController {

  @Resource
  ResultProcessService resultProcessService;

  @PostMapping("/postprocessResult")
  @ResponseBody
  public Response postprocessResult(@RequestBody PostProcessResultRequestType request) {
    resultProcessService.handleResult(request);
    return ResponseUtils.successResponse(true);
  }
}
