#!/bin/bash
git fetch origin
git branch -D temp_merge || true
git checkout -b temp_merge origin/master

git checkout pragmatic-architect/migrate-tier-1-dialogs -- src/main/java/io/github/dsheirer/filter/FilterEditor.java src/main/java/io/github/dsheirer/filter/FilterEditorController.java src/main/resources/fxml/FilterEditor.fxml src/main/java/io/github/dsheirer/gui/NotificationManager.java src/main/java/io/github/dsheirer/module/decode/event/HistoryManagementPanel.java src/main/java/io/github/dsheirer/module/decode/event/filter/EventFilterButton.java src/main/java/io/github/dsheirer/source/tuner/ui/DiscoveredTunerEditor.java src/main/java/io/github/dsheirer/source/tuner/ui/EmptyTunerEditor.java
git rm src/main/java/io/github/dsheirer/filter/FilterEditorPanel.java

git commit -m "Merge master and resolve conflicts for migrate-tier-1-dialogs"
git checkout pragmatic-architect/migrate-tier-1-dialogs
git reset --hard temp_merge
