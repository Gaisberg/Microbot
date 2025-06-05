package net.runelite.client.plugins.microbot.geflipper.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.GeFlipperPlugin;
import net.runelite.client.plugins.microbot.geflipper.services.JavaScriptEvaluator;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

@Slf4j
public class ConfigurationPanel extends JPanel {

    private final GeFlipperPlugin plugin;
    private final JavaScriptEvaluator evaluator;

    public ConfigurationPanel(GeFlipperPlugin plugin) {
        this.plugin = plugin;
        this.evaluator = plugin.getEvaluator();

        initializePanel();
    }

    private void initializePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Investment Configuration
        add(createConfigSection("Investment Settings",
                createSinglePanel("Max Investment:", "maxInvestment"),
                createSinglePanel("Undercut %:", "underCutPercentage"),
                createSinglePanel("Reserved Slots:", "reservedSlots")
        ));

        // Filter criteria configuration
        add(createConfigSection("Filter Criteria",
                createRangePanel("ROI %:", "Roi"),
                createRangePanel("Volume:", "Volume"),
                createRangePanel("Profit GP:", "Profit")
        ));

        add(createConfigSection("Custom Criteria", createCustomJavaScriptPanel()));
    }

    private JPanel createConfigSection(String title, JPanel... contentPanels) {
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

    private JPanel createRangePanel(String label, String key) {
        boolean minEnabled = plugin.getConfiguration("min" + key + "Enabled", boolean.class);
        int minValue = plugin.getConfiguration("min" + key, int.class);
        boolean maxEnabled = plugin.getConfiguration("max" + key + "Enabled", boolean.class);
        int maxValue = plugin.getConfiguration("max" + key, int.class);

        JPanel panel = new JPanel(new GridLayout(2, 3, 5, 5));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel minLabelComp = new JLabel("Min " + label);
        minLabelComp.setForeground(Color.WHITE);

        JCheckBox minCheckBox = new JCheckBox("", minEnabled);
        minCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        minCheckBox.setFocusPainted(false);

        JTextField minField = new JTextField(String.valueOf(minValue));
        styleTextField(minField);

        JLabel maxLabelComp = new JLabel("Max " + label);
        maxLabelComp.setForeground(Color.WHITE);

        JCheckBox maxCheckBox = new JCheckBox("", maxEnabled);
        maxCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        maxCheckBox.setFocusPainted(false);

        JTextField maxField = new JTextField(String.valueOf(maxValue));
        styleTextField(maxField);

        // Set initial field states
        updateFieldState(minField, minEnabled);
        updateFieldState(maxField, maxEnabled);

        // Add listeners
        minField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                plugin.setConfiguration("min" + key, minField.getText());
            }
        });
        maxField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                plugin.setConfiguration("max" + key, maxField.getText());
            }
        });
        minCheckBox.addActionListener(e -> {
            updateFieldState(minField, minCheckBox.isSelected());
            plugin.setConfiguration("min" + key + "Enabled", minCheckBox.isSelected());
        });
        maxCheckBox.addActionListener(e -> {
            updateFieldState(maxField, maxCheckBox.isSelected());
            plugin.setConfiguration("max" + key + "Enabled", maxCheckBox.isSelected());
        });

        // Add components to panel
        panel.add(minCheckBox);
        panel.add(minLabelComp);
        panel.add(minField);
        panel.add(maxCheckBox);
        panel.add(maxLabelComp);
        panel.add(maxField);

        return panel;
    }

    private JPanel createCustomJavaScriptPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        String customCode = plugin.getConfiguration("customJavaScriptCode", String.class);

        // Code text area
        JTextArea jsCodeField = new JTextArea(8, 50);
        jsCodeField.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        jsCodeField.setForeground(Color.WHITE);
        jsCodeField.setCaretColor(Color.WHITE);
        jsCodeField.setText(customCode);
        if(evaluator.getEngine() == null) {
            jsCodeField.setEnabled(false);
            jsCodeField.setText(evaluator.validateCode(""));
        }

        JScrollPane scrollPane = new JScrollPane(jsCodeField);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR));

        JLabel helpLabel = new JLabel("<html>" +
                "<b>Available FlipOpportunity methods:</b><br/>" +
                "<b>Basic Properties:</b><br/>" +
                "• <code>opportunity.getItemId()</code> - Item ID<br/>" +
                "• <code>opportunity.getName()</code> - Item name<br/>" +
                "• <code>opportunity.getLimit()</code> - Buy limit<br/>" +
                "• <code>opportunity.isMembers()</code> - Members item<br/><br/>" +

                "<b>Price & Profit:</b><br/>" +
                "• <code>opportunity.getLowPrice()</code> - Buy price<br/>" +
                "• <code>opportunity.getHighPrice()</code> - Sell price<br/>" +
                "• <code>opportunity.getProfit()</code> - Expected profit (after tax)<br/>" +
                "• <code>opportunity.getRoi()</code> - Return on investment %<br/><br/>" +

                "<b>Volume:</b><br/>" +
                "• <code>opportunity.getLowVolume()</code> - Buy volume<br/>" +
                "• <code>opportunity.getHighVolume()</code> - Sell volume<br/>" +
                "• <code>opportunity.getDailyVolume().getLeft()</code> - Daily low volume<br/>" +
                "• <code>opportunity.getDailyVolume().getRight()</code> - Daily high volume<br/><br/>" +

                "<b>Market Analysis:</b><br/>" +
                "• <code>opportunity.getTrend()</code> - Price trend (hourly % change)<br/>" +
                "• <code>opportunity.getVolatility()</code> - Price volatility %<br/><br/>" +

                "<b>Examples:</b><br/>" +
                "• <code>opportunity.getRoi() > 15 && opportunity.getProfit() > 500</code><br/>" +
                "• <code>opportunity.getName().toLowerCase().indexOf('dragon') !== -1</code><br/>" +
                "• <code>opportunity.isMembers() && opportunity.getLimit() > 100</code><br/>" +
                "• <code>opportunity.getTrend() > 0 && opportunity.getVolatility() < 20</code>" +
                "</html>");
        helpLabel.setForeground(Color.LIGHT_GRAY);
        helpLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

        // Validation button
        JButton validateButton = new JButton("Validate Code");
        validateButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        validateButton.setForeground(Color.WHITE);
        validateButton.setFocusPainted(false);
        validateButton.setBorder(new EmptyBorder(4, 8, 4, 8));

        jsCodeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                plugin.setConfiguration("customJavaScriptCode", jsCodeField.getText());
            }
        });

        validateButton.addActionListener(e -> validateJavaScriptCode(jsCodeField.getText()));

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        topPanel.add(validateButton, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(helpLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void validateJavaScriptCode(String code) {
        SwingUtilities.invokeLater(() -> {
            String error = evaluator.validateCode(code);
            if (error == null) {
                JOptionPane.showMessageDialog(this, "JavaScript code is valid!",
                        "Validation Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Validation Error:\n" + error,
                        "Validation Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updateFieldState(JTextField field, boolean enabled) {
        field.setEnabled(enabled);
        if (enabled) {
            field.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            field.setForeground(Color.WHITE);
        } else {
            field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            field.setForeground(Color.GRAY);
        }
    }

    private JPanel createSinglePanel(String label, String key) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        int value = plugin.getConfiguration(key, int.class);

        JLabel labelComp = new JLabel(label);
        labelComp.setForeground(Color.WHITE);

        JTextField field = new JTextField(String.valueOf(value));
        styleTextField(field);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                plugin.setConfiguration(key, field.getText());
            }
        });

        panel.add(labelComp);
        panel.add(field);

        return panel;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }
}

