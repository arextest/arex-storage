package com.arextest.storage.repository.impl.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.PropertyModelBuilder;


@SuppressWarnings({"unchecked", "rawtypes"})
final class MillisecondsDateTimeConventionImpl implements Convention {

    private final Codec<?> millisecondsDateTimeCodec = new MillisecondsDateTimeCodec();


    @Override
    public void apply(ClassModelBuilder<?> classModelBuilder) {
        PropertyModelBuilder propertyModelBuilder =
                classModelBuilder.getProperty(AbstractMongoRepositoryProvider.CREATE_TIME_COLUMN_NAME);
        if (propertyModelBuilder == null) {
            return;
        }
        propertyModelBuilder.codec(millisecondsDateTimeCodec);
    }

}