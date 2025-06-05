package net.runelite.client.plugins.microbot.geflipper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class WikiMappingResponse {
    @JsonProperty("examine")
    private String examine;

    @JsonProperty("id")
    private int id;

    @JsonProperty("members")
    private boolean members;

    @JsonProperty("lowalch")
    private Integer lowalch;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("value")
    private Integer value;

    @JsonProperty("highalch")
    private Integer highalch;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("name")
    private String name;
}
