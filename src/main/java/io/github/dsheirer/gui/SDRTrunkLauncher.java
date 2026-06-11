package io.github.dsheirer.gui;

public class SDRTrunkLauncher {
    public static void main(String[] args) {
        System.setProperty("javafx.preloader", SDRTrunkPreloader.class.getName());
        SDRTrunk.main(args);
    }
}
