package io.github.dsheirer.gui.help;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebView;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class HelpViewController {

    @FXML
    private TextField searchField;

    @FXML
    private TreeView<String> navigationTree;

    @FXML
    private WebView contentWebView;

    @FXML
    public void initialize() {
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
                "<html><body style='font-family: sans-serif; padding: 20px;'>" +
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
        gettingStartedNode.getChildren().add(new TreeItem<>("Now Playing"));
        gettingStartedNode.getChildren().add(new TreeItem<>("Overview"));
        gettingStartedNode.getChildren().add(new TreeItem<>("Quickstart"));
        gettingStartedNode.getChildren().add(new TreeItem<>("Signal Flow & Routing"));
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
        advancedSystemNode.getChildren().add(new TreeItem<>("Audio Quality & Tuning"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Audio Recordings"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Diagnostics"));
        advancedSystemNode.getChildren().add(new TreeItem<>("CTCSS DCS NAC Filtering"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Gemini Ai"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Ignore Unwanted Talkgroups"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Inactivity Monitoring"));
        advancedSystemNode.getChildren().add(new TreeItem<>("Notifications"));
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
            markdown.append("* **Rdio Scanner Improvements:** Enhanced integration with Rdio Scanner to easily manage API keys and verify audio streams.\n\n");
            markdown.append("### 🎧 Audio Quality & Tuning\n");
            markdown.append("* **Analog Hiss Reduction:** New background noise and hiss reduction filters for NBFM (analog) channels.\n");
            markdown.append("* **Anti-Clipping:** Audio filters tuned by default to prevent loud transmissions from distorting.\n");
            markdown.append("* **P25 Audio Enhancements:** Specialized tuning for clearer digital audio on complex P25 systems.\n");
            markdown.append("* **Virtual Audio Cable (VAC) Routing:** Route specific radio aliases directly to Virtual Audio Cables for external application use.\n\n");
            markdown.append("### 🎛️ Advanced Filtering & Control\n");
            markdown.append("* **Ignore Unwanted Talkgroups:** Option to ignore talkgroups not saved in your alias list.\n");
            markdown.append("* **CTCSS / DCS / NAC Filtering:** Filter out unwanted analog or digital interference using specific tones and codes.\n");
            markdown.append("* **P25 NAC Override:** Advanced control for overriding and filtering P25 system NAC codes.\n\n");
            markdown.append("### 💻 User Interface Improvements\n");
            markdown.append("* **Persistent Layouts:** Channels tab remembers column widths and sorting after restarts.\n");
            markdown.append("* **Alphabetical Sorting:** Alias list can now be sorted alphabetically.\n");
            markdown.append("* **Mute/Unmute from Waterfall:** Quickly mute/unmute audio directly from the spectral waterfall display.\n");
            markdown.append("* **Radio Reference Import:** Streamlined process for importing radio system data.\n\n");
            markdown.append("### 🛠️ System & Diagnostics\n");
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
        return markdown.toString();
    }

    private void updateContent(String topic) {
        if (topic.equals("Knowledge Base") || topic.equals("Guides & Documentation")) {
            contentWebView.getEngine().loadContent(
                    "<html><body style='font-family: sans-serif; padding: 20px;'>" +
                            "<h1>" + topic + "</h1>" +
                            "<p>Select a sub-topic from the left to view details.</p></body></html>"
            );
            return;
        }

        String markdown = "# " + topic + "\n\n" + getMarkdownContent(topic);

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlBody = renderer.render(document);

        String html = "<html><body style='font-family: sans-serif; padding: 20px;'>" + htmlBody + "</body></html>";
        contentWebView.getEngine().loadContent(html);
    }
}
