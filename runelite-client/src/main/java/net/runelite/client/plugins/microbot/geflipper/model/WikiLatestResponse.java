package net.runelite.client.plugins.microbot.geflipper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
public class WikiLatestResponse {
    @JsonProperty("data")
    private Map<String, PriceEntry> data;

    private long timestamp = System.currentTimeMillis();

    @Getter
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceEntry {
        @JsonProperty("high")
        private Integer high;

        @JsonProperty("low")
        private Integer low;
    }
}
