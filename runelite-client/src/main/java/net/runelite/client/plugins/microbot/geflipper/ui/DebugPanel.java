package net.runelite.client.plugins.microbot.geflipper.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperPlugin;
import net.runelite.client.plugins.microbot.geflipper.services.GeService;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
public class DebugPanel extends JPanel {

    private final GeFlipperPlugin plugin;
    private final GeService geService;

    public DebugPanel(GeFlipperPlugin plugin) {
        this.plugin = plugin;
        this.geService = plugin.getGeService();
        initializePanel();
    }

    private void initializePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Debug title
        JLabel debugTitle = new JLabel("🐛 DEBUG MODE");
        debugTitle.setForeground(ColorScheme.BRAND_ORANGE);
        debugTitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(debugTitle);

        JCheckBox pausedCheckbox = new JCheckBox("Paused");
        pausedCheckbox.setSelected(plugin.isPaused());
        pausedCheckbox.addActionListener(e -> plugin.setPaused(pausedCheckbox.isSelected()));
        add(pausedCheckbox);

        // GE Service Testing Section
        add(createDebugSection("GE Service Testing",
                createGeStatusPanel(),
                createGeActionPanel(),
                createOfferTestPanel()
        ));
    }

    private JPanel createDebugSection(String title, JPanel... contentPanels) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        section.setBorder(new EmptyBorder(5, 0, 15, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        section.add(titleLabel);

        for (JPanel contentPanel : contentPanels) {
            section.add(contentPanel);
        }

        return section;
    }

    private JPanel createGeStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 5));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(5, 0, 10, 0));

        // GE Open Status
        JLabel geOpenLabel = new JLabel("GE Open:");
        geOpenLabel.setForeground(Color.WHITE);
        JLabel geOpenStatus = new JLabel("Unknown");
        geOpenStatus.setForeground(Color.GRAY);

        // Available Slots
        JLabel slotsLabel = new JLabel("Available Slots:");
        slotsLabel.setForeground(Color.WHITE);
        JLabel slotsStatus = new JLabel("Unknown");
        slotsStatus.setForeground(Color.GRAY);

        // Refresh Button
        JButton refreshButton = new JButton("Refresh Status");
        styleDebugButton(refreshButton);
        refreshButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    boolean isOpen = geService.isGrandExchangeOpen();
                    geOpenStatus.setText(isOpen ? "OPEN" : "CLOSED");
                    geOpenStatus.setForeground(isOpen ? Color.GREEN : Color.RED);

                    boolean hasSlots = geService.slotsAvailable();
                    int availableSlot = geService.getAvailableSlot().orElse(-1);
                    slotsStatus.setText(hasSlots ? "Available (Slot " + availableSlot + ")" : "None Available");
                    slotsStatus.setForeground(hasSlots ? Color.GREEN : Color.RED);
                } catch (Exception ex) {
                    log.error("Error refreshing GE status", ex);
                    showDebugMessage("Error refreshing status: " + ex.getMessage(), false);
                }
            });
        });

        panel.add(geOpenLabel);
        panel.add(geOpenStatus);
        panel.add(slotsLabel);
        panel.add(slotsStatus);
        panel.add(refreshButton);
        panel.add(new JLabel()); // Empty cell

        return panel;
    }

    private JPanel createGeActionPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 5));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(5, 0, 10, 0));

        // Open GE Button
        JButton openGeButton = new JButton("Open GE");
        styleDebugButton(openGeButton);
        openGeButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    boolean success = geService.openGrandExchange();
                    showDebugMessage(success ? "GE opened successfully" : "Failed to open GE", success);
                } catch (Exception ex) {
                    log.error("Error opening GE", ex);
                    showDebugMessage("Error opening GE: " + ex.getMessage(), false);
                }
            });
        });

        // Get Offers Button
        JButton getOffersButton = new JButton("Get Offers");
        styleDebugButton(getOffersButton);
        getOffersButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    var offers = geService.getOffers();
                    StringBuilder sb = new StringBuilder("Current Offers:\n");
                    for (int i = 0; i < offers.length; i++) {
                        var offer = offers[i];
                        sb.append(String.format("Slot %d: %s - Item: %d, State: %s\n",
                                i, offer != null ? "Active" : "Empty",
                                offer != null ? offer.getItemId() : 0,
                                offer != null ? offer.getState() : "EMPTY"));
                    }
                    showDebugMessage(sb.toString(), true);
                } catch (Exception ex) {
                    log.error("Error getting offers", ex);
                    showDebugMessage("Error getting offers: " + ex.getMessage(), false);
                }
            });
        });

        panel.add(openGeButton);
        panel.add(getOffersButton);

        return panel;
    }

    private JPanel createOfferTestPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(5, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Item ID field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel itemIdLabel = new JLabel("Item ID:");
        itemIdLabel.setForeground(Color.WHITE);
        panel.add(itemIdLabel, gbc);

        gbc.gridx = 1;
        JTextField itemIdField = new JTextField("554", 10); // Logs as default
        styleDebugTextField(itemIdField);
        panel.add(itemIdField, gbc);

        // Quantity field
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setForeground(Color.WHITE);
        panel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        JTextField quantityField = new JTextField("1", 10);
        styleDebugTextField(quantityField);
        panel.add(quantityField, gbc);

        // Price field
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setForeground(Color.WHITE);
        panel.add(priceLabel, gbc);

        gbc.gridx = 1;
        JTextField priceField = new JTextField("100", 10);
        styleDebugTextField(priceField);
        panel.add(priceField, gbc);

        // Slot field
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel slotLabel = new JLabel("Slot (optional):");
        slotLabel.setForeground(Color.WHITE);
        panel.add(slotLabel, gbc);

        gbc.gridx = 1;
        JTextField slotField = new JTextField("", 10);
        styleDebugTextField(slotField);
        panel.add(slotField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        JButton buyButton = new JButton("Place Buy Offer");
        styleDebugButton(buyButton);
        buyButton.addActionListener(e -> {
            try {
                int itemId = Integer.parseInt(itemIdField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                int price = Integer.parseInt(priceField.getText());

                int result;
                if (slotField.getText().trim().isEmpty()) {
                    result = geService.placeBuyOffer(itemId, quantity, price);
                } else {
                    int slot = Integer.parseInt(slotField.getText());
                    result = geService.placeBuyOffer(itemId, quantity, price, slot);
                }

                showDebugMessage(result >= 0 ?
                        "Buy offer placed successfully in slot " + result :
                        "Failed to place buy offer", result >= 0);
            } catch (NumberFormatException ex) {
                showDebugMessage("Invalid number format", false);
            } catch (Exception ex) {
                log.error("Error placing buy offer", ex);
                showDebugMessage("Error placing buy offer: " + ex.getMessage(), false);
            }
        });
        panel.add(buyButton, gbc);

        gbc.gridx = 1;
        JButton sellButton = new JButton("Place Sell Offer");
        styleDebugButton(sellButton);
        sellButton.addActionListener(e -> {
            try {
                int itemId = Integer.parseInt(itemIdField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                int price = Integer.parseInt(priceField.getText());

                int result = geService.placeSellOffer(itemId, quantity, price);
                showDebugMessage(result >= 0 ?
                        "Sell offer placed successfully in slot " + result :
                        "Failed to place sell offer", result >= 0);
            } catch (NumberFormatException ex) {
                showDebugMessage("Invalid number format", false);
            } catch (Exception ex) {
                log.error("Error placing sell offer", ex);
                showDebugMessage("Error placing sell offer: " + ex.getMessage(), false);
            }
        });
        panel.add(sellButton, gbc);

        // Cancel/Collect buttons
        gbc.gridx = 0; gbc.gridy = 5;
        JButton cancelButton = new JButton("Cancel Offer");
        styleDebugButton(cancelButton);
        cancelButton.addActionListener(e -> {
            try {
                String slotText = slotField.getText().trim();
                if (slotText.isEmpty()) {
                    showDebugMessage("Please specify a slot to cancel", false);
                    return;
                }
                int slot = Integer.parseInt(slotText);
                boolean success = geService.cancelOffer(slot);
                showDebugMessage(success ?
                        "Offer cancelled successfully from slot " + slot :
                        "Failed to cancel offer", success);
            } catch (NumberFormatException ex) {
                showDebugMessage("Invalid slot number", false);
            } catch (Exception ex) {
                log.error("Error cancelling offer", ex);
                showDebugMessage("Error cancelling offer: " + ex.getMessage(), false);
            }
        });
        panel.add(cancelButton, gbc);

        gbc.gridx = 1;
        JButton collectButton = new JButton("Collect Offer");
        styleDebugButton(collectButton);
        collectButton.addActionListener(e -> {
            try {
                String slotText = slotField.getText().trim();
                if (slotText.isEmpty()) {
                    showDebugMessage("Please specify a slot to collect", false);
                    return;
                }
                int slot = Integer.parseInt(slotText);
                boolean success = geService.collectOffer(slot);
                showDebugMessage(success ?
                        "Offer collected successfully from slot " + slot :
                        "Failed to collect offer", success);
            } catch (NumberFormatException ex) {
                showDebugMessage("Invalid slot number", false);
            } catch (Exception ex) {
                log.error("Error collecting offer", ex);
                showDebugMessage("Error collecting offer: " + ex.getMessage(), false);
            }
        });
        panel.add(collectButton, gbc);

        return panel;
    }

    private void styleDebugButton(JButton button) {
        button.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(6, 10, 6, 10));
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 11f));
    }

    private void styleDebugTextField(JTextField field) {
        field.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void showDebugMessage(String message, boolean success) {
        SwingUtilities.invokeLater(() -> {
            String title = success ? "Debug Success" : "Debug Error";
            int messageType = success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;

            // Create a scrollable text area for long messages
            JTextArea textArea = new JTextArea(message);
            textArea.setEditable(false);
            textArea.setBackground(success ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
            textArea.setForeground(Color.WHITE);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, Math.min(200, message.split("\n").length * 20 + 40)));

            JOptionPane.showMessageDialog(this, scrollPane, title, messageType);

            // Also log the message
            if (success) {
                log.info("Debug: {}", message);
            } else {
                log.warn("Debug Error: {}", message);
            }
        });
    }
}
