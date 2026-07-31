package com.vointika.metafield.infrastructure.port;

import com.vointika.metafield.application.port.JsonSyntaxPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class JacksonJsonSyntaxPort implements JsonSyntaxPort {

    private final ObjectMapper objectMapper;

    public JacksonJsonSyntaxPort(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isWellFormed(String value) {
        try {
            // readValue, not readTree: readTree stops at the first complete document
            // and ignores whatever follows, so trailing garbage would be stored.
            objectMapper.readValue(value, JsonNode.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
