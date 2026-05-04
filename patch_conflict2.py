with open("src/main/java/io/github/dsheirer/gui/LogsPanel.java", "r") as f:
    content = f.read()

conflict_block2 = """<<<<<<< HEAD
                logContent = String.join("\\n", lines);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error reading log file: " + e.getMessage());
                    resetAnalyzeButton();
                });
=======
                contentStr = String.join("\\n", lines);
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

with open("src/main/java/io/github/dsheirer/gui/LogsPanel.java", "w") as f:
    f.write(content)
