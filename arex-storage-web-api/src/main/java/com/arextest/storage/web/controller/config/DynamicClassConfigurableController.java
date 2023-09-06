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
//import com.arextest.config.model.dto.record.DynamicClassConfiguration;
//import com.arextest.storage.service.config.ConfigurableHandler;
//
///**
// * @author jmo
// * @since 2022/1/22
// */
//@Controller
//@RequestMapping(path = "/api/config/dynamicClass", produces = MediaType.APPLICATION_JSON_VALUE)
//public final class DynamicClassConfigurableController
//    extends AbstractConfigurableController<DynamicClassConfiguration> {
//    public DynamicClassConfigurableController(
//        @Autowired ConfigurableHandler<DynamicClassConfiguration> configurableHandler) {
//        super(configurableHandler);
//    }
//
//    @Resource
//    private ServiceCollectConfigurableController serviceCollectConfigurableController;
//
//    @Override
//    @ResponseBody
//    public Response modify(@PathVariable ModifyType modifyType, @RequestBody DynamicClassConfiguration configuration)
//        throws Exception {
//        // change dataChangeUpdatesTime in recordServiceConfig before modifying
//        serviceCollectConfigurableController.updateServiceCollectTime(configuration.getAppId());
//
//        return super.modify(modifyType, configuration);
//    }
//}
