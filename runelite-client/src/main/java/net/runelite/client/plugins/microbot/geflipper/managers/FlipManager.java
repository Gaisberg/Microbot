package net.runelite.client.plugins.microbot.geflipper.managers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperConfig;
import net.runelite.client.plugins.microbot.geflipper.model.Flip;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class FlipManager {

    @Inject
    private GeFlipperConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private List<Flip> activeFlips = new ArrayList<>();


    public void loadFlips() {
        try {
            String activeFlipsJson = config.activeFlips();
            if (activeFlipsJson != null && !activeFlipsJson.equals("[]")) {
                activeFlips = objectMapper.readValue(activeFlipsJson, new TypeReference<>() {
                });
                log.info("Loaded {} active flips from config", activeFlips.size());
            }
        } catch (Exception e) {
            log.error("Failed to load flips from config", e);
            activeFlips = new ArrayList<>();
        }
    }

    public void saveFlips() {
        try {
            String activeFlipsJson = objectMapper.writeValueAsString(activeFlips);
            config.setActiveFlips(activeFlipsJson);
        } catch (Exception e) {
            log.error("Failed to save flips to config", e);
        }
    }

    public void addFlip(Flip flip) {
        activeFlips.add(flip);
        flip.setBuyTime(System.currentTimeMillis());
        saveFlips();
        log.info("Added new flip for item {} at buy price {}", flip.getItemName(), flip.getBuyPrice());
    }

    public void removeFlip(Flip flip) {
        activeFlips.remove(flip);
        log.info("Removed flip for item {}", flip.getItemName());
    }

    public void completeFlip(Flip flip) {
        activeFlips.remove(flip);
        log.info("Completed flip for item {} for profit of {}", flip.getItemName(), flip.getProfit());
    }

    public Optional<Flip> getFlipByItemId(int itemId) {
        return activeFlips.stream()
                .filter(flip -> flip.getItemId() == itemId)
                .findFirst();
    }

    public Optional<Flip> getFlipByName(String itemName) {
        return activeFlips.stream()
                .filter(flip -> flip.getItemName().equalsIgnoreCase(itemName))
                .findFirst();
    }

    public List<Flip> getFlipsByStatus(Flip.FlipStatus status) {
        return activeFlips.stream()
                .filter(flip -> flip.getStatus() == status)
                .collect(Collectors.toList());

    }

    public int getTotalInvestment() {
        return activeFlips.stream()
                .mapToInt(Flip::getInvestment)
                .sum();
    }
}
