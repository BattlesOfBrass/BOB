package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.ModernTextField;
import de.idiotischer.bob.render.menu.components.button.BOBButton;

import javax.swing.*;
import java.awt.*;

public class MultiplayerMenu extends JPanel {

    private final int layoutScaleX = 850;
    private final int layoutScaleY = 500;

    public MultiplayerMenu() {
        this.setOpaque(false);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(layoutScaleX, layoutScaleY));

        int bottomY = layoutScaleY - 60;

        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();

        nameField.setUI(new ModernTextField());
        ipField.setUI(new ModernTextField());

        int fieldWidth = 300;
        int fieldHeight = 40;
        int spacing = 15;

        int centerX = (layoutScaleX - fieldWidth) / 2;
        int startY = layoutScaleY / 2 - fieldHeight - spacing;

        addLabeledField("Name:", nameField, centerX, startY, fieldWidth, fieldHeight);
        addLabeledField("Server IP:", ipField, centerX, startY + fieldHeight + spacing, fieldWidth, fieldHeight);

        JButton backBtn = createButton("Back to Menu", 180, 40);
        backBtn.setBounds(40, bottomY, 180, 40);
        backBtn.addActionListener(e -> BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false));

        JButton joinBtn = createButton("Join", 120, 40);
        joinBtn.setBounds(layoutScaleX - 160, bottomY, 120, 40);

        this.add(backBtn);
        this.add(joinBtn);
    }

    private void addLabeledField(String labelText, JTextField field, int x, int y, int fieldWidth, int fieldHeight) {
        int labelWidth = 100;

        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setBounds(x - labelWidth - 10, y, labelWidth, fieldHeight);

        field.setBounds(x, y, fieldWidth, fieldHeight);

        this.add(label);
        this.add(field);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.dispose();
    }

    private JButton createButton(String text, int width, int height) {
        BOBButton btn = new BOBButton(text,
                Color.WHITE,
                Color.BLACK,
                Color.DARK_GRAY.darker(),
                Color.LIGHT_GRAY,
                16,
                5
        );
        btn.setPreferredSize(new Dimension(width, height));
        btn.setFocusable(false);
        return btn;
    }

}
