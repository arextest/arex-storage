package com.arextest.storage.core.bean;

import com.arextest.storage.model.serialization.ObjectIdDeserializer;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Created by rchen9 on 2022/10/28.
 */
@Configuration
public class Jackson2ObjectMapperBuilderConfiguration {
    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .deserializerByType(ObjectId.class, new ObjectIdDeserializer());
    }

}
