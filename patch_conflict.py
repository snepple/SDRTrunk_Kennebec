with open("src/main/java/io/github/dsheirer/gui/LogsPanel.java", "r") as f:
    content = f.read()

import re

# We need to resolve the conflicts:
# It looks like the master branch changed analyzeSelectedLog() to analyzeLog(LogFile, Button)
# And resetAnalyzeButton() to resetAnalyzeButton(Button)

conflict_block1 = """<<<<<<< HEAD
        mAnalyzeBtn.setDisable(true);
        mAnalyzeBtn.setText("Analyzing...");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Log Analysis");
        alert.setHeaderText("Analyzing log with Gemini AI...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        VBox vbox = new VBox(progressIndicator);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPrefSize(600, 400);
        alert.getDialogPane().setContent(vbox);

        // Disable the OK button while analyzing
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Prevent closing the alert while analysis is ongoing
        alert.setOnCloseRequest(event -> {
            if (okButton.isDisabled()) {
                event.consume();
            }
        });

        alert.show();

        Thread worker = new Thread(() -> {
            String logContent;
=======
        analyzeBtn.setDisable(true);
        analyzeBtn.setText("Analyzing...");

        Thread worker = new Thread(() -> {
            String contentStr;
>>>>>>> origin/master"""

resolved_block1 = """        analyzeBtn.setDisable(true);
        analyzeBtn.setText("Analyzing...");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Log Analysis");
        alert.setHeaderText("Analyzing log with Gemini AI...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        VBox vbox = new VBox(progressIndicator);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPrefSize(600, 400);
        alert.getDialogPane().setContent(vbox);

        // Disable the OK button while analyzing
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Prevent closing the alert while analysis is ongoing
        alert.setOnCloseRequest(event -> {
            if (okButton.isDisabled()) {
                event.consume();
            }
        });

        alert.show();

        Thread worker = new Thread(() -> {
            String logContent;"""

content = content.replace(conflict_block1, resolved_block1)

conflict_block2 = """<<<<<<< HEAD
                logContent = String.join("\n", lines);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error reading log file: " + e.getMessage());
                    resetAnalyzeButton();
                });
=======
                contentStr = String.join("\n", lines);
            } catch (IOException e) {
                showError("Error reading log file: " + e.getMessage());
                resetAnalyzeButton(analyzeBtn);
>>>>>>> origin/master"""

resolved_block2 = """                logContent = String.join("\\n", lines);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error reading log file: " + e.getMessage());
                    resetAnalyzeButton(analyzeBtn);
                });"""
content = content.replace(conflict_block2, resolved_block2)


conflict_block3 = """<<<<<<< HEAD
                String result = analyzer.analyze(logContent);

                Platform.runLater(() -> {
                    try {
                        Parser parser = Parser.builder().build();
                        Node document = parser.parse(result);
                        HtmlRenderer renderer = HtmlRenderer.builder().build();
                        String htmlResult = renderer.render(document);

                        WebView webView = new WebView();
                        webView.getEngine().loadContent("<html><body style='font-family: sans-serif;'>" + htmlResult + "</body></html>");
                        webView.setPrefSize(600, 400);

                        if (!alert.isShowing()) {
                            alert.show(); // Show again if closed
                        }
                        alert.setHeaderText(null);
                        alert.getDialogPane().setContent(webView);
                        alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
                    } catch (Exception ex) {
                        alert.close();
                        showError("Error formatting AI response:\\n" + ex.getMessage());
                    } finally {
                        resetAnalyzeButton();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error analyzing log:\\n" + ex.getMessage());
                    resetAnalyzeButton();
                });
=======
                String result = analyzer.analyze(contentStr);

                Platform.runLater(() -> {
                    TextArea textArea = new TextArea(result);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setPrefSize(600, 400);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("AI Log Analysis");
                    alert.setHeaderText(null);
                    alert.getDialogPane().setContent(textArea);
                    alert.showAndWait();

                    resetAnalyzeButton(analyzeBtn);
                });
            } catch (Exception ex) {
                showError("Error analyzing log:\\n" + ex.getMessage());
                resetAnalyzeButton(analyzeBtn);
>>>>>>> origin/master"""

resolved_block3 = """                String result = analyzer.analyze(logContent);

                Platform.runLater(() -> {
                    try {
                        Parser parser = Parser.builder().build();
                        Node document = parser.parse(result);
                        HtmlRenderer renderer = HtmlRenderer.builder().build();
                        String htmlResult = renderer.render(document);

                        WebView webView = new WebView();
                        webView.getEngine().loadContent("<html><body style='font-family: sans-serif;'>" + htmlResult + "</body></html>");
                        webView.setPrefSize(600, 400);

                        if (!alert.isShowing()) {
                            alert.show(); // Show again if closed
                        }
                        alert.setHeaderText(null);
                        alert.getDialogPane().setContent(webView);
                        alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
                    } catch (Exception ex) {
                        alert.close();
                        showError("Error formatting AI response:\\n" + ex.getMessage());
                    } finally {
                        resetAnalyzeButton(analyzeBtn);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error analyzing log:\\n" + ex.getMessage());
                    resetAnalyzeButton(analyzeBtn);
                });"""
content = content.replace(conflict_block3, resolved_block3)

conflict_block4 = """<<<<<<< HEAD
    private void resetAnalyzeButton() {
=======
    private void resetAnalyzeButton(Button analyzeBtn) {
>>>>>>> origin/master"""

resolved_block4 = """    private void resetAnalyzeButton(Button analyzeBtn) {"""
content = content.replace(conflict_block4, resolved_block4)


with open("src/main/java/io/github/dsheirer/gui/LogsPanel.java", "w") as f:
    f.write(content)
