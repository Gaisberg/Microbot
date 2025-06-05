package net.runelite.client.plugins.microbot.geflipper.model;

import lombok.Getter;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

public class GeOffer implements GrandExchangeOffer {

    @Getter
    private final int slot;
    private final GrandExchangeOffer offer;

    public GeOffer(int slot, GrandExchangeOffer offer) {
        this.slot = slot;
        this.offer = offer;
    }

    @Override
    public int getQuantitySold() {
        return offer != null ? offer.getQuantitySold() : 0;
    }

    @Override
    public int getItemId() {
        return offer != null ? offer.getItemId() : 0;
    }

    @Override
    public int getTotalQuantity() {
        return offer != null ? offer.getTotalQuantity() : 0;
    }

    @Override
    public int getPrice() {
        return offer != null ? offer.getPrice() : 0;
    }

    @Override
    public int getSpent() {
        return offer != null ? offer.getSpent() : 0;
    }

    @Override
    public GrandExchangeOfferState getState() {
        return offer != null ? offer.getState() : GrandExchangeOfferState.EMPTY;
    }
}