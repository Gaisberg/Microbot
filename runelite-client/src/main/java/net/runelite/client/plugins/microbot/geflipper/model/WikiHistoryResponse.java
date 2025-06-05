package net.runelite.client.plugins.microbot.geflipper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiHistoryResponse {
    @JsonProperty("data")
    private Map<String, PriceEntry> data;

    @JsonProperty(value = "timestamp")
    private long timestamp;

    @Getter
    @Data
    public static class PriceEntry {
        @JsonProperty("avgHighPrice")
        private Integer avgHighPrice;

        @JsonProperty("highPriceVolume")
        private Integer highPriceVolume;

        @JsonProperty("avgLowPrice")
        private Integer avgLowPrice;

        @JsonProperty("lowPriceVolume")
        private Integer lowPriceVolume;

        public int getAveragePrice() {
            return (avgHighPrice + avgLowPrice) / 2;
        }
    }


}
