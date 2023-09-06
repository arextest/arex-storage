//package com.arextest.storage.web.controller.config;
//
//import javax.annotation.Resource;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//import com.arextest.common.model.response.Response;
//import com.arextest.config.model.dto.ModifyType;
//import com.arextest.config.model.dto.application.InstancesConfiguration;
//import com.arextest.storage.service.config.impl.ApplicationInstancesConfigurableHandler;
//
///**
// * Created by rchen9 on 2022/9/30.
// */
//@Controller
//@RequestMapping(path = "/api/config/applicationInstances", produces = MediaType.APPLICATION_JSON_VALUE)
//public class ApplicationInstancesConfigurableController extends AbstractConfigurableController<InstancesConfiguration> {
//
//    public ApplicationInstancesConfigurableController(
//        @Autowired ApplicationInstancesConfigurableHandler configurableHandler) {
//        super(configurableHandler);
//    }
//
//    @Resource
//    private ServiceCollectConfigurableController serviceCollectConfigurableController;
//
//    @Override
//    @ResponseBody
//    public Response modify(@PathVariable ModifyType modifyType, @RequestBody InstancesConfiguration configuration)
//        throws Exception {
//        serviceCollectConfigurableController.updateServiceCollectTime(configuration.getAppId());
//
//        return super.modify(modifyType, configuration);
//    }
//}