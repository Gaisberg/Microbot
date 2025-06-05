package net.runelite.client.plugins.microbot.geflipper.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperPlugin;
import net.runelite.client.plugins.microbot.geflipper.services.GeService;
import net.runelite.client.plugins.microbot.geflipper.services.JavaScriptEvaluator;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

@Slf4j
public class Panel extends PluginPanel {

    private final GeFlipperPlugin plugin;
    private final JavaScriptEvaluator evaluator;
    private final GeService geService;

    // UI Components
    private ConfigurationPanel configPanel;
    private DebugPanel debugPanel;
    private JButton configToggleButton;
    private boolean configExpanded = false;
    private boolean debugExpanded = false;

    public Panel(GeFlipperPlugin plugin) {
        this.plugin = plugin;
        this.evaluator = plugin.getEvaluator();
        this.geService = plugin.getGeService();

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // Configuration toggle button
        configToggleButton = new JButton("⚙ Configure");
        configToggleButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        configToggleButton.setForeground(Color.WHITE);
        configToggleButton.setFocusPainted(false);
        configToggleButton.setBorder(new EmptyBorder(8, 12, 8, 12));
        configToggleButton.addActionListener(this::onConfigToggle);

        // Configuration panel
        configPanel = new ConfigurationPanel(plugin);
        configPanel.setVisible(false);

        // Debug panel
        debugPanel = new DebugPanel(plugin);
        debugPanel.setVisible(false);
    }

    private void onConfigToggle(ActionEvent e) {
        boolean ctrlPressed = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;

        if (ctrlPressed) {
            // Toggle debug panel
            debugExpanded = !debugExpanded;
            debugPanel.setVisible(debugExpanded);

            // Hide config panel when showing debug
            if (debugExpanded) {
                configExpanded = false;
                configPanel.setVisible(false);
            }

            // Update button text to show debug mode
            if (debugExpanded) {
                configToggleButton.setText("🐛 Hide Debug");
                configToggleButton.setBackground(ColorScheme.BRAND_ORANGE);
            } else {
                configToggleButton.setText("⚙ Configure");
                configToggleButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }
        } else {
            // Toggle config panel
            configExpanded = !configExpanded;
            configPanel.setVisible(configExpanded);

            // Hide debug panel when showing config
            if (configExpanded) {
                debugExpanded = false;
                debugPanel.setVisible(false);
            }

            if (!debugExpanded) {
                configToggleButton.setText(configExpanded ? "⚙ Hide Configuration" : "⚙ Configure");
                configToggleButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }
        }

        revalidate();
        repaint();
    }

    private void layoutComponents() {
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("GE Flipper");
        titleLabel.setForeground(Color.WHITE);

        // Add tooltip to explain debug mode
        configToggleButton.setToolTipText("Click to configure settings, Ctrl+Click for debug mode");

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(configToggleButton, BorderLayout.EAST);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.add(configPanel, BorderLayout.NORTH);
        contentPanel.add(debugPanel, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.NORTH);
    }
}
