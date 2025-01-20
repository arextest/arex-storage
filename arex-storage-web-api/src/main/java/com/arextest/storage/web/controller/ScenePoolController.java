package com.arextest.storage.web.controller;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.model.replay.QuerySceneRequestType;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.service.ScenePoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author: QizhengMo
 * @date: 2024/3/13 13:02
 */
@Slf4j
@Controller
@RequestMapping("/api/scene/")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class ScenePoolController {
  private final ScenePoolService scenePoolService;

  @GetMapping(value = "/clearReplayPool/{appId}", produces = "application/json")
  @ResponseBody
  public Response clearReplayPool(@PathVariable("appId") String appId) {
    scenePoolService.clearPoolByApp(appId, ScenePoolFactory.REPLAY_SCENE_POOL);
    return ResponseUtils.successResponse(true);
  }

  @PostMapping(value = "/findByAppId", produces = "application/json")
  @ResponseBody
  public Response findRecordsByAppId(@RequestBody QuerySceneRequestType requestType) {
    return ResponseUtils.successResponse(
        scenePoolService.findRecordsByAppId(requestType.getAppId(), requestType.getLastId(), requestType.getCategoryType(),
            requestType.getPageSize()));
  }
}
