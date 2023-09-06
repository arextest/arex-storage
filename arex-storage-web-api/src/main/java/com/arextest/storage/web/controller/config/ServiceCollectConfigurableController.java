//package com.arextest.storage.web.controller.config;
//
//import javax.annotation.Resource;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
//import com.arextest.storage.service.config.ConfigurableHandler;
//
///**
// * @author jmo
// * @since 2022/1/22
// */
//@Controller
//@RequestMapping(path = "/api/config/serviceCollect", produces = MediaType.APPLICATION_JSON_VALUE)
//public final class ServiceCollectConfigurableController
//    extends AbstractConfigurableController<ServiceCollectConfiguration> {
//    public ServiceCollectConfigurableController(
//        @Autowired ConfigurableHandler<ServiceCollectConfiguration> configurableHandler) {
//        super(configurableHandler);
//    }
//
//    @Resource
//    private ConfigurableHandler<ServiceCollectConfiguration> serviceCollectHandler;
//
//    public void updateServiceCollectTime(String appId) {
//        ServiceCollectConfiguration serviceCollectConfiguration = serviceCollectHandler.useResult(appId);
//        serviceCollectHandler.update(serviceCollectConfiguration);
//    }
//}
