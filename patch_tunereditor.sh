sed -i '/protected JLabel getTunerIdLabel/i \
    /** \
     * Helper to retrieve the USB Bus and Port, if available \
     */ \
    protected String getUsbInfo() \
    { \
        if(getDiscoveredTuner() instanceof io.github.dsheirer.source.tuner.manager.DiscoveredUSBTuner usbTuner) \
        { \
            return " (USB Bus: " + usbTuner.getBus() + " Port: " + usbTuner.getPortAddress() + ")"; \
        } \
        return ""; \
    } \
' ./src/main/java/io/github/dsheirer/source/tuner/ui/TunerEditor.java
