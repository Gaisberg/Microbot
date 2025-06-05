package net.runelite.client.plugins.microbot.geflipper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiVolumeResponse {
    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("data")
    @JsonDeserialize(using = VolumeDataDeserializer.class)
    private List<VolumeEntry> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolumeEntry {
        private int itemId;
        private int volume;
    }

    /**
     * Custom deserializer to convert Map<String, Integer> to List<VolumeEntry>
     */
    public static class VolumeDataDeserializer extends JsonDeserializer<List<VolumeEntry>> {
        @Override
        public List<VolumeEntry> deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            List<VolumeEntry> entries = new ArrayList<>();

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                try {
                    int itemId = Integer.parseInt(field.getKey());
                    int volume = field.getValue().asInt();
                    entries.add(new VolumeEntry(itemId, volume));
                } catch (NumberFormatException e) {
                    // Skip invalid item IDs
                }
            }

            return entries;
        }
    }
}