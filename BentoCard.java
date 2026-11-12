import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class BentoCard extends JPanel {
    private final int cornerRadius = 30;

    public BentoCard(Color backgroundColor, int padding) {
        super();
        setOpaque(false);
        setBackground(backgroundColor);
        setLayout(new BorderLayout(10, 10)); // Internal component gaps
        
        // Principle: Internal Padding - Keeps content away from edges
        setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Principle: Strict Containment - distinct background with rounded corners
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        
        g2.dispose();
        super.paintComponent(g);
    }
}