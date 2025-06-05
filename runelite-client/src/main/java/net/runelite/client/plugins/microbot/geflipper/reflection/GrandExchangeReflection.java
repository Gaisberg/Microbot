package net.runelite.client.plugins.microbot.geflipper.reflection;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.geflipper.model.GeOffer;

import java.lang.reflect.Field;
import java.util.Optional;

@Slf4j
public class GrandExchangeReflection {

    private static final Field GRAND_EXCHANGE_OFFERS_FIELD;

    static {
        Field offersField = null;

        try {
            Client client = Microbot.getClient();
            Class<?> clientClass = client.getClass();

            // Look for any field that is an array of GrandExchangeOffer implementations
            for (Field field : clientClass.getDeclaredFields()) {
                Class<?> fieldType = field.getType();

                // Check if it's an array
                if (fieldType.isArray()) {
                    Class<?> componentType = fieldType.getComponentType();

                    // Check if the component type implements GrandExchangeOffer
                    if (GrandExchangeOffer.class.isAssignableFrom(componentType)) {
                        offersField = field;
                        offersField.setAccessible(true);
                        log.info("Found GrandExchangeOffer array field: {} of type {}[]",
                                field.getName(), componentType.getSimpleName());
                        break;
                    }
                }
            }

            if (offersField == null) {
                log.error("Could not find GrandExchangeOffer array field in client class");
            }

        } catch (Exception e) {
            log.error("Failed to initialize GrandExchangeReflectionHelper", e);
        }

        GRAND_EXCHANGE_OFFERS_FIELD = offersField;
    }

    /**
     * Get Grand Exchange offers using reflection
     */
    private static GrandExchangeOffer[] getGrandExchangeOffers() {
        try {
            if (GRAND_EXCHANGE_OFFERS_FIELD == null) {
                log.warn("GrandExchangeOffer field not initialized");
                return new GrandExchangeOffer[0];
            }

            Object offersArray = GRAND_EXCHANGE_OFFERS_FIELD.get(Microbot.getClient());

            if (offersArray == null) {
                return new GrandExchangeOffer[0];
            }

            return (GrandExchangeOffer[]) offersArray;

        } catch (Exception e) {
            log.error("Error getting GE offers", e);
            return new GrandExchangeOffer[0];
        }
    }

    /**
     * Get Grand Exchange offers with slot information
     */
    public static GeOffer[] getGeOffers() {
        GrandExchangeOffer[] offers = getGrandExchangeOffers();
        GeOffer[] geOffers = new GeOffer[offers.length];

        for (int i = 0; i < offers.length; i++) {
            geOffers[i] = new GeOffer(i, offers[i]);
        }

        return geOffers;
    }

    /**
     * Get offer by slot index
     */
    public static Optional<GeOffer> getGeOfferBySlot(int slot) {
        GeOffer[] offers = getGeOffers();
        if (slot >= 0 && slot < offers.length) {
            return Optional.of(offers[slot]);
        }
        return Optional.empty();
    }

    /**
     * Check if the helper is properly initialized
     */
    public static boolean isInitialized() {
        return GRAND_EXCHANGE_OFFERS_FIELD != null;
    }

    /**
     * Get debug information
     */
    public static String getDebugInfo() {
        return String.format("Field: %s, Initialized: %s",
                GRAND_EXCHANGE_OFFERS_FIELD != null ? GRAND_EXCHANGE_OFFERS_FIELD.getName() : "null",
                isInitialized());
    }
}
