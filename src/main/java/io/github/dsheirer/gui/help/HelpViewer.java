package io.github.dsheirer.gui.help;

import net.miginfocom.swing.MigLayout;
import com.jidesoft.swing.JideSplitPane;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class HelpViewer extends JPanel {
    private JideSplitPane splitPane;

    private JTree navigationTree;
    private JEditorPane contentPane;
    private JTextField searchField;

    public HelpViewer() {



        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        splitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);

        // Left side: Navigation and Search
        JPanel leftPanel = new JPanel(new MigLayout("insets 5, fill", "[grow,fill]", "[]5[grow,fill]"));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search Help...");

        searchField.addActionListener(e -> {
            String query = searchField.getText().toLowerCase();
            if(query.isEmpty()) return;

            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) navigationTree.getModel().getRoot();
            DefaultMutableTreeNode foundNode = searchNode(rootNode, query);
            if(foundNode != null) {
                javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(foundNode.getPath());
                navigationTree.setSelectionPath(path);
                navigationTree.scrollPathToVisible(path);
            }
        });

        leftPanel.add(searchField, "wrap");

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Knowledge Base");
        createNodes(root);
        navigationTree = new JTree(new DefaultTreeModel(root));
        navigationTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) navigationTree.getLastSelectedPathComponent();
            if (node == null) return;
            updateContent(node.getUserObject().toString());
        });

        JScrollPane treeScrollPane = new JScrollPane(navigationTree);
        leftPanel.add(treeScrollPane);

        // Right side: Content Renderer
        contentPane = new JEditorPane();
        contentPane.setEditable(false);
        contentPane.setContentType("text/html");
        contentPane.setText("<html><body style='font-family: sans-serif; padding: 20px;'>" +
                "<h1>Welcome to SDRTrunk Help</h1>" +
                "<p>Select a topic from the left to view details.</p></body></html>");
        JScrollPane contentScrollPane = new JScrollPane(contentPane);

        splitPane.add(leftPanel);
        splitPane.add(contentScrollPane);
        splitPane.setProportionalLayout(true);
        splitPane.setProportions(new double[]{0.3});

        add(splitPane, BorderLayout.CENTER);
    }

    private DefaultMutableTreeNode searchNode(DefaultMutableTreeNode node, String query) {
        if(node.getUserObject().toString().toLowerCase().contains(query)) {
            return node;
        }
        for(int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode found = searchNode(child, query);
            if(found != null) return found;
        }
        return null;
    }


    private void createNodes(DefaultMutableTreeNode root) {
        root.add(new DefaultMutableTreeNode("What's New"));

        DefaultMutableTreeNode guidesNode = new DefaultMutableTreeNode("Guides & Documentation");
        guidesNode.add(new DefaultMutableTreeNode("Airspy Hackrf"));
        guidesNode.add(new DefaultMutableTreeNode("Aliases Talkgroups"));
        guidesNode.add(new DefaultMutableTreeNode("Analog"));
        guidesNode.add(new DefaultMutableTreeNode("Audio Recordings"));
        guidesNode.add(new DefaultMutableTreeNode("Broadcastify"));
        guidesNode.add(new DefaultMutableTreeNode("Diagnostics"));
        guidesNode.add(new DefaultMutableTreeNode("Dmr"));
        guidesNode.add(new DefaultMutableTreeNode("Gemini Ai"));
        guidesNode.add(new DefaultMutableTreeNode("Inactivity Monitoring"));
        guidesNode.add(new DefaultMutableTreeNode("Introduction"));
        guidesNode.add(new DefaultMutableTreeNode("Mqtt"));
        guidesNode.add(new DefaultMutableTreeNode("Notifications"));
        guidesNode.add(new DefaultMutableTreeNode("Other Platforms"));
        guidesNode.add(new DefaultMutableTreeNode("Overview"));
        guidesNode.add(new DefaultMutableTreeNode("P25"));
        guidesNode.add(new DefaultMutableTreeNode("Playlist Editor"));
        guidesNode.add(new DefaultMutableTreeNode("Quickstart"));
        guidesNode.add(new DefaultMutableTreeNode("Rtl Sdr"));
        guidesNode.add(new DefaultMutableTreeNode("Supported Tuners"));
        guidesNode.add(new DefaultMutableTreeNode("System Requirements"));
        guidesNode.add(new DefaultMutableTreeNode("Tuner Self Healing"));
        guidesNode.add(new DefaultMutableTreeNode("Two Tone Detect"));
        guidesNode.add(new DefaultMutableTreeNode("User Preferences"));
        guidesNode.add(new DefaultMutableTreeNode("Zello"));
        root.add(guidesNode);
    }






    private void updateContent(String topic) {
        String markdown = "# " + topic + "\n\n";

        if (topic.equals("What's New")) {
            markdown += "This edition is a specialized Dispatch & Efficiency fork of the original SDRTrunk software. It includes features and enhancements designed specifically for automated dispatch and operator efficiency.\n\n";
            markdown += "### 📡 Streaming & Integrations\n";
            markdown += "* **Zello Streaming:** Directly stream dispatch audio to Zello with automatic reconnection and detailed troubleshooting logs.\n";
            markdown += "* **ThinLine Radio Support:** Built-in streaming directly to ThinLine Radio with out-of-the-box debug logging.\n";
            markdown += "* **Rdio Scanner Improvements:** Enhanced integration with Rdio Scanner to easily manage API keys and verify audio streams.\n\n";
            markdown += "### 🎧 Audio Quality & Tuning\n";
            markdown += "* **Analog Hiss Reduction:** New background noise and hiss reduction filters for NBFM (analog) channels.\n";
            markdown += "* **Anti-Clipping:** Audio filters tuned by default to prevent loud transmissions from distorting.\n";
            markdown += "* **P25 Audio Enhancements:** Specialized tuning for clearer digital audio on complex P25 systems.\n";
            markdown += "* **Virtual Audio Cable (VAC) Routing:** Route specific radio aliases directly to Virtual Audio Cables for external application use.\n\n";
            markdown += "### 🎛️ Advanced Filtering & Control\n";
            markdown += "* **Ignore Unwanted Talkgroups:** Option to ignore talkgroups not saved in your alias list.\n";
            markdown += "* **CTCSS / DCS / NAC Filtering:** Filter out unwanted analog or digital interference using specific tones and codes.\n";
            markdown += "* **P25 NAC Override:** Advanced control for overriding and filtering P25 system NAC codes.\n\n";
            markdown += "### 💻 User Interface Improvements\n";
            markdown += "* **Persistent Layouts:** Channels tab remembers column widths and sorting after restarts.\n";
            markdown += "* **Alphabetical Sorting:** Alias list can now be sorted alphabetically.\n";
            markdown += "* **Mute/Unmute from Waterfall:** Quickly mute/unmute audio directly from the spectral waterfall display.\n";
            markdown += "* **Radio Reference Import:** Streamlined process for importing radio system data.\n\n";
            markdown += "### 🛠️ System & Diagnostics\n";
            markdown += "* **Live Diagnostics Panel:** New \"Diagnostics (Logging)\" settings panel to toggle detailed troubleshooting logs on the fly.\n";
            markdown += "* **Hardware & Decoding Fixes:** Various stability fixes for DMR decoding and SDR hardware tuning.\n";
        } else {
            String fileName = topic.toLowerCase().replace(" ", "-") + ".md";
            try (java.io.InputStream is = getClass().getResourceAsStream("/docs/" + fileName)) {
                if (is != null) {
                    try (java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A")) {
                        markdown = s.hasNext() ? s.next() : "";
                    }
                } else {
                    markdown += "Detailed documentation for this topic could not be found or is being migrated.";
                }
            } catch (java.io.IOException e) {
                markdown += "Error loading documentation: " + e.getMessage();
            }
        }

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlBody = renderer.render(document);

        String html = "<html><body style='font-family: sans-serif; padding: 20px;'>" + htmlBody + "</body></html>";
        contentPane.setText(html);
        contentPane.setCaretPosition(0);
    }
}
