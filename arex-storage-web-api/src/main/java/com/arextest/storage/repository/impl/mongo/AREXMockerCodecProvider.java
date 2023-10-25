package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import java.lang.reflect.Field;
import java.util.List;
import lombok.Builder;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.TypeWithTypeParameters;

/**
 * @author jmo
 * @since 2022/1/18
 */
@Builder
@SuppressWarnings("unchecked")
final class AREXMockerCodecProvider implements CodecProvider {

  private final static String TARGET_REQUEST_NAME = "targetRequest";
  private final static String TARGET_RESPONSE_NAME = "targetResponse";
  private static final String CATEGORY_TYPE = "categoryType";
  private final Codec<?> millisecondsDateTimeCodec = new MillisecondsDateTimeCodecImpl();
  private volatile Codec<AREXMocker> arexMockerCodec;
  private Codec<?> targetCodec;

  @Override
  public <T> Codec<T> get(Class<T> aClass, CodecRegistry codecRegistry) {
    return aClass == AREXMocker.class ? (Codec<T>) createCodec(codecRegistry) : null;
  }

  private Codec<AREXMocker> createCodec(CodecRegistry registry) {
    if (arexMockerCodec != null) {
      return this.arexMockerCodec;
    }
    synchronized (this) {
      if (arexMockerCodec != null) {
        return this.arexMockerCodec;
      }
      ClassModel<AREXMocker> classModel;
      try {
        classModel = buildClassModel(registry);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      this.arexMockerCodec = new AREXMockerCodecImpl(classModel);
    }
    return this.arexMockerCodec;
  }

  private ClassModel<AREXMocker> buildClassModel(CodecRegistry registry) throws Exception {
    ClassModelBuilder<AREXMocker> classModelBuilder = ClassModel.builder(AREXMocker.class);
    classModelBuilder.removeProperty(CATEGORY_TYPE);
    List<PropertyModelBuilder<?>> propertyModelBuilders = classModelBuilder.getPropertyModelBuilders();
    Class<PropertyModelBuilder> propertyModelBuilderClass = PropertyModelBuilder.class;
    Field propertyField = propertyModelBuilderClass.getDeclaredField("typeData");
    propertyField.setAccessible(true);
    TypeWithTypeParameters withTypeParameters;
    for (PropertyModelBuilder<?> propertyModelBuilder : propertyModelBuilders) {
      Codec codec = customCodecLookup(propertyModelBuilder);
      if (codec == null) {
        withTypeParameters = (TypeWithTypeParameters) propertyField.get(propertyModelBuilder);
        codec = registry.get(withTypeParameters.getType());
      }
      propertyModelBuilder.codec(codec);
    }
    return classModelBuilder.build();
  }

  private Codec customCodecLookup(PropertyModelBuilder<?> propertyModelBuilder) {
    if (TARGET_REQUEST_NAME.equals(propertyModelBuilder.getName())) {
      return targetCodec;
    }
    if (TARGET_RESPONSE_NAME.equals(propertyModelBuilder.getName())) {
      return targetCodec;
    }
    if (AREXMockerMongoRepositoryProvider.CREATE_TIME_COLUMN_NAME.equals(
        propertyModelBuilder.getName())
        || AREXMockerMongoRepositoryProvider.UPDATE_TIME_COLUMN_NAME.equals(
        propertyModelBuilder.getName())
        || AREXMockerMongoRepositoryProvider.EXPIRATION_TIME_COLUMN_NAME.equals(
        propertyModelBuilder.getName())) {
      return millisecondsDateTimeCodec;
    }
    return null;
  }

}