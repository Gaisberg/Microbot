package net.runelite.client.plugins.microbot.geflipper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.runelite.client.plugins.microbot.Microbot;

@Data
@NoArgsConstructor
public class Flip {

    @JsonProperty("itemId")
    private int itemId;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("buyPrice")
    private int buyPrice;

    @JsonProperty("sellPrice")
    private int sellPrice;

    @JsonProperty("buyTime")
    private Long buyTime;

    @JsonProperty("sellTime")
    private Long sellTime;

    @JsonProperty("createdTime")
    private long createdTime;

    @JsonProperty("status")
    private FlipStatus status;

    @JsonProperty("progression")
    private int progression;

    @JsonProperty("geSlot")
    private Integer geSlot;

    @JsonProperty("itemName")
    private String itemName;

    @JsonProperty("profit")
    private int profit;

    @JsonProperty("flipDuration")
    private long flipDuration;

    @JsonProperty("investment")
    private int investment;

    public enum FlipStatus {
        PENDING,
        BUYING,
        BOUGHT,
        PENDING_SALE,
        SELLING,
        SOLD,
    }

    public Flip(int itemId, int quantity, int buyPrice, int sellPrice) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.createdTime = System.currentTimeMillis();
        this.status = FlipStatus.PENDING;
    }

    public int getProfit() {
        return (int) ((sellPrice - buyPrice) * quantity * 0.98); // 2% tax
    }

    public int getInvestment() {
        return buyPrice * quantity;
    }

    public String getItemName() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> Microbot.getClient().getItemDefinition(itemId).getName()).orElse("");
    }
}
