
package io.github.dsheirer.gui.help;
import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebView;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import java.util.List;

public class HelpViewController {

    @FXML
    private TextField searchField;

    @FXML
    private TreeView<String> navigationTree;

    @FXML
    private WebView contentWebView;

    @FXML
    public void initialize() {
        searchField.getStyleClass().add("kennebec-search-field");
        TreeItem<String> root = new TreeItem<>("Knowledge Base");
        createNodes(root);
        root.setExpanded(true);
        navigationTree.setRoot(root);
        navigationTree.setShowRoot(true);

        navigationTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateContent(newValue.getValue());
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue.toLowerCase();
            if (query.isEmpty()) {
                return;
            }
            TreeItem<String> foundNode = searchNode(root, query);
            if (foundNode != null) {
                navigationTree.getSelectionModel().select(foundNode);
                int row = navigationTree.getRow(foundNode);
                navigationTree.scrollTo(row);
            }
        });

        contentWebView.getEngine().loadContent(
                "<html><head>" + getCssStyle() + "</head><body>" +
                        "<h1>Welcome to SDRTrunk Help</h1>" +
                        "<p>Select a topic from the left to view details.</p></body></html>"
        );
    }

    private void createNodes(TreeItem<String> root) {
        root.getChildren().add(new TreeItem<>("What's New"));

        TreeItem<String> guidesNode = new TreeItem<>("Guides & Documentation");
        guidesNode.setExpanded(true);

        TreeItem<String> gettingStartedNode = new TreeItem<>("Getting Started");
        gettingStartedNode.getChildren().add(new TreeItem<>("Index"));
        gettingStartedNode.getChildren().add(new TreeItem<>("Introduction"));
        gettingStartedNode.getChildren().add(new TreeItem<>("Overview"));
        gettingStartedNode.getChildren().add(new TreeItem<>("Quickstart"));
        guidesNode.getChildren().add(gettingStartedNode);

        TreeItem<String> hardwareTunersNode = new TreeItem<>("Hardware & Tuners");
        hardwareTunersNode.getChildren().add(new TreeItem<>("Airspy Hackrf"));
        hardwareTunersNode.getChildren().add(new TreeItem<>("Rtl Sdr"));
        hardwareTunersNode.getChildren().add(new TreeItem<>("Spectrum & Waterfall"));
        hardwareTunersNode.getChildren().add(new TreeItem<>("Supported Tuners"));
        hardwareTunersNode.getChildren().add(new TreeItem<>("Tuner Self Healing"));
        guidesNode.getChildren().add(hardwareTunersNode);

        TreeItem<String> channelsDecodingNode = new TreeItem<>("Channels & Decoding");
        channelsDecodingNode.getChildren().add(new TreeItem<>("Analog"));
        channelsDecodingNode.getChildren().add(new TreeItem<>("Dmr"));
        channelsDecodingNode.getChildren().add(new TreeItem<>("P25"));
        channelsDecodingNode.getChildren().add(new TreeItem<>("Channel Images"));
        guidesNode.getChildren().add(channelsDecodingNode);

        TreeItem<String> organizationPlaylistsNode = new TreeItem<>("Organization & Playlists");
        organizationPlaylistsNode.getChildren().add(new TreeItem<>("Aliases Talkgroups"));
        organizationPlaylistsNode.getChildren().add(new TreeItem<>("Playlist Editor"));
        organizationPlaylistsNode.getChildren().add(new TreeItem<>("Radio Reference"));
        guidesNode.getChildren().add(organizationPlaylistsNode);

        TreeItem<String> integrationsStreamingNode = new TreeItem<>("Integrations & Streaming");
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Broadcastify"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Mqtt"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Openmhz"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Other Platforms"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Rdio Scanner"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Thinline Radio"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Two Tone Detect"));
        integrationsStreamingNode.getChildren().add(new TreeItem<>("Zello"));
        guidesNode.getChildren().add(integrationsStreamingNode);

        TreeItem<String> advancedSystemNode = new TreeItem<>("Advanced & System");
        advancedSystemNode.getChildren().add(new TreeItem<>("Audio Recordings"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Diagnostics"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Gemini Ai"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Ignore Unwanted Talkgroups"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Inactivity Monitoring"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Notifications"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Smart Bandwidth"));
        advancedSystemNode.getChildren().add(new TreeItem<>("System Requirements"));
        advancedSystemNode.getChildren().add(new TreeItem<>("User Preferences"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Virtual Audio Cable"));
        guidesNode.getChildren().add(advancedSystemNode);

        root.getChildren().add(guidesNode);
    }

    private TreeItem<String> searchNode(TreeItem<String> node, String query) {
        String topic = node.getValue();
        if (topic.toLowerCase().contains(query) || getMarkdownContent(topic).toLowerCase().contains(query)) {
            expandParents(node);
            return node;
        }
        for (TreeItem<String> child : node.getChildren()) {
            TreeItem<String> found = searchNode(child, query);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void expandParents(TreeItem<?> item) {
        if (item != null && item.getParent() != null) {
            item.getParent().setExpanded(true);
            expandParents(item.getParent());
        }
    }

    private String getMarkdownContent(String topic) {
        StringBuilder markdown = new StringBuilder();
        if (topic.equals("What's New")) {
            markdown.append("This edition is a specialized Dispatch & Efficiency fork of the original SDRTrunk software. It includes features and enhancements designed specifically for automated dispatch and operator efficiency.\n\n");
            markdown.append("### 📡 Streaming & Integrations\n");
            markdown.append("* **Zello Streaming:** Directly stream dispatch audio to Zello with automatic reconnection and detailed troubleshooting logs.\n");
            markdown.append("* **ThinLine Radio Support:** Built-in streaming directly to ThinLine Radio with out-of-the-box debug logging.\n");
            markdown.append("* **Rdio Scanner Improvements:** Enhanced integration with Rdio Scanner to easily manage API keys and verify audio streams.\n");
            markdown.append("* **IAmResponding:** Real-time UDP streaming feature specifically for the IAmResponding platform.\n");
            markdown.append("* **MQTT Auto-Discovery:** MQTT publishing for Two-Tone Detectors with Home Assistant Auto-Discovery.\n\n");
            markdown.append("### 🎧 Audio Quality & Tuning\n");
            markdown.append("* **Analog Hiss Reduction:** New background noise and hiss reduction filters for NBFM (analog) channels.\n");
            markdown.append("* **NBFM AI Audio Optimizer:** AI-driven noise filtering for analog channels.\n");
            markdown.append("* **Anti-Clipping:** Audio filters tuned by default to prevent loud transmissions from distorting.\n");
            markdown.append("* **P25 Audio Enhancements:** Specialized tuning for clearer digital audio on complex P25 systems.\n");
            markdown.append("* **Virtual Audio Cable (VAC) Routing:** Route specific radio aliases directly to Virtual Audio Cables for external application use.\n\n");
            markdown.append("### 🎛️ Advanced Filtering & Control\n");
            markdown.append("* **Smart Bandwidth:** Auto-optimizing sample rate to save CPU.\n");
            markdown.append("* **Native Sequential Paging:** Phase 4 Native Sequential Paging Detection.\n");
            markdown.append("* **Ignore Unwanted Talkgroups:** Option to ignore talkgroups not saved in your alias list.\n");
            markdown.append("* **CTCSS / DCS / NAC Filtering:** Filter out unwanted analog or digital interference using specific tones and codes.\n");
            markdown.append("* **P25 NAC Override:** Advanced control for overriding and filtering P25 system NAC codes.\n\n");
            markdown.append("### 💻 User Interface Improvements\n");
            markdown.append("* **macOS Aesthetic Redesign:** A major macOS System Settings-style UI redesign, including a custom collapsible left-hand sidebar and high-dpi SVG icons.\n");
            markdown.append("* **Contextual Help Viewer:** An embedded, searchable documentation viewer right inside the app.\n");
            markdown.append("* **Persistent Layouts:** Channels tab remembers column widths and sorting after restarts.\n");
            markdown.append("* **Alphabetical Sorting:** Alias list can now be sorted alphabetically.\n");
            markdown.append("* **Mute/Unmute from Waterfall:** Quickly mute/unmute audio directly from the spectral waterfall display.\n");
            markdown.append("* **Radio Reference Import:** Streamlined process for importing radio system data.\n\n");
            markdown.append("### 🛠️ System & Diagnostics\n");
            markdown.append("* **Audio Transcriptions:** Support for transcribing radio audio using OpenAI Whisper and Google Speech-to-Text APIs.\n");
            markdown.append("* **Live Diagnostics Panel:** New \"Diagnostics (Logging)\" settings panel to toggle detailed troubleshooting logs on the fly.\n");
            markdown.append("* **Hardware & Decoding Fixes:** Various stability fixes for DMR decoding and SDR hardware tuning.\n");
        } else {
            String fileName = topic.toLowerCase().replace(" & ", "-&-").replace(" ", "-") + ".md";
            try (java.io.InputStream is = getClass().getResourceAsStream("/docs/" + fileName)) {
                if (is != null) {
                    try (java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A")) {
                        markdown.append(s.hasNext() ? s.next() : "");
                    }
                } else {
                    markdown.append("Detailed documentation for this topic could not be found or is being migrated.");
                }
            } catch (java.io.IOException e) {
                markdown.append("Error loading documentation: ").append(e.getMessage());
            }
        }
        return sanitizeMarkdown(markdown.toString());
    }

    /**
     * Strips JSX/Mintlify components and converts them to clean standard markdown.
     */
    private String sanitizeMarkdown(String markdown) {
        // Remove JSX component tags: <Card ...>, </Card>, <Tabs>, </Tabs>, <Tab title="...">, </Tab>,
        // <Steps>, </Steps>, <Step title="...">, </Step>, <Accordion>, </Accordion>
        // Extract the title attribute from Tab/Step tags and convert to heading
        markdown = markdown.replaceAll("(?s)<Card[^>]*>", "");
        markdown = markdown.replaceAll("</Card>", "");
        markdown = markdown.replaceAll("<Tabs>", "");
        markdown = markdown.replaceAll("</Tabs>", "");
        markdown = markdown.replaceAll("<Tab\\s+title=\"([^\"]*)\">", "\n**$1:**\n");
        markdown = markdown.replaceAll("</Tab>", "\n---\n");
        markdown = markdown.replaceAll("<Steps>", "");
        markdown = markdown.replaceAll("</Steps>", "");
        markdown = markdown.replaceAll("<Step\\s+title=\"([^\"]*)\">", "\n#### $1\n");
        markdown = markdown.replaceAll("</Step>", "");
        markdown = markdown.replaceAll("<Accordion[^>]*>", "");
        markdown = markdown.replaceAll("</Accordion>", "");

        // Remove JSX-style HTML tags with className attributes
        markdown = markdown.replaceAll("<h[1-6]\\s+className=\"[^\"]*\">", "");
        markdown = markdown.replaceAll("</h[1-6]>", "");
        markdown = markdown.replaceAll("<p\\s+className=\"[^\"]*\">", "\n");
        markdown = markdown.replaceAll("</p>", "\n");
        markdown = markdown.replaceAll("<div\\s+className=\"[^\"]*\">", "");
        markdown = markdown.replaceAll("</div>", "");

        // Remove mermaid code blocks - render as plaintext description
        markdown = markdown.replaceAll("(?s)```mermaid\\s*\\n(.*?)```", "*(Diagram omitted — see source documentation)*");

        // Clean up excessive blank lines
        markdown = markdown.replaceAll("\n{4,}", "\n\n\n");

        return markdown;
    }

    private String getCssStyle() {
        boolean dark = io.github.dsheirer.gui.theme.ThemeManager.isNightModeEnabled();
        if (dark) {
            return "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif; " +
            "  padding: 24px 32px; margin: 0; color: #e0e0e0; background: #1a1a2e; line-height: 1.7; font-size: 14px; }" +
            "h1 { color: #ffffff; font-size: 24px; font-weight: 700; margin-top: 0; margin-bottom: 16px; " +
            "  padding-bottom: 12px; border-bottom: 2px solid #3a3a5a; }" +
            "h2 { color: #e0e0e0; font-size: 20px; font-weight: 600; margin-top: 28px; margin-bottom: 12px; }" +
            "h3 { color: #d0d0d0; font-size: 16px; font-weight: 600; margin-top: 24px; margin-bottom: 8px; }" +
            "h4 { color: #c0c0c0; font-size: 14px; font-weight: 600; margin-top: 20px; margin-bottom: 6px; }" +
            "p { margin: 8px 0; }" +
            "a { color: #4a90e2; text-decoration: none; }" +
            "a:hover { text-decoration: underline; }" +
            "ul, ol { padding-left: 24px; margin: 8px 0; }" +
            "li { margin: 4px 0; }" +
            "code { background: #2a2a4a; padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: 'Consolas', 'Monaco', monospace; }" +
            "pre { background: #16213e; color: #e0e0e0; padding: 16px; border-radius: 8px; overflow-x: auto; " +
            "  font-size: 13px; line-height: 1.5; margin: 12px 0; border: 1px solid #3a3a5a; }" +
            "pre code { background: none; padding: 0; color: inherit; }" +
            "blockquote { margin: 16px 0; padding: 12px 16px; border-left: 4px solid #4a90e2; " +
            "  background: #1e1e3a; border-radius: 0 6px 6px 0; }" +
            "blockquote p { margin: 4px 0; }" +
            "blockquote strong { color: #4a90e2; }" +
            "table { border-collapse: collapse; width: 100%; margin: 16px 0; }" +
            "th { background: #16213e; color: #e0e0e0; font-weight: 600; text-align: left; padding: 10px 14px; " +
            "  border: 1px solid #3a3a5a; font-size: 13px; }" +
            "td { padding: 8px 14px; border: 1px solid #3a3a5a; font-size: 13px; }" +
            "tr:nth-child(even) { background: #1e1e3a; }" +
            "hr { border: none; border-top: 1px solid #3a3a5a; margin: 20px 0; }" +
            "img { max-width: 100%; border-radius: 6px; margin: 8px 0; }" +
            "</style>";
        } else {
            return "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif; " +
            "  padding: 24px 32px; margin: 0; color: #1a1a2e; background: #fafbfc; line-height: 1.7; font-size: 14px; }" +
            "h1 { color: #1a1a2e; font-size: 24px; font-weight: 700; margin-top: 0; margin-bottom: 16px; " +
            "  padding-bottom: 12px; border-bottom: 2px solid #e8e8f0; }" +
            "h2 { color: #2d2d4e; font-size: 20px; font-weight: 600; margin-top: 28px; margin-bottom: 12px; }" +
            "h3 { color: #3d3d6e; font-size: 16px; font-weight: 600; margin-top: 24px; margin-bottom: 8px; }" +
            "h4 { color: #4d4d7e; font-size: 14px; font-weight: 600; margin-top: 20px; margin-bottom: 6px; }" +
            "p { margin: 8px 0; }" +
            "a { color: #4a6fa5; text-decoration: none; }" +
            "a:hover { text-decoration: underline; }" +
            "ul, ol { padding-left: 24px; margin: 8px 0; }" +
            "li { margin: 4px 0; }" +
            "code { background: #e8e8f0; padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: 'Consolas', 'Monaco', monospace; }" +
            "pre { background: #1e1e2e; color: #cdd6f4; padding: 16px; border-radius: 8px; overflow-x: auto; " +
            "  font-size: 13px; line-height: 1.5; margin: 12px 0; }" +
            "pre code { background: none; padding: 0; color: inherit; }" +
            "blockquote { margin: 16px 0; padding: 12px 16px; border-left: 4px solid #4a6fa5; " +
            "  background: #eef2f8; border-radius: 0 6px 6px 0; }" +
            "blockquote p { margin: 4px 0; }" +
            "blockquote strong { color: #2d4a7a; }" +
            "table { border-collapse: collapse; width: 100%; margin: 16px 0; }" +
            "th { background: #eef2f8; font-weight: 600; text-align: left; padding: 10px 14px; " +
            "  border: 1px solid #d0d5e0; font-size: 13px; }" +
            "td { padding: 8px 14px; border: 1px solid #d0d5e0; font-size: 13px; }" +
            "tr:nth-child(even) { background: #f5f7fa; }" +
            "hr { border: none; border-top: 1px solid #e0e0e8; margin: 20px 0; }" +
            "img { max-width: 100%; border-radius: 6px; margin: 8px 0; }" +
            "</style>";
        }
    }

    private void updateContent(String topic) {
        if (topic.equals("Knowledge Base") || topic.equals("Guides & Documentation")) {
            contentWebView.getEngine().loadContent(
                    "<html><head>" + getCssStyle() + "</head><body>" +
                            "<h1>" + topic + "</h1>" +
                            "<p>Select a sub-topic from the left to view details.</p></body></html>"
            );
            return;
        }

        String markdown = "# " + topic + "\n\n" + getMarkdownContent(topic);

        Parser parser = Parser.builder().extensions(List.of(TablesExtension.create())).build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(List.of(TablesExtension.create())).build();
        String htmlBody = renderer.render(document);

        String html = "<html><head>" + getCssStyle() + "</head><body>" + htmlBody + "</body></html>";
        contentWebView.getEngine().loadContent(html);
    }
}
