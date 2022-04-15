package io.arex.storage.core.repository.impl.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.PropertyModelBuilder;

/**
 * @author jmo
 * @since 2022/1/18
 */
final class MillisecondsDateTimeConventionImpl implements Convention {

    private final Codec<?> millisecondsDateTimeCodec = new MillisecondsDateTimeCodec();

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void apply(ClassModelBuilder<?> classModelBuilder) {
        PropertyModelBuilder propertyModelBuilder =
                classModelBuilder.getProperty(AbstractMongoDbRepository.CREATE_TIME_COLUMN_NAME);
        if (propertyModelBuilder == null) {
            return;
        }
        propertyModelBuilder.codec(millisecondsDateTimeCodec);
    }
}
