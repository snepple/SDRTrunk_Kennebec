package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import io.github.dsheirer.controller.ControllerPanel;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.audio.broadcast.BroadcastStatusPanel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.playlist.PlaylistEditorRequest;
import io.github.dsheirer.gui.playlist.alias.AliasTabRequest;
import io.github.dsheirer.gui.playlist.channel.ChannelTabRequest;
import io.github.dsheirer.gui.playlist.radioreference.ViewRadioReferenceRequest;
import io.github.dsheirer.gui.playlist.streaming.StreamTabRequest;
import io.github.dsheirer.gui.playlist.twotone.TwoToneTabRequest;

public class ViewRouter implements SidebarPanel.SidebarListener {
    private final SDRTrunk app;

    public ViewRouter(SDRTrunk app) {
        this.app = app;
    }

    @Override
    public void onItemSelected(String id) {
        if (id == null) return;
        
        if (id.equals("exit")) {
            app.onItemSelected(id);
            return;
        }

        if (id.startsWith("playlist_")) {
            if (id.equals("playlist_playlists")) {
                MyEventBus.getGlobalEventBus().post(new ViewPlaylistRequest());
            } else if (id.equals("playlist_channels")) {
                MyEventBus.getGlobalEventBus().post(new ChannelTabRequest() {
                    @Override
                    public PlaylistEditorRequest.TabName getTabName() { return PlaylistEditorRequest.TabName.CHANNEL; }
                });
            } else if (id.equals("playlist_aliases")) {
                MyEventBus.getGlobalEventBus().post(new AliasTabRequest() {
                    @Override
                    public PlaylistEditorRequest.TabName getTabName() { return PlaylistEditorRequest.TabName.ALIAS; }
                });
            } else if (id.equals("playlist_streaming")) {
                MyEventBus.getGlobalEventBus().post(new StreamTabRequest() {
                    @Override
                    public PlaylistEditorRequest.TabName getTabName() { return PlaylistEditorRequest.TabName.STREAM; }
                });
            } else if (id.equals("playlist_radioreference")) {
                MyEventBus.getGlobalEventBus().post(new ViewRadioReferenceRequest());
            } else if (id.equals("playlist_twotones")) {
                MyEventBus.getGlobalEventBus().post(new TwoToneTabRequest() {
                    @Override
                    public PlaylistEditorRequest.TabName getTabName() { return PlaylistEditorRequest.TabName.TWO_TONE; }
                });
            }
        }
        app.onViewChanged(id);
    }

    @Override
    public void onActionRequested(String actionId) {
        app.onActionRequested(actionId);
    }
}
