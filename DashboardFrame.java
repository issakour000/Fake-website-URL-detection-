import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DashboardFrame extends JFrame {

    private PhisingDetector.ScanResult lastScanResult;
    private String lastScannedUrl;
    private final String currentUserEmail;

    // Scan tab components
    private final JTextField urlField = new JTextField();
    private final RiskGauge gauge = new RiskGauge();
    private final DefaultTableModel featureModel = Theme.tableModel("Check", "Status", "Points", "Details");
    private final JTextArea liveArea = new JTextArea();

    // Batch tab components
    private final List<String> batchUrls = new ArrayList<>();
    private final List<PhisingDetector.ScanResult> batchResults = new ArrayList<>();
    private final DefaultTableModel batchModel = Theme.tableModel("URL", "Score", "Verdict");
    private final JLabel batchInfo = createLabel("No file loaded", BentoTheme.BODY_FONT, Color.GRAY);

    // History tab components
    private final DefaultTableModel historyModel = Theme.tableModel("Time", "URL", "Score", "Verdict");
    private final StatsPanel statsPanel = new StatsPanel();

    private final JTabbedPane tabs = new JTabbedPane();

    public DashboardFrame(String email) {
        this.currentUserEmail = email;

        setTitle("Phishing Detection Dashboard");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BentoTheme.BACKGROUND);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        tabs.setFont(BentoTheme.BODY_FONT);
        tabs.addTab("Scan URL", buildScanTab());
        tabs.addTab("Batch Scan", buildBatchTab());
        tabs.addTab("History & Statistics", buildHistoryTab());
        // Existing external panels integrated seamlessly
        tabs.addTab("ML Model (Dataset)", new MLPanel());
        tabs.addTab("Simulation", new SimulationPanel());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) refreshHistory();
        });
        add(tabs, BorderLayout.CENTER);

        refreshHistory();
    }

    private JPanel buildHeader() {
        BentoCard header = new BentoCard(BentoTheme.CARD_BG_LIGHT, 15);
        header.setLayout(new BorderLayout());
        
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(createLabel("Fake Website Detector", BentoTheme.HEADER_FONT, BentoTheme.TEXT_PRIMARY));
        
        String name = DataBaseConnection.getName(currentUserEmail);
        left.add(createLabel("Logged in as " + name + " (" + currentUserEmail + ")", BentoTheme.BODY_FONT, Color.GRAY));

        JButton logout = new JButton("Logout");
        logout.setBackground(new Color(231, 76, 60));
        logout.setForeground(Color.WHITE);
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        header.add(left, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    private JPanel buildScanTab() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10); // Generous Spacing Gutter

        // --- Card 1: Input & Verdict (Top Left, Spans 2 Columns) ---
        BentoCard inputCard = new BentoCard(BentoTheme.CARD_BG_LIGHT, 25);
        
        urlField.setFont(new Font("Consolas", Font.PLAIN, 15));
        urlField.setToolTipText("Example: https://example.com/login");
        urlField.addActionListener(e -> doScan());

        JButton scan = new JButton("Scan");
        scan.setBackground(BentoTheme.ACCENT);
        scan.setForeground(Color.WHITE);
        scan.addActionListener(e -> doScan());

        JPanel inputWrapper = new JPanel(new BorderLayout(10, 0));
        inputWrapper.setOpaque(false);
        inputWrapper.add(createLabel("Target URL:", BentoTheme.BODY_FONT, BentoTheme.TEXT_PRIMARY), BorderLayout.WEST);
        inputWrapper.add(urlField, BorderLayout.CENTER);
        inputWrapper.add(scan, BorderLayout.EAST);

        inputCard.add(inputWrapper, BorderLayout.NORTH);
        inputCard.add(gauge, BorderLayout.CENTER);
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2; gbc.gridheight = 1;
        gbc.weightx = 0.7; gbc.weighty = 0.2;
        mainPanel.add(inputCard, gbc);

        // --- Card 2: Quick Actions (Top Right) ---
        BentoCard actionCard = new BentoCard(BentoTheme.CARD_BG_DARK, 25);
        actionCard.setLayout(new GridLayout(3, 1, 0, 10));
        
        JButton live = new JButton("Live Network Check");
        JButton export = new JButton("Export Report");
        JButton clear = new JButton("Clear Results");
        
        live.addActionListener(e -> doLiveCheck());
        export.addActionListener(e -> doExport());
        clear.addActionListener(e -> doClear());
        
        actionCard.add(live);
        actionCard.add(export);
        actionCard.add(clear);

        gbc.gridx = 2; gbc.gridy = 0;
        gbc.gridwidth = 1; gbc.gridheight = 1;
        gbc.weightx = 0.3; gbc.weighty = 0.2;
        mainPanel.add(actionCard, gbc);

        // --- Card 3: Feature Breakdown Table (Bottom Left, Spans 2 Columns) ---
        BentoCard tableCard = new BentoCard(BentoTheme.CARD_BG_LIGHT, 25);
        tableCard.add(createLabel("Feature Breakdown", BentoTheme.HEADER_FONT, BentoTheme.TEXT_PRIMARY), BorderLayout.NORTH);
        
        JTable table = new JTable(featureModel);
        Theme.styleTable(table, 1);
        table.getColumnModel().getColumn(0).setPreferredWidth(190);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(520);
        
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableCard.add(tableScroll, BorderLayout.CENTER);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2; gbc.gridheight = 1;
        gbc.weightx = 0.7; gbc.weighty = 0.8;
        mainPanel.add(tableCard, gbc);

        // --- Card 4: Live Network Log (Bottom Right) ---
        BentoCard logCard = new BentoCard(BentoTheme.CARD_BG_LIGHT, 25);
        logCard.add(createLabel("Live Execution Logs", BentoTheme.HEADER_FONT, BentoTheme.TEXT_PRIMARY), BorderLayout.NORTH);
        
        liveArea.setEditable(false);
        liveArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        liveArea.setBackground(new Color(12, 20, 36));
        liveArea.setForeground(new Color(120, 230, 160));
        liveArea.setMargin(new Insets(8, 10, 8, 10));
        liveArea.setText("Press \"Live Network Check\" to test DNS, HTTP status, redirects, and TLS.");
        
        JScrollPane liveScroll = new JScrollPane(liveArea);
        liveScroll.setBorder(BorderFactory.createEmptyBorder());
        logCard.add(liveScroll, BorderLayout.CENTER);

        gbc.gridx = 2; gbc.gridy = 1;
        gbc.gridwidth = 1; gbc.gridheight = 1;
        gbc.weightx = 0.3; gbc.weighty = 0.8;
        mainPanel.add(logCard, gbc);

        return mainPanel;
    }

    private JPanel buildBatchTab() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        BentoCard controlCard = new BentoCard(BentoTheme.CARD_BG_LIGHT, 20);
        JButton load = new JButton("Load File (.txt / .csv)");
        JButton scanAll = new JButton("Scan All");
        JButton save = new JButton("Save Results (CSV)");
        
        load.addActionListener(e -> loadBatchFile());
        scanAll.addActionListener(e -> scanAllBatch());
        save.addActionListener(e -> saveBatchCsv());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(load);
        btnPanel.add(scanAll);
        btnPanel.add(save);
        btnPanel.add(batchInfo);
        
        controlCard.add(createLabel("Batch Processing", BentoTheme.HEADER_FONT, BentoTheme.TEXT_PRIMARY), BorderLayout.NORTH);
        controlCard.add(btnPanel, BorderLayout.CENTER);
        controlCard.add(createLabel("Format: one URL per line. Lines starting with # are ignored.", BentoTheme.BODY_FONT, Color.GRAY), BorderLayout.SOUTH);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0.1;
        mainPanel.add(controlCard, gbc);

        BentoCard tableCard = new BentoCard(BentoTheme.CARD_BG_LIGHT, 20);
        JTable table = new JTable(batchModel);
        Theme.styleTable(table, 2);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        gbc.gridy = 1; gbc.weighty = 0.9;
        mainPanel.add(tableCard, gbc);
        return mainPanel;
    }

    private JPanel buildHistoryTab() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        BentoCard tableCard = new BentoCard(BentoTheme.CARD_BG_LIGHT, 25);
        tableCard.add(createLabel("Scan History", BentoTheme.HEADER_FONT, BentoTheme.TEXT_PRIMARY), BorderLayout.NORTH);
        JTable table = new JTable(historyModel);
        Theme.styleTable(table, 3);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 2;
        gbc.weightx = 0.7; gbc.weighty = 1.0;
        mainPanel.add(tableCard, gbc);

        BentoCard statsBento = new BentoCard(BentoTheme.CARD_BG_LIGHT, 25);
        statsBento.add(statsPanel, BorderLayout.CENTER);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 1;
        gbc.weightx = 0.3; gbc.weighty = 0.8;
        mainPanel.add(statsBento, gbc);

        BentoCard controlCard = new BentoCard(BentoTheme.CARD_BG_DARK, 20);
        controlCard.setLayout(new GridLayout(2, 1, 0, 10));
        JButton refresh = new JButton("Refresh History");
        JButton clear = new JButton("Clear History");
        refresh.addActionListener(e -> refreshHistory());
        clear.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Delete all your saved scans?",
                    "Clear history", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                ScanHistory.clear(currentUserEmail);
                refreshHistory();
            }
        });
        controlCard.add(refresh);
        controlCard.add(clear);

        gbc.gridy = 1; gbc.weighty = 0.2;
        mainPanel.add(controlCard, gbc);

        return mainPanel;
    }

    private JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    // ================================================================ Internal Logic

    private void doScan() {
        String url = urlField.getText().trim();
        if (!PhisingDetector.looksLikeUrl(url)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid URL, e.g. https://example.com/login",
                    "Invalid URL", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PhisingDetector.ScanResult r = PhisingDetector.analyze(url);
        lastScanResult = r;
        lastScannedUrl = url;

        gauge.setResult(r.score, r.verdict);
        featureModel.setRowCount(0);
        for (FeatureExtractor.Feature f : r.features) {
            String pts = f.points == 0 ? "-" : (f.points > 0 ? "+" + f.points : String.valueOf(f.points));
            featureModel.addRow(new Object[]{f.name, f.triggered ? "FLAGGED" : "OK", pts, f.description});
        }
        if (MLModel.isTrained()) {
            double p = MLModel.predict(url);
            featureModel.addRow(new Object[]{"ML model (dataset)", p >= 0.5 ? "FLAGGED" : "OK", "-",
                    String.format("Phishing probability %.1f%% (model trained on %d URLs)", p * 100, MLModel.trainedOn())});
        }
        ScanHistory.add(currentUserEmail, r);
        refreshHistory();
    }

    private void doLiveCheck() {
        final String url = urlField.getText().trim();
        if (!PhisingDetector.looksLikeUrl(url)) {
            JOptionPane.showMessageDialog(this, "Enter a URL first.", "Invalid URL", JOptionPane.WARNING_MESSAGE);
            return;
        }
        liveArea.setText("Checking " + url + " ...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return URLscanner.liveCheck(url);
            }
            @Override protected void done() {
                try {
                    liveArea.setText(get());
                } catch (Exception ex) {
                    liveArea.setText("Live check failed: " + ex.getMessage());
                }
                liveArea.setCaretPosition(0);
            }
        }.execute();
    }

    private void doExport() {
        if (lastScanResult == null) {
            JOptionPane.showMessageDialog(this, "Scan a URL first, then export its report.",
                    "Nothing to export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("scan_report.html"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("HTML report (*.html)", "html"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Text report (*.txt)", "txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        String n = f.getName().toLowerCase();
        if (!n.endsWith(".html") && !n.endsWith(".txt")) f = new File(f.getPath() + ".html");
        try {
            ReportGenerator.save(f, lastScanResult, currentUserEmail);
            JOptionPane.showMessageDialog(this, "Report saved to:\n" + f.getAbsolutePath(),
                    "Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save report: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doClear() {
        urlField.setText("");
        featureModel.setRowCount(0);
        gauge.setResult(0, null);
        liveArea.setText("Press \"Live Check\" to test DNS, HTTP status, redirects and TLS for this URL.");
        lastScanResult = null;
        lastScannedUrl = null;
    }

    private void loadBatchFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Text / CSV files", "txt", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            batchUrls.clear();
            batchResults.clear();
            batchModel.setRowCount(0);
            for (String line : lines) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                if (s.contains(",")) s = s.substring(0, s.indexOf(','));
                s = s.replace("\"", "").trim();
                if (s.equalsIgnoreCase("url")) continue;      
                if (PhisingDetector.looksLikeUrl(s)) batchUrls.add(s);
            }
            batchInfo.setText(batchUrls.size() + " URL(s) loaded from " + f.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scanAllBatch() {
        if (batchUrls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Load a file with URLs first.",
                    "No URLs", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        batchModel.setRowCount(0);
        batchResults.clear();
        final List<String> urls = new ArrayList<>(batchUrls);

        new SwingWorker<Void, PhisingDetector.ScanResult>() {
            @Override protected Void doInBackground() {
                for (String u : urls) publish(PhisingDetector.analyze(u));
                return null;
            }
            @Override protected void process(List<PhisingDetector.ScanResult> chunks) {
                for (PhisingDetector.ScanResult r : chunks) {
                    batchResults.add(r);
                    batchModel.addRow(new Object[]{r.url, String.valueOf(r.score), r.verdict});
                    ScanHistory.add(currentUserEmail, r);
                }
                batchInfo.setText("Scanned " + batchResults.size() + " / " + urls.size());
            }
            @Override protected void done() {
                int bad = 0;
                for (PhisingDetector.ScanResult r : batchResults) if (!r.verdict.equals("SAFE")) bad++;
                batchInfo.setText("Done: " + batchResults.size() + " scanned, " + bad + " not safe");
                refreshHistory();
            }
        }.execute();
    }

    private void saveBatchCsv() {
        if (batchResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run \"Scan All\" first.",
                    "Nothing to save", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("batch_results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        List<String> out = new ArrayList<>();
        out.add("url,score,verdict");
        for (PhisingDetector.ScanResult r : batchResults) {
            out.add("\"" + r.url.replace("\"", "\"\"") + "\"," + r.score + "," + r.verdict);
        }
        try {
            Files.write(f.toPath(), out, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Saved to:\n" + f.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshHistory() {
        List<ScanHistory> list = ScanHistory.forUser(currentUserEmail);
        historyModel.setRowCount(0);
        for (int i = list.size() - 1; i >= 0; i--) {          
            ScanHistory h = list.get(i);
            historyModel.addRow(new Object[]{h.time, h.url, String.valueOf(h.score), h.verdict});
        }
        statsPanel.setCounts(ScanHistory.counts(list));
    }

    // ===================================================== Custom AWT Panels

    private static class RiskGauge extends JPanel {
        private int score;
        private String verdict;

        RiskGauge() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 72));
        }

        void setResult(int score, String verdict) {
            this.score = score;
            this.verdict = verdict;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g.setColor(BentoTheme.BACKGROUND);
            g.fillRoundRect(0, 0, w, h, 16, 16);

            int bx = 16, bh = 12, by = h - 24, bw = w - 32;
            g.setColor(new Color(50, 65, 95));
            g.fillRoundRect(bx, by, bw, bh, bh, bh);

            Color c = verdict == null ? Color.GRAY : Theme.verdictColor(verdict);
            if (verdict != null && score > 0) {
                g.setColor(c);
                g.fillRoundRect(bx, by, Math.max(bh, bw * score / 100), bh, bh, bh);
            }
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.setColor(c);
            String text = verdict == null ? "Enter a URL and press Scan"
                    : verdict + "   (risk " + score + " / 100)";
            g.drawString(text, 16, 30);
            g.dispose();
        }
    }

    private static class StatsPanel extends JPanel {
        private int[] counts = new int[3];

        StatsPanel() {
            setOpaque(false);
        }

        void setCounts(int[] c) {
            counts = c;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g.setColor(BentoTheme.BACKGROUND);
            g.fillRoundRect(0, 0, w, h, 16, 16);
            g.setFont(BentoTheme.HEADER_FONT);
            g.setColor(BentoTheme.TEXT_PRIMARY);
            g.drawString("Scan Statistics", 16, 30);

            int total = counts[0] + counts[1] + counts[2];
            if (total == 0) {
                g.setColor(Color.GRAY);
                g.drawString("No scans yet", w / 2 - 40, h / 2);
                g.dispose();
                return;
            }

            String[] names = {"Safe", "Suspicious", "Phishing"};
            Color[] cols = {Theme.SAFE, Theme.WARN, Theme.DANGER};

            int d = Math.max(60, Math.min(w - 60, h - 160));
            int x = (w - d) / 2, y = 50;
            int last = 2;
            while (last > 0 && counts[last] == 0) last--;

            int start = 90, used = 0;
            for (int i = 0; i < 3; i++) {
                if (counts[i] == 0) continue;
                int angle = (i == last) ? 360 - used : Math.round(360f * counts[i] / total);
                g.setColor(cols[i]);
                g.fillArc(x, y, d, d, start, -angle);
                start -= angle;
                used += angle;
            }

            int ly = y + d + 30;
            for (int i = 0; i < 3; i++) {
                g.setColor(cols[i]);
                g.fillRoundRect(24, ly - 12, 14, 14, 4, 4);
                g.setColor(BentoTheme.TEXT_PRIMARY);
                g.setFont(BentoTheme.BODY_FONT);
                g.drawString(names[i] + ": " + counts[i] + "  (" + (100 * counts[i] / total) + "%)", 46, ly);
                ly += 24;
            }
            g.dispose();
        }
    }
}