package com.arextest.storage.model.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import org.bson.types.ObjectId;

import java.io.IOException;

/**
 * Created by rchen9 on 2022/10/19.
 */
public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
    @Override
    public ObjectId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        TextNode jsonNodes = jsonParser.readValueAs(TextNode.class);
        if (jsonNodes == null) {
            return null;
        }
        return new ObjectId(jsonNodes.asText());
    }
}