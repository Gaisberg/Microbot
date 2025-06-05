package net.runelite.client.plugins.microbot.geflipper.model;

import lombok.Getter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum TimeFrame {
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    SIX_HOURS("6h", Duration.ofHours(6)),
    TWENTY_FOUR_HOURS("24h", Duration.ofHours(24));

    private final String endpoint;
    private final Duration duration;

    TimeFrame(String endpoint, Duration duration) {
        this.endpoint = endpoint;
        this.duration = duration;
    }

    public static TimeFrame fromEndpoint(String endpoint) {
        return Arrays.stream(values())
                .filter(tf -> tf.endpoint.equals(endpoint))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint: " + endpoint));
    }

    public static List<TimeFrame> getOrderedTimeFrames() {
        return Arrays.stream(values())
                .sorted(Comparator.comparing(a -> a.duration))
                .collect(Collectors.toList());
    }

    public static List<TimeFrame> getOrderedTimeFramesOldestFirst() {
        return Arrays.stream(values())
                .sorted((a, b) -> b.duration.compareTo(a.duration))
                .collect(Collectors.toList());
    }

    public long getSeconds() {
        return duration.getSeconds();
    }
}
