import re

file_path = "C:/Users/default.LAPTOP-U0KBII5M/SDRTrunk_Kennebec_Build/src/main/java/io/github/dsheirer/gui/playlist/channel/NBFMConfigurationEditor.java"

with open(file_path, "r") as f:
    code = f.read()

# 1. getAudioFiltersPane
code = code.replace(
"""            // 1. Low-pass filter (primary/most-used control - always visible)
            contentBox.getChildren().add(createLowPassSection());
            contentBox.getChildren().add(new Separator());

            // Progressive Disclosure: Advanced Settings
            // Collapsed by default to reduce cognitive load for new users.
            // The most useful control (Low-Pass) is always visible above.
            VBox advancedContent = new VBox(8);
            advancedContent.setVisible(false);
            advancedContent.setManaged(false);

            // 2. Hiss Reduction
            advancedContent.getChildren().add(createHissReductionSection());
            advancedContent.getChildren().add(new Separator());

            // 3. Bass Boost
            advancedContent.getChildren().add(createBassBoostSection());
            advancedContent.getChildren().add(new Separator());

            // 4. Voice Enhancement
            advancedContent.getChildren().add(createVoiceEnhanceSection());
            advancedContent.getChildren().add(new Separator());

            // 5. Squelch Tail
            advancedContent.getChildren().add(createSquelchTailSection());
            advancedContent.getChildren().add(new Separator());

            // 6. Intelligent Squelch
            advancedContent.getChildren().add(createSquelchSection());
            advancedContent.getChildren().add(new Separator());

            // 7. Output Gain
            advancedContent.getChildren().add(createInputGainSection());

            // Toggle button
            Button advancedToggle = new Button("\\u25B6  Show Advanced Filters");
            advancedToggle.getStyleClass().add("flat-button");
            advancedToggle.setStyle("-fx-font-size: 12px; -fx-text-fill: #4f8ef7; -fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: transparent;");
            advancedToggle.setOnAction(e -> {
                boolean isVisible = advancedContent.isVisible();
                advancedContent.setVisible(!isVisible);
                advancedContent.setManaged(!isVisible);
                advancedToggle.setText(isVisible ? "\\u25B6  Show Advanced Filters" : "\\u25BC  Hide Advanced Filters");
            });

            contentBox.getChildren().addAll(advancedToggle, advancedContent);
""",
"""            // Filters are now cleanly laid out in SettingsCards
            contentBox.getChildren().add(createLowPassSection());

            // Progressive Disclosure: Advanced Settings
            VBox advancedContent = new VBox(10);
            advancedContent.setVisible(false);
            advancedContent.setManaged(false);

            advancedContent.getChildren().add(createHissReductionSection());
            advancedContent.getChildren().add(createBassBoostSection());
            advancedContent.getChildren().add(createVoiceEnhanceSection());
            advancedContent.getChildren().add(createSquelchTailSection());
            advancedContent.getChildren().add(createSquelchSection());
            advancedContent.getChildren().add(createInputGainSection());

            // Toggle button
            Button advancedToggle = new Button("\\u25B6  Show Advanced Filters");
            advancedToggle.getStyleClass().add("flat-button");
            advancedToggle.setStyle("-fx-font-size: 12px; -fx-text-fill: #4f8ef7; -fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: transparent;");
            advancedToggle.setOnAction(e -> {
                boolean isVisible = advancedContent.isVisible();
                advancedContent.setVisible(!isVisible);
                advancedContent.setManaged(!isVisible);
                advancedToggle.setText(isVisible ? "\\u25B6  Show Advanced Filters" : "\\u25BC  Hide Advanced Filters");
            });

            contentBox.getChildren().addAll(advancedToggle, advancedContent);
"""
)

# Replace all create...Section to return SettingsCard instead of VBox
# 1. Input Gain
code = code.replace(
"""    private VBox createInputGainSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("7. Output Gain (Applied Last)");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Adjusts the final audio volume level after all filters have been applied. Use this to compensate for volume lost due to filtering or to boost quiet signals."));

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        Label gainLabel = new Label("Gain:");
        GridPane.setConstraints(gainLabel, 0, 0);
        controlsPane.getChildren().add(gainLabel);

        mInputGainSlider = new Slider(0.1, 5.0, 2.0);
        mInputGainSlider.setMajorTickUnit(1.0);
        mInputGainSlider.setMinorTickCount(4);
        mInputGainSlider.setShowTickMarks(true);
        mInputGainSlider.setShowTickLabels(true);
        mInputGainSlider.setPrefWidth(300);
        mInputGainSlider.setTooltip(new Tooltip("Output gain applied after all filters\\n1.0 = unity, 2.0 = +6dB (default)"));
        mInputGainSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mInputGainField.setText(String.format("%.1fx (%.1f dB)", val.floatValue(),
                    20.0 * Math.log10(val.doubleValue())));
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mInputGainSlider, 1, 0);
        controlsPane.getChildren().add(mInputGainSlider);

        mInputGainField = new TextField("2.0x (6.0 dB)");
        mInputGainField.setPrefWidth(120);
        mInputGainField.setMaxWidth(120);
        mInputGainField.setStyle("-fx-font-size: 11px;");
        mInputGainField.setOnAction(event -> {
            commitTextFieldToSlider(mInputGainField, mInputGainSlider, "x", "%.1f");
            // Re-format with dB display
            double v = (int) mInputGainSlider.getValue();
            mInputGainField.setText(String.format("%.1fx (%.1f dB)", v, 20.0 * Math.log10(v)));
        });
        mInputGainField.focusedProperty().addListener((obs2, wasFocused, isFocused) -> {
            if(!isFocused)
            {
                commitTextFieldToSlider(mInputGainField, mInputGainSlider, "x", "%.1f");
                double v = (int) mInputGainSlider.getValue();
                mInputGainField.setText(String.format("%.1fx (%.1f dB)", v, 20.0 * Math.log10(v)));
            }
        });
        GridPane.setConstraints(mInputGainField, 2, 0);
        controlsPane.getChildren().add(mInputGainField);

        section.getChildren().addAll(titleBox, controlsPane);
        return section;
    }""",
"""    private SettingsCard createInputGainSection()
    {
        SettingsCard section = new SettingsCard();

        mInputGainSlider = new Slider(0.1, 5.0, 2.0);
        mInputGainSlider.setMajorTickUnit(1.0);
        mInputGainSlider.setMinorTickCount(4);
        mInputGainSlider.setShowTickMarks(true);
        mInputGainSlider.setShowTickLabels(true);
        mInputGainSlider.setPrefWidth(300);
        mInputGainSlider.setTooltip(new Tooltip("Output gain applied after all filters\\n1.0 = unity, 2.0 = +6dB (default)"));
        mInputGainSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mInputGainField.setText(String.format("%.1fx (%.1f dB)", val.floatValue(),
                    20.0 * Math.log10(val.doubleValue())));
                modifiedProperty().set(true);
            }
        });

        mInputGainField = new TextField("2.0x (6.0 dB)");
        mInputGainField.setPrefWidth(120);
        mInputGainField.setMaxWidth(120);
        mInputGainField.setStyle("-fx-font-size: 11px;");
        mInputGainField.setOnAction(event -> {
            commitTextFieldToSlider(mInputGainField, mInputGainSlider, "x", "%.1f");
            double v = (int) mInputGainSlider.getValue();
            mInputGainField.setText(String.format("%.1fx (%.1f dB)", v, 20.0 * Math.log10(v)));
        });
        mInputGainField.focusedProperty().addListener((obs2, wasFocused, isFocused) -> {
            if(!isFocused)
            {
                commitTextFieldToSlider(mInputGainField, mInputGainSlider, "x", "%.1f");
                double v = (int) mInputGainSlider.getValue();
                mInputGainField.setText(String.format("%.1fx (%.1f dB)", v, 20.0 * Math.log10(v)));
            }
        });

        section.getChildren().add(new SettingsRow("Output Gain", createHelpIcon("Adjusts the final audio volume level after all filters have been applied. Use this to compensate for volume lost due to filtering or to boost quiet signals."), mInputGainSlider, mInputGainField));
        return section;
    }"""
)

# 2. High Pass
code = code.replace(
"""    private VBox createHighPassSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("1. High-Pass Filter");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Removes DC offset and sub-audible signalling."));

        section.getChildren().addAll(titleBox, getAudioFilterEnable());
        return section;
    }""",
"""    private SettingsCard createHighPassSection()
    {
        SettingsCard section = new SettingsCard();
        section.getChildren().add(new SettingsRow("High-Pass Filter", createHelpIcon("Removes DC offset and sub-audible signalling."), getAudioFilterEnable()));
        return section;
    }"""
)

# 3. Low Pass
code = code.replace(
"""    private VBox createLowPassSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("1. Low-Pass Filter");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Removes high frequencies above the cutoff. Use this to eliminate harsh high-pitched static and hiss from weak analog FM signals."));

        mLowPassEnabledSwitch = new ToggleSwitch("Enable Low-Pass Filter");
        mLowPassEnabledSwitch.setTooltip(new Tooltip("Remove high-frequency hiss/noise"));
        mLowPassEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mLowPassCutoffSlider.setDisable(!val);
            }
        });

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        Label cutoffLabel = new Label("Cutoff:");
        GridPane.setConstraints(cutoffLabel, 0, 0);
        controlsPane.getChildren().add(cutoffLabel);

        mLowPassCutoffSlider = new Slider(2500, 4000, 2800);
        mLowPassCutoffSlider.setMajorTickUnit(500);
        mLowPassCutoffSlider.setMinorTickCount(4);
        mLowPassCutoffSlider.setShowTickMarks(true);
        mLowPassCutoffSlider.setShowTickLabels(true);
        mLowPassCutoffSlider.setPrefWidth(300);
        mLowPassCutoffSlider.setTooltip(new Tooltip("Higher = brighter\\nLower = less noise\\nDefault: 2800 Hz"));
        mLowPassCutoffSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mLowPassCutoffField.setText(val.intValue() + " Hz");
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mLowPassCutoffSlider, 1, 0);
        controlsPane.getChildren().add(mLowPassCutoffSlider);

        mLowPassCutoffField = createSliderTextField(mLowPassCutoffSlider, "2800 Hz", " Hz", "%.0f");
        GridPane.setConstraints(mLowPassCutoffField, 2, 0);
        controlsPane.getChildren().add(mLowPassCutoffField);

        section.getChildren().addAll(titleBox, mLowPassEnabledSwitch, controlsPane);
        return section;
    }""",
"""    private SettingsCard createLowPassSection()
    {
        SettingsCard section = new SettingsCard();

        mLowPassEnabledSwitch = new ToggleSwitch("Enable");
        mLowPassEnabledSwitch.setTooltip(new Tooltip("Remove high-frequency hiss/noise"));
        mLowPassEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mLowPassCutoffSlider.setDisable(!val);
            }
        });

        mLowPassCutoffSlider = new Slider(2500, 4000, 2800);
        mLowPassCutoffSlider.setMajorTickUnit(500);
        mLowPassCutoffSlider.setMinorTickCount(4);
        mLowPassCutoffSlider.setShowTickMarks(true);
        mLowPassCutoffSlider.setShowTickLabels(true);
        mLowPassCutoffSlider.setPrefWidth(300);
        mLowPassCutoffSlider.setTooltip(new Tooltip("Higher = brighter\\nLower = less noise\\nDefault: 2800 Hz"));
        mLowPassCutoffSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mLowPassCutoffField.setText(val.intValue() + " Hz");
                modifiedProperty().set(true);
            }
        });

        mLowPassCutoffField = createSliderTextField(mLowPassCutoffSlider, "2800 Hz", " Hz", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Low-Pass Filter", createHelpIcon("Removes high frequencies above the cutoff. Use this to eliminate harsh high-pitched static and hiss from weak analog FM signals."), mLowPassEnabledSwitch),
            new SettingsRow("Cutoff", mLowPassCutoffSlider, mLowPassCutoffField)
        );
        return section;
    }"""
)

# 4. Voice Enhancement
code = code.replace(
"""    private VBox createVoiceEnhanceSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("4. Voice Enhancement");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Boosts frequencies in the 2-4 kHz range (presence peak). Use this to improve speech intelligibility and make voices stand out, especially in noisy environments."));

        mVoiceEnhanceEnabledSwitch = new ToggleSwitch("Enable Voice Enhancement");
        mVoiceEnhanceEnabledSwitch.setTooltip(new Tooltip("Boost speech clarity (2-4 kHz presence)"));
        mVoiceEnhanceEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mVoiceEnhanceSlider.setDisable(!val);
            }
        });

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        Label amountLabel = new Label("Amount:");
        GridPane.setConstraints(amountLabel, 0, 0);
        controlsPane.getChildren().add(amountLabel);

        mVoiceEnhanceSlider = new Slider(0, 100, 0);
        mVoiceEnhanceSlider.setMajorTickUnit(25);
        mVoiceEnhanceSlider.setMinorTickCount(4);
        mVoiceEnhanceSlider.setShowTickMarks(true);
        mVoiceEnhanceSlider.setShowTickLabels(true);
        mVoiceEnhanceSlider.setPrefWidth(300);
        mVoiceEnhanceSlider.setTooltip(new Tooltip("Boost speech presence\\n0% = off, 100% = max clarity\\nDefault: 0%"));
        mVoiceEnhanceSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mVoiceEnhanceField.setText(val.intValue() + "%");
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mVoiceEnhanceSlider, 1, 0);
        controlsPane.getChildren().add(mVoiceEnhanceSlider);

        mVoiceEnhanceField = createSliderTextField(mVoiceEnhanceSlider, "0%", "%", "%.0f");
        GridPane.setConstraints(mVoiceEnhanceField, 2, 0);
        controlsPane.getChildren().add(mVoiceEnhanceField);

        section.getChildren().addAll(titleBox, mVoiceEnhanceEnabledSwitch, controlsPane);
        return section;
    }""",
"""    private SettingsCard createVoiceEnhanceSection()
    {
        SettingsCard section = new SettingsCard();

        mVoiceEnhanceEnabledSwitch = new ToggleSwitch("Enable");
        mVoiceEnhanceEnabledSwitch.setTooltip(new Tooltip("Boost speech clarity (2-4 kHz presence)"));
        mVoiceEnhanceEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mVoiceEnhanceSlider.setDisable(!val);
            }
        });

        mVoiceEnhanceSlider = new Slider(0, 100, 0);
        mVoiceEnhanceSlider.setMajorTickUnit(25);
        mVoiceEnhanceSlider.setMinorTickCount(4);
        mVoiceEnhanceSlider.setShowTickMarks(true);
        mVoiceEnhanceSlider.setShowTickLabels(true);
        mVoiceEnhanceSlider.setPrefWidth(300);
        mVoiceEnhanceSlider.setTooltip(new Tooltip("Boost speech presence\\n0% = off, 100% = max clarity\\nDefault: 0%"));
        mVoiceEnhanceSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mVoiceEnhanceField.setText(val.intValue() + "%");
                modifiedProperty().set(true);
            }
        });

        mVoiceEnhanceField = createSliderTextField(mVoiceEnhanceSlider, "0%", "%", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Voice Enhancement", createHelpIcon("Boosts frequencies in the 2-4 kHz range (presence peak). Use this to improve speech intelligibility and make voices stand out, especially in noisy environments."), mVoiceEnhanceEnabledSwitch),
            new SettingsRow("Amount", mVoiceEnhanceSlider, mVoiceEnhanceField)
        );
        return section;
    }"""
)

# 5. Bass Boost
code = code.replace(
"""    private VBox createBassBoostSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("3. Bass Boost");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Boosts low frequencies (low-shelf) below 400 Hz. Use this to add depth and richness to voices that sound thin or \\'tinny\\'."));

        mBassBoostEnabledSwitch = new ToggleSwitch("Enable Bass Boost");
        mBassBoostEnabledSwitch.setTooltip(new Tooltip("Boost low frequencies below 400 Hz for warmth"));
        mBassBoostEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mBassBoostSlider.setDisable(!val);
            }
        });

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        Label amountLabel = new Label("Boost Amount:");
        GridPane.setConstraints(amountLabel, 0, 0);
        controlsPane.getChildren().add(amountLabel);

        mBassBoostSlider = new Slider(0, 12, 0);
        mBassBoostSlider.setMajorTickUnit(3);
        mBassBoostSlider.setMinorTickCount(2);
        mBassBoostSlider.setShowTickMarks(true);
        mBassBoostSlider.setShowTickLabels(true);
        mBassBoostSlider.setPrefWidth(300);
        mBassBoostSlider.setTooltip(new Tooltip("Low-shelf boost below 400 Hz\\n0 dB = off, +12 dB = max bass\\nDefault: 0 dB"));
        mBassBoostSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mBassBoostField.setText(String.format("+%.1f dB", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mBassBoostSlider, 1, 0);
        controlsPane.getChildren().add(mBassBoostSlider);

        mBassBoostField = createSliderTextField(mBassBoostSlider, "+0.0 dB", " dB", "+%.1f");
        GridPane.setConstraints(mBassBoostField, 2, 0);
        controlsPane.getChildren().add(mBassBoostField);

        section.getChildren().addAll(titleBox, mBassBoostEnabledSwitch, controlsPane);
        return section;
    }""",
"""    private SettingsCard createBassBoostSection()
    {
        SettingsCard section = new SettingsCard();

        mBassBoostEnabledSwitch = new ToggleSwitch("Enable");
        mBassBoostEnabledSwitch.setTooltip(new Tooltip("Boost low frequencies below 400 Hz for warmth"));
        mBassBoostEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mBassBoostSlider.setDisable(!val);
            }
        });

        mBassBoostSlider = new Slider(0, 12, 0);
        mBassBoostSlider.setMajorTickUnit(3);
        mBassBoostSlider.setMinorTickCount(2);
        mBassBoostSlider.setShowTickMarks(true);
        mBassBoostSlider.setShowTickLabels(true);
        mBassBoostSlider.setPrefWidth(300);
        mBassBoostSlider.setTooltip(new Tooltip("Low-shelf boost below 400 Hz\\n0 dB = off, +12 dB = max bass\\nDefault: 0 dB"));
        mBassBoostSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mBassBoostField.setText(String.format("+%.1f dB", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mBassBoostField = createSliderTextField(mBassBoostSlider, "+0.0 dB", " dB", "+%.1f");

        section.getChildren().addAll(
            new SettingsRow("Bass Boost", createHelpIcon("Boosts low frequencies (low-shelf) below 400 Hz. Use this to add depth and richness to voices that sound thin or \\'tinny\\'."), mBassBoostEnabledSwitch),
            new SettingsRow("Boost Amount", mBassBoostSlider, mBassBoostField)
        );
        return section;
    }"""
)

# 6. Hiss Reduction
code = code.replace(
"""    private VBox createHissReductionSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("2. Hiss Reduction");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Attenuates high frequencies (high-shelf cut) above the corner frequency. Use this to soften background hiss while preserving voice clarity better than a hard low-pass filter."));

        mHissReductionEnabledSwitch = new ToggleSwitch("Enable Hiss Reduction");
        mHissReductionEnabledSwitch.setTooltip(new Tooltip(
                "High-shelf cut above corner frequency to reduce FM hiss.\\n" +
                "Stacks with Low-Pass Filter."));
        mHissReductionEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mHissReductionDbSlider.setDisable(!val);
                mHissReductionCornerSlider.setDisable(!val);
            }
        });

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        // Row 0: Shelf cut amount
        Label dbLabel = new Label("Cut Amount:");
        GridPane.setConstraints(dbLabel, 0, 0);
        controlsPane.getChildren().add(dbLabel);

        mHissReductionDbSlider = new Slider(-12, 0, -6);
        mHissReductionDbSlider.setMajorTickUnit(3);
        mHissReductionDbSlider.setMinorTickCount(2);
        mHissReductionDbSlider.setShowTickMarks(true);
        mHissReductionDbSlider.setShowTickLabels(true);
        mHissReductionDbSlider.setPrefWidth(300);
        mHissReductionDbSlider.setTooltip(new Tooltip(
                "High-shelf attenuation above corner frequency.\\n" +
                "0 dB = off, -12 dB = max hiss cut\\nDefault: -6 dB"));
        mHissReductionDbSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHissReductionDbField.setText(String.format("%.1f dB", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mHissReductionDbSlider, 1, 0);
        controlsPane.getChildren().add(mHissReductionDbSlider);

        mHissReductionDbField = createSliderTextField(mHissReductionDbSlider, "-6.0 dB", " dB", "%.1f");
        GridPane.setConstraints(mHissReductionDbField, 2, 0);
        controlsPane.getChildren().add(mHissReductionDbField);

        // Row 1: Corner frequency
        Label cornerLabel = new Label("Corner Freq:");
        GridPane.setConstraints(cornerLabel, 0, 1);
        controlsPane.getChildren().add(cornerLabel);

        mHissReductionCornerSlider = new Slider(1000, 3500, 2000);
        mHissReductionCornerSlider.setMajorTickUnit(500);
        mHissReductionCornerSlider.setMinorTickCount(4);
        mHissReductionCornerSlider.setShowTickMarks(true);
        mHissReductionCornerSlider.setShowTickLabels(true);
        mHissReductionCornerSlider.setPrefWidth(300);
        mHissReductionCornerSlider.setTooltip(new Tooltip(
                "Shelf pivot frequency. Hiss above this is attenuated.\\n" +
                "Lower = more hiss cut but slightly duller voice.\\nDefault: 2000 Hz"));
        mHissReductionCornerSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHissReductionCornerField.setText(String.format("%.0f Hz", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mHissReductionCornerSlider, 1, 1);
        controlsPane.getChildren().add(mHissReductionCornerSlider);

        mHissReductionCornerField = createSliderTextField(mHissReductionCornerSlider, "2000 Hz", " Hz", "%.0f");
        GridPane.setConstraints(mHissReductionCornerField, 2, 1);
        controlsPane.getChildren().add(mHissReductionCornerField);

        section.getChildren().addAll(titleBox, mHissReductionEnabledSwitch, controlsPane);
        return section;
    }""",
"""    private SettingsCard createHissReductionSection()
    {
        SettingsCard section = new SettingsCard();

        mHissReductionEnabledSwitch = new ToggleSwitch("Enable");
        mHissReductionEnabledSwitch.setTooltip(new Tooltip(
                "High-shelf cut above corner frequency to reduce FM hiss.\\n" +
                "Stacks with Low-Pass Filter."));
        mHissReductionEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mHissReductionDbSlider.setDisable(!val);
                mHissReductionCornerSlider.setDisable(!val);
            }
        });

        mHissReductionDbSlider = new Slider(-12, 0, -6);
        mHissReductionDbSlider.setMajorTickUnit(3);
        mHissReductionDbSlider.setMinorTickCount(2);
        mHissReductionDbSlider.setShowTickMarks(true);
        mHissReductionDbSlider.setShowTickLabels(true);
        mHissReductionDbSlider.setPrefWidth(300);
        mHissReductionDbSlider.setTooltip(new Tooltip(
                "High-shelf attenuation above corner frequency.\\n" +
                "0 dB = off, -12 dB = max hiss cut\\nDefault: -6 dB"));
        mHissReductionDbSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHissReductionDbField.setText(String.format("%.1f dB", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mHissReductionDbField = createSliderTextField(mHissReductionDbSlider, "-6.0 dB", " dB", "%.1f");

        mHissReductionCornerSlider = new Slider(1000, 3500, 2000);
        mHissReductionCornerSlider.setMajorTickUnit(500);
        mHissReductionCornerSlider.setMinorTickCount(4);
        mHissReductionCornerSlider.setShowTickMarks(true);
        mHissReductionCornerSlider.setShowTickLabels(true);
        mHissReductionCornerSlider.setPrefWidth(300);
        mHissReductionCornerSlider.setTooltip(new Tooltip(
                "Shelf pivot frequency. Hiss above this is attenuated.\\n" +
                "Lower = more hiss cut but slightly duller voice.\\nDefault: 2000 Hz"));
        mHissReductionCornerSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHissReductionCornerField.setText(String.format("%.0f Hz", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mHissReductionCornerField = createSliderTextField(mHissReductionCornerSlider, "2000 Hz", " Hz", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Hiss Reduction", createHelpIcon("Attenuates high frequencies (high-shelf cut) above the corner frequency. Use this to soften background hiss while preserving voice clarity better than a hard low-pass filter."), mHissReductionEnabledSwitch),
            new SettingsRow("Cut Amount", mHissReductionDbSlider, mHissReductionDbField),
            new SettingsRow("Corner Freq", mHissReductionCornerSlider, mHissReductionCornerField)
        );
        return section;
    }"""
)


# 7. Squelch Tail
code = code.replace(
"""    private VBox createSquelchTailSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("5. Squelch Tail Removal");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Trims the beginning or end of transmissions to remove noise bursts or tone ramp-ups. Hangtime adds a delay before closing audio."));

        mSquelchTailEnabledSwitch = new ToggleSwitch("Enable Squelch Tail Removal");
        mSquelchTailEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
            }
        });

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        Label headLabel = new Label("Head Trim (ms):");
        GridPane.setConstraints(headLabel, 0, 0);
        controlsPane.getChildren().add(headLabel);

        mHeadRemovalSpinner = new Spinner<>(0, 150, 0, 10);
        mHeadRemovalSpinner.setEditable(true);
        mHeadRemovalSpinner.setPrefWidth(100);
        mHeadRemovalSpinner.setTooltip(new Tooltip("Milliseconds to trim from start of transmission (removes tone ramp-up)"));
        mHeadRemovalSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mHeadRemovalSpinner, 1, 0);
        controlsPane.getChildren().add(mHeadRemovalSpinner);

        Label tailLabel = new Label("Tail Trim (ms):");
        GridPane.setConstraints(tailLabel, 2, 0);
        controlsPane.getChildren().add(tailLabel);

        mTailRemovalSpinner = new Spinner<>(0, 300, 100, 10);
        mTailRemovalSpinner.setEditable(true);
        mTailRemovalSpinner.setPrefWidth(100);
        mTailRemovalSpinner.setTooltip(new Tooltip("Milliseconds to trim from end of transmission (removes noise burst)"));
        mTailRemovalSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mTailRemovalSpinner, 3, 0);
        controlsPane.getChildren().add(mTailRemovalSpinner);

        Label hangtimeLabel = new Label("Hangtime (ms):");
        GridPane.setConstraints(hangtimeLabel, 0, 1);
        controlsPane.getChildren().add(hangtimeLabel);

        mAudioHangtimeSpinner = new Spinner<>(0, 1000, 0, 50);
        mAudioHangtimeSpinner.setEditable(true);
        mAudioHangtimeSpinner.setPrefWidth(100);
        mAudioHangtimeSpinner.setTooltip(new Tooltip("Delay before closing audio segment after transmission ends.\\nPrevents cutting off the end of audio in ThinLine/Zello streams.\\n0 = immediate close (default), 100-300 = recommended for streaming"));
        mAudioHangtimeSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mAudioHangtimeSpinner, 1, 1);
        controlsPane.getChildren().add(mAudioHangtimeSpinner);

        section.getChildren().addAll(titleBox, mSquelchTailEnabledSwitch, controlsPane);
        return section;
    }""",
"""    private SettingsCard createSquelchTailSection()
    {
        SettingsCard section = new SettingsCard();

        mSquelchTailEnabledSwitch = new ToggleSwitch("Enable");
        mSquelchTailEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
            }
        });

        mHeadRemovalSpinner = new Spinner<>(0, 150, 0, 10);
        mHeadRemovalSpinner.setEditable(true);
        mHeadRemovalSpinner.setPrefWidth(100);
        mHeadRemovalSpinner.setTooltip(new Tooltip("Milliseconds to trim from start of transmission (removes tone ramp-up)"));
        mHeadRemovalSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });

        mTailRemovalSpinner = new Spinner<>(0, 300, 100, 10);
        mTailRemovalSpinner.setEditable(true);
        mTailRemovalSpinner.setPrefWidth(100);
        mTailRemovalSpinner.setTooltip(new Tooltip("Milliseconds to trim from end of transmission (removes noise burst)"));
        mTailRemovalSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });

        mAudioHangtimeSpinner = new Spinner<>(0, 1000, 0, 50);
        mAudioHangtimeSpinner.setEditable(true);
        mAudioHangtimeSpinner.setPrefWidth(100);
        mAudioHangtimeSpinner.setTooltip(new Tooltip("Delay before closing audio segment after transmission ends.\\nPrevents cutting off the end of audio in ThinLine/Zello streams.\\n0 = immediate close (default), 100-300 = recommended for streaming"));
        mAudioHangtimeSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });

        section.getChildren().addAll(
            new SettingsRow("Squelch Tail Removal", createHelpIcon("Trims the beginning or end of transmissions to remove noise bursts or tone ramp-ups. Hangtime adds a delay before closing audio."), mSquelchTailEnabledSwitch),
            new SettingsRow("Head Trim (ms)", mHeadRemovalSpinner),
            new SettingsRow("Tail Trim (ms)", mTailRemovalSpinner),
            new SettingsRow("Hangtime (ms)", mAudioHangtimeSpinner)
        );
        return section;
    }"""
)

# 8. Squelch 
code = code.replace(
"""    private VBox createSquelchSection()
    {
        VBox section = new VBox(5);
        Label title = new Label("6. Squelch / Noise Gate");
        title.setFont(Font.font(null, FontWeight.BOLD, 12));
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, createHelpIcon("Mutes the audio when the signal level drops below the threshold. Use this to silence background static and noise between active voice transmissions."));

        mSquelchEnabledSwitch = new ToggleSwitch("Enable Squelch/Noise Gate");
        mSquelchEnabledSwitch.setTooltip(new Tooltip("Silence carrier/static between voice"));
        mSquelchEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mSquelchThresholdSlider.setDisable(!val);
                mSquelchReductionSlider.setDisable(!val);
                mHoldTimeSlider.setDisable(!val);
            }
        });

        GridPane controlsPane = new GridPane();
        controlsPane.setHgap(10);
        controlsPane.setVgap(5);

        // Threshold: 0-100%
        Label threshLabel = new Label("Threshold:");
        GridPane.setConstraints(threshLabel, 0, 0);
        controlsPane.getChildren().add(threshLabel);

        mSquelchThresholdSlider = new Slider(0, 100, 4.0);
        mSquelchThresholdSlider.setMajorTickUnit(25);
        mSquelchThresholdSlider.setMinorTickCount(4);
        mSquelchThresholdSlider.setShowTickMarks(true);
        mSquelchThresholdSlider.setShowTickLabels(true);
        mSquelchThresholdSlider.setPrefWidth(300);
        mSquelchThresholdSlider.setTooltip(new Tooltip("Gate opens when level > threshold\\nLower = more sensitive\\nDefault: 4%"));
        mSquelchThresholdSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mSquelchThresholdField.setText(String.format("%.1f%%", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mSquelchThresholdSlider, 1, 0);
        controlsPane.getChildren().add(mSquelchThresholdSlider);

        mSquelchThresholdField = createSliderTextField(mSquelchThresholdSlider, "4.0%", "%", "%.1f");
        GridPane.setConstraints(mSquelchThresholdField, 2, 0);
        controlsPane.getChildren().add(mSquelchThresholdField);

        // Reduction: 0-100%
        Label reductionLabel = new Label("Reduction:");
        GridPane.setConstraints(reductionLabel, 0, 1);
        controlsPane.getChildren().add(reductionLabel);

        mSquelchReductionSlider = new Slider(0, 100, 80);
        mSquelchReductionSlider.setMajorTickUnit(25);
        mSquelchReductionSlider.setMinorTickCount(4);
        mSquelchReductionSlider.setShowTickMarks(true);
        mSquelchReductionSlider.setShowTickLabels(true);
        mSquelchReductionSlider.setPrefWidth(300);
        mSquelchReductionSlider.setTooltip(new Tooltip("How much to reduce carrier noise\\n0% = no reduction, 100% = full mute\\nDefault: 80%"));
        mSquelchReductionSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mSquelchReductionField.setText(val.intValue() + "%");
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mSquelchReductionSlider, 1, 1);
        controlsPane.getChildren().add(mSquelchReductionSlider);

        mSquelchReductionField = createSliderTextField(mSquelchReductionSlider, "80%", "%", "%.0f");
        GridPane.setConstraints(mSquelchReductionField, 2, 1);
        controlsPane.getChildren().add(mSquelchReductionField);

        // Hold Time (Delay): 0-1000ms
        Label holdLabel = new Label("Delay (Hold Time):");
        GridPane.setConstraints(holdLabel, 0, 2);
        controlsPane.getChildren().add(holdLabel);

        mHoldTimeSlider = new Slider(0, 1000, 500);
        mHoldTimeSlider.setMajorTickUnit(250);
        mHoldTimeSlider.setMinorTickCount(4);
        mHoldTimeSlider.setShowTickMarks(true);
        mHoldTimeSlider.setShowTickLabels(true);
        mHoldTimeSlider.setPrefWidth(300);
        mHoldTimeSlider.setTooltip(new Tooltip("Keep gate open after voice stops\\nSilences carrier/static between voice\\nDefault: 500ms"));
        mHoldTimeSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHoldTimeField.setText(val.intValue() + " ms");
                modifiedProperty().set(true);
            }
        });
        GridPane.setConstraints(mHoldTimeSlider, 1, 2);
        controlsPane.getChildren().add(mHoldTimeSlider);

        mHoldTimeField = createSliderTextField(mHoldTimeSlider, "500 ms", " ms", "%.0f");
        GridPane.setConstraints(mHoldTimeField, 2, 2);
        controlsPane.getChildren().add(mHoldTimeField);

        section.getChildren().addAll(titleBox, mSquelchEnabledSwitch, controlsPane);
        return section;
    }""",
"""    private SettingsCard createSquelchSection()
    {
        SettingsCard section = new SettingsCard();

        mSquelchEnabledSwitch = new ToggleSwitch("Enable");
        mSquelchEnabledSwitch.setTooltip(new Tooltip("Silence carrier/static between voice"));
        mSquelchEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mSquelchThresholdSlider.setDisable(!val);
                mSquelchReductionSlider.setDisable(!val);
                mHoldTimeSlider.setDisable(!val);
            }
        });

        mSquelchThresholdSlider = new Slider(0, 100, 4.0);
        mSquelchThresholdSlider.setMajorTickUnit(25);
        mSquelchThresholdSlider.setMinorTickCount(4);
        mSquelchThresholdSlider.setShowTickMarks(true);
        mSquelchThresholdSlider.setShowTickLabels(true);
        mSquelchThresholdSlider.setPrefWidth(300);
        mSquelchThresholdSlider.setTooltip(new Tooltip("Gate opens when level > threshold\\nLower = more sensitive\\nDefault: 4%"));
        mSquelchThresholdSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mSquelchThresholdField.setText(String.format("%.1f%%", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mSquelchThresholdField = createSliderTextField(mSquelchThresholdSlider, "4.0%", "%", "%.1f");

        mSquelchReductionSlider = new Slider(0, 100, 80);
        mSquelchReductionSlider.setMajorTickUnit(25);
        mSquelchReductionSlider.setMinorTickCount(4);
        mSquelchReductionSlider.setShowTickMarks(true);
        mSquelchReductionSlider.setShowTickLabels(true);
        mSquelchReductionSlider.setPrefWidth(300);
        mSquelchReductionSlider.setTooltip(new Tooltip("How much to reduce carrier noise\\n0% = no reduction, 100% = full mute\\nDefault: 80%"));
        mSquelchReductionSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mSquelchReductionField.setText(val.intValue() + "%");
                modifiedProperty().set(true);
            }
        });

        mSquelchReductionField = createSliderTextField(mSquelchReductionSlider, "80%", "%", "%.0f");

        mHoldTimeSlider = new Slider(0, 1000, 500);
        mHoldTimeSlider.setMajorTickUnit(250);
        mHoldTimeSlider.setMinorTickCount(4);
        mHoldTimeSlider.setShowTickMarks(true);
        mHoldTimeSlider.setShowTickLabels(true);
        mHoldTimeSlider.setPrefWidth(300);
        mHoldTimeSlider.setTooltip(new Tooltip("Keep gate open after voice stops\\nSilences carrier/static between voice\\nDefault: 500ms"));
        mHoldTimeSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHoldTimeField.setText(val.intValue() + " ms");
                modifiedProperty().set(true);
            }
        });

        mHoldTimeField = createSliderTextField(mHoldTimeSlider, "500 ms", " ms", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Squelch / Noise Gate", createHelpIcon("Mutes the audio when the signal level drops below the threshold. Use this to silence background static and noise between active voice transmissions."), mSquelchEnabledSwitch),
            new SettingsRow("Threshold", mSquelchThresholdSlider, mSquelchThresholdField),
            new SettingsRow("Reduction", mSquelchReductionSlider, mSquelchReductionField),
            new SettingsRow("Delay (Hold Time)", mHoldTimeSlider, mHoldTimeField)
        );
        return section;
    }"""
)


with open(file_path, "w") as f:
    f.write(code)

print("Done replacing NBFM file.")
