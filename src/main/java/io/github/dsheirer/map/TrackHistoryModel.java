package io.github.dsheirer.map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;

public class TrackHistoryModel {
    public static final int MAX_LOCATION_HISTORY = 10;
    private static final String[] COLUMNS = new String[]{"Time", "Latitude", "Longitude"};
    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DecimalFormat mDegreeFormat = new DecimalFormat("0.00000");
    private final ObservableList<TimestampedGeoPosition> mTimestampedGeoPositions = FXCollections.observableArrayList();

    public TrackHistoryModel() {}

    public ObservableList<TimestampedGeoPosition> getTrackHistory() {
        return mTimestampedGeoPositions;
    }

    public void add(TimestampedGeoPosition latest) {
        Platform.runLater(() -> {
            TimestampedGeoPosition mostRecentPosition = null;
            if(!mTimestampedGeoPositions.isEmpty()) {
                mostRecentPosition = mTimestampedGeoPositions.get(0);
            }
            if(isUnique(latest, mostRecentPosition)) {
                mTimestampedGeoPositions.add(0, latest);
                while(mTimestampedGeoPositions.size() > MAX_LOCATION_HISTORY) {
                    mTimestampedGeoPositions.remove(mTimestampedGeoPositions.size() - 1);
                }
            }
        });
    }

    private boolean isUnique(TimestampedGeoPosition latest, TimestampedGeoPosition previous) {
        if(latest != null && previous == null) return true;
        if(latest != null) {
            return latest.getTimestamp() > (previous.getTimestamp() + 2_000) ||
                    Math.abs(latest.getLatitude() - previous.getLatitude()) > 0.00001 ||
                    Math.abs(latest.getLongitude() - previous.getLongitude()) > 0.00001;
        }
        return false;
    }

    public TimestampedGeoPosition get(int index) {
        if(index >= 0 && index < mTimestampedGeoPositions.size()) {
            return mTimestampedGeoPositions.get(index);
        }
        return null;
    }
}
