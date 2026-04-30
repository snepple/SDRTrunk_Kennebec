package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.gui.playlist.PlaylistEditorRequest;

public abstract class RadioReferenceTabRequest extends PlaylistEditorRequest {
    @Override
    public TabName getTabName() {
        return TabName.RADIOREFERENCE;
    }
}
