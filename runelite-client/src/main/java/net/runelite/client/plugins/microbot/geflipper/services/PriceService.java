package net.runelite.client.plugins.microbot.geflipper.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.model.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
public class PriceService {

    private static final String API_BASE_URL = "https://prices.runescape.wiki/api/v1/osrs/";
    private static final String MAPPING_ENDPOINT = "mapping";
    private static final String AGENT = "Runelite GE Plugin";

    // Endpoint configurations with cache durations in seconds
    private static final Map<String, EndpointConfig> ENDPOINTS = Map.of(
            "latest", new EndpointConfig("latest", EndpointType.LATEST, 10),           // 10 seconds
            "5m", new EndpointConfig("5m", EndpointType.HISTORY, 5 * 60),             // 5 minutes
            "1h", new EndpointConfig("1h", EndpointType.HISTORY, 60 * 60),            // 1 hour
            "6h", new EndpointConfig("6h", EndpointType.HISTORY, 6 * 60 * 60),        // 6 hours
            "24h", new EndpointConfig("24h", EndpointType.HISTORY, 24 * 60 * 60)      // 24 hours
    );

    @Inject
    private OkHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Integer, FlipOpportunity> itemPrices = new ConcurrentHashMap<>();

    private final Map<String, CachedEndpointData> endpointCache = new ConcurrentHashMap<>();

    @Getter
    private boolean initialized = false;

    private static class EndpointConfig {
        final String endpoint;
        final EndpointType type;
        final int cacheDurationSeconds;

        EndpointConfig(String endpoint, EndpointType type, int cacheDurationSeconds) {
            this.endpoint = endpoint;
            this.type = type;
            this.cacheDurationSeconds = cacheDurationSeconds;
        }
    }

    // Cached data container
    private static class CachedEndpointData {
        final long timestamp;
        final long fetchTime;
        final Object data;

        CachedEndpointData(long timestamp, long fetchTime, Object data) {
            this.timestamp = timestamp;
            this.fetchTime = fetchTime;
            this.data = data;
        }
    }

    private enum EndpointType {
        LATEST, HISTORY
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            log.info("Initializing PriceService - fetching item mappings...");
            updateItemMappings();
            log.info("Loaded {} item mappings", itemPrices.size());
            initialized = true;
            log.info("PriceService initialization complete");
        } catch (Exception e) {
            log.error("Failed to initialize PriceService", e);
        }
    }

    public Map<Integer, FlipOpportunity> getItemPrices() {
        ensureLatestPrices();
        ensureAllHistoricalPrices();
        return itemPrices;
    }

    /**
     * Get latest price data, fetching if cache is stale
     */
    public void ensureLatestPrices() {
        ensureEndpointData("latest");
    }

    /**
     * Get all available historical data, fetching stale data as needed
     */
    public void ensureAllHistoricalPrices() {
        ENDPOINTS.keySet().stream()
                .filter(endpoint -> !endpoint.equals("latest"))
                .forEach(this::ensureEndpointData);
    }

    /**
     * Check if endpoint data is cached and fresh
     */
    public boolean isEndpointDataFresh(String endpoint) {
        if (!initialized) {
            initialize();
        }

        EndpointConfig config = ENDPOINTS.get(endpoint);
        if (config == null) {
            return false;
        }

        CachedEndpointData cached = endpointCache.get(endpoint);
        if (cached == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        // For history endpoints, calculate expiration based on API timestamp + duration
        // For latest endpoints, use fetch time + duration
        long expirationTime;
        if (config.type == EndpointType.HISTORY) {
            // Use API timestamp (in milliseconds) + cache duration
            expirationTime = cached.timestamp + (config.cacheDurationSeconds * 1000L);
        } else {
            // Use fetch time + cache duration for latest endpoint
            expirationTime = cached.fetchTime + (config.cacheDurationSeconds * 1000L);
        }

        return currentTime < expirationTime;
    }

    private void ensureEndpointData(String endpoint) {
        if (!initialized) {
            initialize();
        }

        if (!isEndpointDataFresh(endpoint)) {
            try {
                fetchEndpointData(endpoint);
                log.debug("Fetched fresh data for endpoint: {}", endpoint);
            } catch (Exception e) {
                log.error("Failed to fetch data for endpoint: {}", endpoint, e);
            }
        }
    }

    private void fetchEndpointData(String endpoint) throws IOException {
        EndpointConfig config = ENDPOINTS.get(endpoint);
        if (config == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }

        switch (config.type) {
            case LATEST:
                fetchLatestPrices(endpoint);
                break;
            case HISTORY:
                fetchHistoryPrices(endpoint);
                break;
        }
    }

    private void fetchLatestPrices(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(API_BASE_URL + endpoint)
                .addHeader("User-Agent", AGENT)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(endpoint + " API request failed: " + response.code());
            }

            String responseBody = response.body().string();
            WikiLatestResponse latestResponse = objectMapper.readValue(responseBody, WikiLatestResponse.class);

            long currentTime = System.currentTimeMillis();

            // Cache the response (for latest, timestamp and fetchTime are the same)
            endpointCache.put(endpoint, new CachedEndpointData(currentTime, currentTime, latestResponse));

            // Update item data
            for (Map.Entry<String, WikiLatestResponse.PriceEntry> entry : latestResponse.getData().entrySet()) {
                try {
                    int itemId = Integer.parseInt(entry.getKey());
                    WikiLatestResponse.PriceEntry priceEntry = entry.getValue();

                    FlipOpportunity flipOpportunity = itemPrices.get(itemId);
                    if (flipOpportunity != null) {
                        flipOpportunity.setLatest(priceEntry);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid item ID in latest data: {}", entry.getKey());
                }
            }

            log.debug("Updated latest data for {} items", latestResponse.getData().size());
        }
    }

    private void fetchHistoryPrices(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(API_BASE_URL + endpoint)
                .addHeader("User-Agent", AGENT)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(endpoint + " API request failed: " + response.code());
            }

            String responseBody = response.body().string();
            WikiHistoryResponse historyResponse = objectMapper.readValue(responseBody, WikiHistoryResponse.class);

            // Extract timestamp from response and convert to milliseconds
            Long responseTimestamp = historyResponse.getTimestamp();
            long timestampMillis = responseTimestamp != null ? responseTimestamp * 1000L : System.currentTimeMillis();

            // Cache the response with API timestamp
            endpointCache.put(endpoint, new CachedEndpointData(timestampMillis, System.currentTimeMillis(), historyResponse));

            // Update item data
            for (Map.Entry<String, WikiHistoryResponse.PriceEntry> entry : historyResponse.getData().entrySet()) {
                try {
                    int itemId = Integer.parseInt(entry.getKey());
                    WikiHistoryResponse.PriceEntry priceEntry = entry.getValue();

                    if (priceEntry.getAvgHighPrice() != null && priceEntry.getAvgLowPrice() != null) {
                        FlipOpportunity flipOpportunity = itemPrices.get(itemId);
                        if (flipOpportunity != null) {
                            flipOpportunity.setHistoricalData(TimeFrame.fromEndpoint(endpoint), priceEntry);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid item ID in {} data: {}", endpoint, entry.getKey());
                }
            }

            log.debug("Updated {} data for {} items at timestamp {} (expires at {})",
                    endpoint, historyResponse.getData().size(),
                    Instant.ofEpochMilli(timestampMillis),
                    Instant.ofEpochMilli(timestampMillis + (ENDPOINTS.get(endpoint).cacheDurationSeconds * 1000L)));
        }
    }

    private void updateItemMappings() throws IOException {
        Request request = new Request.Builder()
                .url(API_BASE_URL + MAPPING_ENDPOINT)
                .addHeader("User-Agent", AGENT)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Mapping API request failed: " + response.code());
            }

            String responseBody = response.body().string();
            List<WikiMappingResponse> mappings = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            for (WikiMappingResponse mapping : mappings) {
                itemPrices.put(mapping.getId(), new FlipOpportunity(mapping));
            }
        }
    }
}
