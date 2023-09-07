//import java.util.List;
//
//import javax.annotation.Resource;
//
//import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import com.arextest.config.model.dto.application.ApplicationConfiguration;
//import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
//import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
//import com.arextest.storage.web.boot.WebSpringBootServletInitializer;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = {WebSpringBootServletInitializer.class})
//public class RespositoryTest {
//
//    @Resource
//    ApplicationConfigurationRepositoryImpl applicationConfigurationRepositoryImpl;
//
//    @Resource
//    ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepositoryImpl;
//
//    @Test
//    public void test() {
//        List<ApplicationConfiguration> list = applicationConfigurationRepositoryImpl.list();
//        System.out.println();
//    }
//
//    @Test
//    public void testRemove() {
//        ApplicationOperationConfiguration applicationOperationConfiguration = new ApplicationOperationConfiguration();
//        applicationOperationConfiguration.setId("64f86b48b3df2cce0c75ff6b");
//        applicationOperationConfiguration.setStatus(3);
//        boolean update = applicationOperationConfigurationRepositoryImpl.update(applicationOperationConfiguration);
//        System.out.println();
//    }
//
//}
