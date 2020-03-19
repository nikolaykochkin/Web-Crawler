package crawler;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler extends JFrame {

    private List<HtmlPage> result;

    private final JTextField urlTextField;
    private final JTextField workersTextField;
    private final JToggleButton runButton;
    private final JTextField depthTextField;
    private final JCheckBox depthCheckBox;
    private final JTextField limitTextField;
    private final JCheckBox limitCheckBox;
    private final JLabel elapsedValueLabel;
    private final JLabel parsedValueLabel;
    private final JTextField exportUrlTextField;
    private Task task;

    public WebCrawler() {

        super("Web Crawler");

        result = new ArrayList<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 255);
        setLocationRelativeTo(null);

        JLabel urlTextFieldLabel = new JLabel("Start URL:");
        urlTextFieldLabel.setBounds(5, 5, 140, 25);

        urlTextField = new JTextField();
        urlTextField.setBounds(150, 5, 300, 25);
        urlTextField.setName("UrlTextField");
        urlTextField.setText("https://www.wikipedia.org/");

        add(urlTextFieldLabel);
        add(urlTextField);

        runButton = new JToggleButton("Run");
        runButton.setBounds(455, 5, 125, 25);
        runButton.setName("RunButton");

        add(runButton);

        JLabel workersLabel = new JLabel("Workers:");
        workersLabel.setBounds(5, 35, 140, 25);

        workersTextField = new JTextField();
        workersTextField.setBounds(150, 35, 300, 25);
        workersTextField.setName("WorkersTextField");
        workersTextField.setText(Integer.toString(Runtime.getRuntime().availableProcessors()));

        add(workersLabel);
        add(workersTextField);

        JLabel depthLabel = new JLabel("Maximum depth:");
        depthLabel.setBounds(5, 65, 140, 25);

        depthTextField = new JTextField();
        depthTextField.setBounds(150, 65, 300, 25);
        depthTextField.setName("DepthTextField");
        depthTextField.setText("50");

        depthCheckBox = new JCheckBox("Enabled");
        depthCheckBox.setBounds(455, 65, 120, 25);
        depthCheckBox.setName("DepthCheckBox");

        add(depthLabel);
        add(depthTextField);
        add(depthCheckBox);

        JLabel limitLabel = new JLabel("Time limit (seconds):");
        limitLabel.setBounds(5, 95, 140, 25);

        limitTextField = new JTextField();
        limitTextField.setBounds(150, 95, 300, 25);
        limitTextField.setName("LimitTextField");

        limitCheckBox = new JCheckBox("Enabled");
        limitCheckBox.setBounds(455, 95, 120, 25);
        limitCheckBox.setName("LimitCheckBox");

        add(limitLabel);
        add(limitTextField);
        add(limitCheckBox);

        JLabel elapsedLabel = new JLabel("Elapsed time:");
        elapsedLabel.setBounds(5, 125, 140, 25);

        elapsedValueLabel = new JLabel("0:00");
        elapsedValueLabel.setBounds(150, 125, 300, 25);

        add(elapsedLabel);
        add(elapsedValueLabel);

        JLabel parsedLabel = new JLabel("Parsed pages:");
        parsedLabel.setBounds(5, 155, 140, 25);

        parsedValueLabel = new JLabel("0");
        parsedValueLabel.setBounds(150, 155, 300, 25);
        parsedValueLabel.setName("ParsedLabel");

        add(parsedLabel);
        add(parsedValueLabel);

        JLabel exportUrlFieldLabel = new JLabel("Export:");
        exportUrlFieldLabel.setBounds(5, 185, 140, 25);

        exportUrlTextField = new JTextField();
        exportUrlTextField.setBounds(150, 185, 300, 25);
        exportUrlTextField.setName("ExportUrlTextField");
        exportUrlTextField.setText("D:\\test.txt");

        add(exportUrlFieldLabel);
        add(exportUrlTextField);

        JButton exportButton = new JButton("Save");
        exportButton.setBounds(455, 185, 125, 25);
        exportButton.setName("ExportButton");
        exportButton.addActionListener(actionEvent -> saveResult());

        add(exportButton);

        runButton.addActionListener(actionEvent -> {
            if (runButton.isSelected()) {
                task = new Task();
                task.execute();
            } else {
                task.phaser.forceTermination();
                task.cancel(true);
                task = null;
            }
        });

        setLayout(null);
        setVisible(true);
    }

    class Task extends SwingWorker<Void, Void> {

        private ExecutorService executor;
        private Set<String> parsedURLs;
        private int maxDepth;
        private long parsedPages;
        LocalDateTime startTime;
        Phaser phaser;
        @Override
        protected Void doInBackground() {

            result.clear();

            phaser = new Phaser();
            int phase = phaser.getPhase();
            parsedURLs = new HashSet<>();
            parsedPages = 0;
            maxDepth = depthCheckBox.isSelected() ? Integer.parseInt(depthTextField.getText()) : 0;

            int workersCount = Integer.parseInt(workersTextField.getText());
            int timeLimit = limitCheckBox.isSelected() ? Integer.parseInt(limitTextField.getText()) : 0;
            startTime = LocalDateTime.now();

            executor = Executors.newFixedThreadPool(workersCount);
            addNewTask(urlTextField.getText(), 0);

            if (timeLimit > 0) {
                try {
                    executor.awaitTermination(timeLimit, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executor.shutdown();
            } else {

                phaser.awaitAdvance(phase);
                executor.shutdownNow();
            }

            updateMetrics(startTime, parsedPages);

            return null;
        }

        @Override
        protected void done() {
            runButton.setSelected(false);
        }

        public synchronized void addNewTask(String link, int depth) {
            if (executor != null
                    && !parsedURLs.contains(link)
                    && (maxDepth == 0 || depth <= maxDepth)) {
                parsedURLs.add(link);
                phaser.register();
                executor.submit(new WebCrawlerWorker(this, new HtmlPage(link, depth)));
            }
        }


        public synchronized void addPageToResult(HtmlPage page) {
            result.add(page);
            parsedPages++;
            updateMetrics(startTime, parsedPages);
        }
    }

    public void saveResult() {
        StringBuilder stringBuilder = new StringBuilder();

        for (HtmlPage page : result) {
            stringBuilder.append(page);
        }

        File file = new File(exportUrlTextField.getText());
        try (PrintWriter printWriter = new PrintWriter(file)) {
            printWriter.write(stringBuilder.toString());
        } catch (IOException e) {
            System.out.printf("An exception occurs %s", e.getMessage());
        }
    }

    public void updateMetrics(LocalDateTime startTime, long parsed) {
        long elaplsed = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        elapsedValueLabel.setText(elaplsed / 60 + ":" + elaplsed % 60);
        parsedValueLabel.setText(Long.toString(parsed));
    }

}
