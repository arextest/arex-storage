package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.IdGenerators;
import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertyModel;

/**
 * @author jmo
 * @since 2022/1/18
 */
@Slf4j
final class AREXMockerCodecImpl implements Codec<AREXMocker> {

  private final ClassModel<AREXMocker> classModel;

  AREXMockerCodecImpl(ClassModel<AREXMocker> classModel) {
    this.classModel = classModel;
  }

  @Override
  public AREXMocker decode(BsonReader reader, DecoderContext decoderContext) {
    AREXMocker instanceCreator = new AREXMocker();
    this.decodeProperties(reader, decoderContext, instanceCreator);
    return instanceCreator;
  }

  @Override
  public void encode(BsonWriter writer, AREXMocker value, EncoderContext encoderContext) {
    writer.writeStartDocument();
    @SuppressWarnings("unchecked")
    PropertyModel<String> idPropertyModel = (PropertyModel<String>) this.classModel.getIdPropertyModel();
    this.encodeIdProperty(writer, value, encoderContext, idPropertyModel);
    if (this.classModel.useDiscriminator()) {
      writer.writeString(this.classModel.getDiscriminatorKey(), this.classModel.getDiscriminator());
    }
    List<PropertyModel<?>> propertyModels = this.classModel.getPropertyModels();
    boolean entryPoint = value.getCategoryType().isEntryPoint();
    for (PropertyModel<?> propertyModel : propertyModels) {
      if (entryPoint && isRecordIdPropertyModel(propertyModel)) {
        continue;
      }
      if (!propertyModel.equals(idPropertyModel)) {
        this.encodeProperty(writer, value, encoderContext, propertyModel);
      }
    }
    writer.writeEndDocument();
  }

  private boolean isRecordIdPropertyModel(PropertyModel<?> propertyModel) {
    return StringUtils.equals(AREXMockerMongoRepositoryProvider.RECORD_ID_COLUMN_NAME,
        propertyModel.getName());
  }

  @Override
  public Class<AREXMocker> getEncoderClass() {
    return AREXMocker.class;
  }

  private void encodeIdProperty(BsonWriter writer, AREXMocker instance,
      EncoderContext encoderContext, PropertyModel<String> idPropertyModel) {
    String idValue = getIdPropertyValue(instance);
    this.encodeValue(writer, encoderContext, idPropertyModel, idValue);
  }

  private String getIdPropertyValue(AREXMocker instance) {
    String idValue = instance.getId();
    if (idValue == null) {
      if (instance.getCategoryType().isEntryPoint()) {
        return instance.getRecordId();
      }
      return IdGenerators.STRING_ID_GENERATOR.generate();
    }
    return idValue;
  }

  private <S> void encodeProperty(BsonWriter writer, AREXMocker instance,
      EncoderContext encoderContext, PropertyModel<S> propertyModel) {
    if (propertyModel != null && propertyModel.isReadable()) {
      S propertyValue = propertyModel.getPropertyAccessor().get(instance);
      this.encodeValue(writer, encoderContext, propertyModel, propertyValue);
    }
  }

  private <S> void encodeValue(BsonWriter writer, EncoderContext encoderContext,
      PropertyModel<S> propertyModel, S propertyValue) {
    if (propertyModel.shouldSerialize(propertyValue)) {
      writer.writeName(propertyModel.getReadName());
      if (propertyValue == null) {
        writer.writeNull();
      } else {
        try {
          encoderContext.encodeWithChildContext(propertyModel.getCodec(), writer, propertyValue);
        } catch (CodecConfigurationException var6) {
          throw new CodecConfigurationException(
              String.format("Failed to encode '%s'. Encoding '%s' errored with: %s",
                  this.classModel.getName(), propertyModel.getReadName(), var6.getMessage()), var6);
        }
      }
    }

  }

  private void decodeProperties(BsonReader reader, DecoderContext decoderContext,
      AREXMocker instanceCreator) {
    reader.readStartDocument();
    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      String name = reader.readName();
      if (this.classModel.useDiscriminator() && this.classModel.getDiscriminatorKey()
          .equals(name)) {
        reader.readString();
      } else {
        this.decodePropertyModel(reader, decoderContext, instanceCreator, name);
      }
    }
    reader.readEndDocument();
  }

  private void decodePropertyModel(BsonReader reader, DecoderContext decoderContext,
      AREXMocker instance, String name) {
    PropertyModel<?> propertyModel = this.getPropertyModelByWriteName(name);
    if (propertyModel == null) {
      reader.skipValue();
      return;
    }

    try {
      Object value = null;
      if (reader.getCurrentBsonType() == BsonType.NULL) {
        reader.readNull();
      } else {
        Codec<?> codec = propertyModel.getCodec();
        if (codec == null) {
          throw new CodecConfigurationException(
              String.format("Missing codec in '%s' for '%s'", this.classModel.getName(),
                  propertyModel.getName()));
        }
        value = decoderContext.decodeWithChildContext(codec, reader);
      }
      if (propertyModel.isWritable()) {
        @SuppressWarnings("unchecked")
        PropertyAccessor<Object> propertyAccessor = (PropertyAccessor<Object>) propertyModel.getPropertyAccessor();
        propertyAccessor.set(instance, value);
      }
    } catch (CodecConfigurationException | BsonInvalidOperationException var8) {
      throw new CodecConfigurationException(
          String.format("Failed to decode '%s'. Decoding '%s' errored with: %s",
              this.classModel.getName(), name, var8.getMessage()), var8);
    }

  }

  private PropertyModel<?> getPropertyModelByWriteName(String readName) {
    List<PropertyModel<?>> propertyModels = this.classModel.getPropertyModels();
    for (PropertyModel<?> propertyModel : propertyModels) {
      if (propertyModel.getWriteName().equals(readName)) {
        return propertyModel;
      }
    }
    return null;
  }
}