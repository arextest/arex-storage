package com.arextest.storage.repository.impl.mongo;

import com.arextest.storage.model.mocker.MainEntry;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.IdGenerators;

/**
 * @author jmo
 * @since 2022/1/18
 */
final class IdPropertyNameConventionImpl implements Convention {

    @Override
    public void apply(ClassModelBuilder<?> classModelBuilder) {
        this.useRecordIdAsIdPropertyName(classModelBuilder);
        this.useStringIdGenerator(classModelBuilder);
    }

    private void useRecordIdAsIdPropertyName(ClassModelBuilder<?> classModelBuilder) {
        Class<?> targetClass = classModelBuilder.getType();
        if (MainEntry.class.isAssignableFrom(targetClass)) {
            classModelBuilder.idPropertyName(AbstractMongoRepositoryProvider.RECORD_ID_COLUMN_NAME);
        }
    }

    private void useStringIdGenerator(ClassModelBuilder<?> classModelBuilder) {
        classModelBuilder.idGenerator(IdGenerators.STRING_ID_GENERATOR);
    }
}