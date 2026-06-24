

package io.github.dsheirer.gui.playlist.channel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GeographicSchemaGenerator extends Dialog<String> {
    private Channel mChannel;
    private UserPreferences mUserPreferences;
    private PlaylistManager mPlaylistManager;
    private Protocol mProtocol;
    private ComboBox<State> mStateCombo;
    private ComboBox<CountyData.County> mCountyCombo;
    private ComboBox<AgencyType> mAgencyCombo;
    private TextField mPreviewField;

    public GeographicSchemaGenerator(Channel channel, UserPreferences userPreferences, PlaylistManager playlistManager) {
        this(channel, userPreferences, playlistManager, Protocol.NBFM);
    }

    public GeographicSchemaGenerator(Channel channel, UserPreferences userPreferences, PlaylistManager playlistManager, Protocol protocol) {
        mChannel = channel;
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;
        mProtocol = (protocol != null) ? protocol : Protocol.NBFM;

        setTitle("Generate " + mProtocol + " Talkgroup ID");
        setHeaderText("Select schema options to generate a sequential 10-digit Geographic ID");

        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        mStateCombo = new ComboBox<>();
        for (Map.Entry<String, String> entry : getStates().entrySet()) {
            mStateCombo.getItems().add(new State(entry.getKey(), entry.getValue()));
        }
        mStateCombo.setPrefWidth(200);
        mStateCombo.getSelectionModel().clearSelection();

        mCountyCombo = new ComboBox<>();
        mCountyCombo.setPrefWidth(200);
        mCountyCombo.setDisable(true);
        mStateCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            State s = newValue;

            mCountyCombo.getItems().clear();
            if (s == null) {
                mCountyCombo.setDisable(true);
            } else {
                mCountyCombo.setDisable(false);
                if (s.fips.equals("00")) {
                    mCountyCombo.getItems().add(new CountyData.County("000", "Nationwide"));
                } else {
                    if (mCountyCombo.getScene() != null) mCountyCombo.getScene().setCursor(Cursor.WAIT);
                    Map<String, java.util.List<CountyData.County>> counties = CountyData.getCounties();
                    if (counties.containsKey(s.fips)) {
                        mCountyCombo.getItems().addAll(counties.get(s.fips));
                    } else {
                        mCountyCombo.getItems().add(new CountyData.County("000", "Statewide"));
                    }
                    if (mCountyCombo.getScene() != null) mCountyCombo.getScene().setCursor(Cursor.DEFAULT);
                }
            }
            mCountyCombo.getSelectionModel().clearSelection();
            updatePreview();
        });

        mAgencyCombo = new ComboBox<>();
        mAgencyCombo.setDisable(true);
        mAgencyCombo.getItems().addAll(
                new AgencyType(1, "County Trunked Systems"),
                new AgencyType(2, "State Trunked Systems"),
                new AgencyType(3, "County Agencies"),
                new AgencyType(4, "State Agencies"),
                new AgencyType(5, "National Agencies")
        );
        mAgencyCombo.getSelectionModel().clearSelection();

        mPreviewField = new TextField();
        mPreviewField.setEditable(false);

        grid.add(new Label("State:"), 0, 0);
        grid.add(mStateCombo, 1, 0);
        grid.add(new Label("County (3 digits):"), 0, 1);
        grid.add(mCountyCombo, 1, 1);
        grid.add(new Label("Agency Type:"), 0, 2);
        grid.add(mAgencyCombo, 1, 2);
        grid.add(new Label("Preview:"), 0, 3);
        grid.add(mPreviewField, 1, 3);

        getDialogPane().setContent(grid);

        mCountyCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            CountyData.County c = newValue;

            if (c == null) {
                mAgencyCombo.getSelectionModel().clearSelection();
                mAgencyCombo.setDisable(true);
            } else {
                mAgencyCombo.setDisable(false);
            }
            updatePreview();
        });
        mAgencyCombo.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());

        setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                State state = mStateCombo.getValue();
                CountyData.County countyObj = mCountyCombo.getValue();
                AgencyType agency = mAgencyCombo.getValue();
                if (state != null && countyObj != null && agency != null) {
                    mChannel.setState(state.fips);
                    mChannel.setCounty(countyObj.code);
                    mChannel.setAgency(String.valueOf(agency.code));
                }
                return generateId();
            }
            return null;
        });

        autoFill();
        updatePreview();
    }

    private void autoFill() {
        boolean hasState = false;
        if (mChannel.hasState()) {
            selectState(mChannel.getState());
            hasState = true;
        } else if (mUserPreferences.getRadioReferencePreference().getPreferredStateId() > 0) {
            //The RadioReference preference stores RR's internal sequential state ID (e.g. Maine = 20), but the
            //generator's state dropdown is keyed by US FIPS codes (Maine = 23). Convert before selecting so the
            //correct state is prefilled instead of silently failing to match.
            String preferredFips = rrStateIdToFips(mUserPreferences.getRadioReferencePreference().getPreferredStateId());
            if (preferredFips != null) {
                selectState(preferredFips);
                hasState = true;
            }
        }

        if (hasState) {
            if (mChannel.hasCounty()) {
                selectCounty(String.format("%03d", Integer.parseInt(mChannel.getCounty())));
                if (mChannel.hasAgency()) {
                    selectAgency(mChannel.getAgency());
                }
            } else if (mUserPreferences.getRadioReferencePreference().getPreferredCountyId() > 0) {
                selectCounty(String.format("%03d", mUserPreferences.getRadioReferencePreference().getPreferredCountyId()));
            }
        }
    }
    private void selectCounty(String countyCode) {
        for (CountyData.County c : mCountyCombo.getItems()) {
            if (c.code.equals(countyCode)) {
                mCountyCombo.getSelectionModel().select(c);
                return;
            }
        }
    }

    private void selectAgency(String agencyCode) {
        try {
            int code = Integer.parseInt(agencyCode);
            for (AgencyType a : mAgencyCombo.getItems()) {
                if (a.code == code) {
                    mAgencyCombo.getSelectionModel().select(a);
                    return;
                }
            }
        } catch (NumberFormatException ignored) {}
    }

    private void selectState(String stateFips) {
        for (State state : mStateCombo.getItems()) {
            if (state.fips.equals(stateFips)) {
                mStateCombo.getSelectionModel().select(state);
                return;
            }
        }
    }

    private void updatePreview() {
        String id = generateId();
        if (id != null) {
            mPreviewField.setText(id);
            if (getDialogPane().getButtonTypes().size() > 0) {
                javafx.scene.Node confirmButton = getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
                if (confirmButton != null) confirmButton.setDisable(false);
            }
        } else {
            mPreviewField.setText("");
            if (getDialogPane().getButtonTypes().size() > 0) {
                javafx.scene.Node confirmButton = getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
                if (confirmButton != null) confirmButton.setDisable(true);
            }
        }
    }

    private String generateId() {
        State state = mStateCombo.getValue();
        CountyData.County countyObj = mCountyCombo.getValue();
        String county = countyObj != null ? countyObj.code : null;
        AgencyType agency = mAgencyCombo.getValue();

        if (state == null || county == null || county.length() != 3 || !county.matches("\\d{3}") || agency == null) {
            return null;
        }

        String prefix = state.fips + county + agency.code;
        return nextSequentialId(mPlaylistManager, prefix);
    }

    /**
     * Collects every talkgroup value already in use across the playlist - both alias talkgroup
     * identifiers (any protocol) and channel decode-config assigned talkgroups (NBFM and P25).
     * Values are returned as unsigned decimal strings so that 10-digit geographic IDs that exceed
     * Integer.MAX_VALUE are handled correctly.
     *
     * Scanning channel configs (not just aliases) is essential: a channel can have an assigned
     * talkgroup before any alias has been created for it, so an alias-only scan would hand out
     * duplicate sequence numbers to channels that share the same state/county/agency fields.
     */
    private static Set<String> collectUsedTalkgroupStrings(PlaylistManager playlistManager) {
        Set<String> used = new HashSet<>();

        for (Alias alias : playlistManager.getAliasModel().getAliases()) {
            for (AliasID id : alias.getAliasIdentifiers()) {
                if (id instanceof Talkgroup) {
                    used.add(Integer.toUnsignedString(((Talkgroup) id).getValue()));
                }
            }
        }

        for (Channel channel : playlistManager.getChannelModel().getChannels()) {
            DecodeConfiguration dc = channel.getDecodeConfiguration();
            if (dc instanceof DecodeConfigAnalog) {
                int value = ((DecodeConfigAnalog) dc).getTalkgroup();
                if (value != 0) {
                    used.add(Integer.toUnsignedString(value));
                }
            } else if (dc instanceof DecodeConfigP25) {
                int value = ((DecodeConfigP25) dc).getTalkgroup();
                if (value != 0) {
                    used.add(Integer.toUnsignedString(value));
                }
            }
        }

        return used;
    }

    /**
     * Returns the next available sequential 10-digit geographic ID for the given 6-digit prefix
     * (state[2] + county[3] + agency[1]), scanning all in-use talkgroups so that channels sharing
     * the same fields receive sequential numbers (e.g. ...0001, ...0002, ...0003).
     */
    public static String nextSequentialId(PlaylistManager playlistManager, String prefix) {
        int maxSeq = 0;
        for (String tgValue : collectUsedTalkgroupStrings(playlistManager)) {
            if (tgValue.length() == 10 && tgValue.startsWith(prefix)) {
                try {
                    int seq = Integer.parseInt(tgValue.substring(6));
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%04d", prefix, maxSeq + 1);
    }

    /**
     * Indicates whether the given talkgroup value (interpreted as unsigned) is already assigned to
     * an alias or channel in the playlist.
     */
    public static boolean isTalkgroupUsed(PlaylistManager playlistManager, int value) {
        return collectUsedTalkgroupStrings(playlistManager).contains(Integer.toUnsignedString(value));
    }

    /**
     * Suggests an available alternative for a conflicting talkgroup. If the conflicting value is a
     * 10-digit geographic ID, the next sequential value for its prefix is returned; otherwise the
     * next free integer above the conflicting value is returned.
     */
    public static int suggestAlternative(PlaylistManager playlistManager, int conflictingValue) {
        String s = Integer.toUnsignedString(conflictingValue);
        if (s.length() == 10) {
            String prefix = s.substring(0, 6);
            try {
                return Integer.parseUnsignedInt(nextSequentialId(playlistManager, prefix));
            } catch (NumberFormatException ignored) {}
        }

        Set<String> used = collectUsedTalkgroupStrings(playlistManager);
        int candidate = conflictingValue + 1;
        //Walk forward (unsigned) until a free value is found, capping the search to avoid spinning.
        for (int i = 0; i < 1_000_000; i++) {
            if (candidate != 0 && !used.contains(Integer.toUnsignedString(candidate))) {
                return candidate;
            }
            candidate++;
        }
        return conflictingValue;
    }

    /**
     * Converts a RadioReference internal state ID into the corresponding US FIPS state code used by this generator's
     * state dropdown. RadioReference assigns state IDs sequentially in alphabetical order (1 = Alabama, 20 = Maine,
     * ...), which does not match the FIPS numbering (Maine = 23). Returns null for unknown IDs.
     *
     * @param rrStateId RadioReference internal state ID
     * @return two-digit FIPS state code, or null if the ID is not recognized
     */
    public static String rrStateIdToFips(int rrStateId) {
        return RR_STATE_ID_TO_FIPS.get(rrStateId);
    }

    private static final Map<Integer, String> RR_STATE_ID_TO_FIPS = buildRrStateIdToFips();

    private static Map<Integer, String> buildRrStateIdToFips() {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(1, "01");  //Alabama
        m.put(2, "02");  //Alaska
        m.put(3, "04");  //Arizona
        m.put(4, "05");  //Arkansas
        m.put(5, "06");  //California
        m.put(6, "08");  //Colorado
        m.put(7, "09");  //Connecticut
        m.put(8, "10");  //Delaware
        m.put(9, "11");  //District of Columbia
        m.put(10, "12"); //Florida
        m.put(11, "13"); //Georgia
        m.put(12, "15"); //Hawaii
        m.put(13, "16"); //Idaho
        m.put(14, "17"); //Illinois
        m.put(15, "18"); //Indiana
        m.put(16, "19"); //Iowa
        m.put(17, "20"); //Kansas
        m.put(18, "21"); //Kentucky
        m.put(19, "22"); //Louisiana
        m.put(20, "23"); //Maine
        m.put(21, "24"); //Maryland
        m.put(22, "25"); //Massachusetts
        m.put(23, "26"); //Michigan
        m.put(24, "27"); //Minnesota
        m.put(25, "28"); //Mississippi
        m.put(26, "29"); //Missouri
        m.put(27, "30"); //Montana
        m.put(28, "31"); //Nebraska
        m.put(29, "32"); //Nevada
        m.put(30, "33"); //New Hampshire
        m.put(31, "34"); //New Jersey
        m.put(32, "35"); //New Mexico
        m.put(33, "36"); //New York
        m.put(34, "37"); //North Carolina
        m.put(35, "38"); //North Dakota
        m.put(36, "39"); //Ohio
        m.put(37, "40"); //Oklahoma
        m.put(38, "41"); //Oregon
        m.put(39, "42"); //Pennsylvania
        m.put(40, "44"); //Rhode Island
        m.put(41, "45"); //South Carolina
        m.put(42, "46"); //South Dakota
        m.put(43, "47"); //Tennessee
        m.put(44, "48"); //Texas
        m.put(45, "49"); //Utah
        m.put(46, "50"); //Vermont
        m.put(47, "51"); //Virginia
        m.put(48, "53"); //Washington
        m.put(49, "54"); //West Virginia
        m.put(50, "55"); //Wisconsin
        m.put(51, "56"); //Wyoming
        return m;
    }

    private static Map<String, String> getStates() {
        Map<String, String> states = new LinkedHashMap<>();
        states.put("00", "Nationwide");
        states.put("01", "Alabama");
        states.put("02", "Alaska");
        states.put("04", "Arizona");
        states.put("05", "Arkansas");
        states.put("06", "California");
        states.put("08", "Colorado");
        states.put("09", "Connecticut");
        states.put("10", "Delaware");
        states.put("11", "District of Columbia");
        states.put("12", "Florida");
        states.put("13", "Georgia");
        states.put("15", "Hawaii");
        states.put("16", "Idaho");
        states.put("17", "Illinois");
        states.put("18", "Indiana");
        states.put("19", "Iowa");
        states.put("20", "Kansas");
        states.put("21", "Kentucky");
        states.put("22", "Louisiana");
        states.put("23", "Maine");
        states.put("24", "Maryland");
        states.put("25", "Massachusetts");
        states.put("26", "Michigan");
        states.put("27", "Minnesota");
        states.put("28", "Mississippi");
        states.put("29", "Missouri");
        states.put("30", "Montana");
        states.put("31", "Nebraska");
        states.put("32", "Nevada");
        states.put("33", "New Hampshire");
        states.put("34", "New Jersey");
        states.put("35", "New Mexico");
        states.put("36", "New York");
        states.put("37", "North Carolina");
        states.put("38", "North Dakota");
        states.put("39", "Ohio");
        states.put("40", "Oklahoma");
        states.put("41", "Oregon");
        states.put("42", "Pennsylvania");
        states.put("44", "Rhode Island");
        states.put("45", "South Carolina");
        states.put("46", "South Dakota");
        states.put("47", "Tennessee");
        states.put("48", "Texas");
        states.put("49", "Utah");
        states.put("50", "Vermont");
        states.put("51", "Virginia");
        states.put("53", "Washington");
        states.put("54", "West Virginia");
        states.put("55", "Wisconsin");
        states.put("56", "Wyoming");
        return states;
    }

    private static class State {
        String fips;
        String name;
        State(String fips, String name) { this.fips = fips; this.name = name; }
        @Override public String toString() { return fips + " - " + name; }
    }

    private static class AgencyType {
        int code;
        String name;
        AgencyType(int code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return code + " - " + name; }
    }
}
