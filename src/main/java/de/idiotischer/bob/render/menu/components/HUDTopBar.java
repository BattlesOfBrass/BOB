
package de.idiotischer.bob.render.menu.components;

import de.idiotischer.bob.BOB;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HUDTopBar extends JPanel {
    private String countryName = "---";

    private static final int ARROW_WIDTH = 24;
    private static final int SPEED_WIDTH = 22;
    private static final int SPEED_GAP = 3;
    private static final int CONTROL_GAP = 8;

    private final Rectangle slowerBounds = new Rectangle();
    private final Rectangle fasterBounds = new Rectangle();
    private final Rectangle[] speedBounds = {new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()};

    public HUDTopBar() {
        setOpaque(false);
        setLayout(new BorderLayout());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int speed = BOB.getInstance().getTimeManager().getSpeed();

                if (slowerBounds.contains(e.getPoint())) {
                    changeSpeed(speed - 1);
                } else if (fasterBounds.contains(e.getPoint())) {
                    changeSpeed(speed + 1);
                } else {
                    for (int i = 0; i < speedBounds.length; i++) {
                        if (speedBounds[i].contains(e.getPoint())) {
                            changeSpeed(i + 1);
                            break;
                        }
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hovering = slowerBounds.contains(e.getPoint()) || fasterBounds.contains(e.getPoint());

                for (Rectangle bounds : speedBounds) {
                    if (bounds.contains(e.getPoint())) {
                        hovering = true;
                        break;
                    }
                }

                setCursor(hovering ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    public void changeSpeed(int newSpeed) {
        int currentSpeed = BOB.getInstance().getTimeManager().getSpeed();

        newSpeed = Math.max(1, Math.min(5, newSpeed));

        if (newSpeed == currentSpeed) {
            return;
        }

        onSpeedChange(currentSpeed, newSpeed);
    }

    protected void onSpeedChange(int oldSpeed, int newSpeed) {
        boolean op = BOB.getInstance().getPlayer().hasOP();

        if(!op) return;

        BOB.getInstance().getTimeManager().setSpeed(newSpeed);
    }

    public void setCountryName(String name) {
        this.countryName = name;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        g2.setColor(dark);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setStroke(new BasicStroke(12));
        g2.setColor(dark.darker());
        g2.drawRect(0, 0, getWidth(), getHeight());

        g2.setColor(Color.GREEN);
        Font font = /*FileUtil.getFont();*/new Font(Font.SANS_SERIF, Font.BOLD, 15);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();

        String text = "Current country: " + countryName;
        int x = 20;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);

        Date currentDate = BOB.getInstance().getTimeManager().getDate();

        String date = new SimpleDateFormat("dd.MM.yyyy").format(currentDate);

        int dateX = getWidth() - fm.stringWidth(date) - 20;
        g2.drawString(date, dateX, y);

        g2.setStroke(new BasicStroke(2));

        int speed = BOB.getInstance().getTimeManager().getSpeed();

        int totalWidth = ARROW_WIDTH + CONTROL_GAP + 5 * SPEED_WIDTH + 4 * SPEED_GAP + CONTROL_GAP + ARROW_WIDTH;

        int startX = dateX - totalWidth - CONTROL_GAP;

        int controlY = (getHeight() - 26) / 2;
        int controlHeight = 26;

        slowerBounds.setBounds(startX, controlY, ARROW_WIDTH, controlHeight);
        drawArrow(g2, slowerBounds, false, speed > 1);

        int speedStartX = startX + ARROW_WIDTH + CONTROL_GAP;

        for (int i = 0; i < 5; i++) {
            int speedX = speedStartX + i * (SPEED_WIDTH + SPEED_GAP);

            speedBounds[i].setBounds(speedX, controlY, SPEED_WIDTH, controlHeight);

            boolean selected = speed == i + 1;

            g2.setColor(selected ? new Color(190, 155, 65) : new Color(55, 55, 55));
            g2.fillRoundRect(speedX, controlY, SPEED_WIDTH, controlHeight, 5, 5);

            g2.setColor(selected ? new Color(255, 225, 145) : new Color(100, 100, 100));
            g2.drawRoundRect(speedX, controlY, SPEED_WIDTH, controlHeight, 5, 5);

            String label = Integer.toString(i + 1);

            g2.setColor(selected ? Color.WHITE : new Color(180, 180, 180));

            int labelX = speedX + (SPEED_WIDTH - fm.stringWidth(label)) / 2;
            int labelY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.drawString(label, labelX, labelY);
        }

        int fasterX = speedStartX + 5 * SPEED_WIDTH + 4 * SPEED_GAP + CONTROL_GAP;

        fasterBounds.setBounds(fasterX, controlY, ARROW_WIDTH, controlHeight);
        drawArrow(g2, fasterBounds, true, speed < 5);

        g2.dispose();
    }

    private void drawArrow(Graphics2D g2, Rectangle bounds, boolean right, boolean enabled) {
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        g2.setColor(enabled ? new Color(220, 220, 220) : new Color(90, 90, 90));

        int direction = right ? 1 : -1;

        Polygon arrow = new Polygon(new int[]{centerX - direction * 5, centerX + direction * 5, centerX - direction * 5}, new int[]{centerY - 7, centerY, centerY + 7}, 3);

        g2.fillPolygon(arrow);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension parentSize = getParent() != null ? getParent().getSize() : new Dimension(0, 40);
        return new Dimension(parentSize.width, 40);
    }
}
