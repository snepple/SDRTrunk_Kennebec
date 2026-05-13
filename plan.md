1. **Optimize Search Field Filters in P25 and DMR Viewers**:
   - The memory note explicitly says: "In JavaFX `FilteredList` implementations (e.g., table search fields in SDRTrunk), avoid compiling regex patterns (`Pattern.compile()`) or applying invariant transformations (like `toLowerCase()`) inside the `setPredicate` lambda. Hoist these operations outside the predicate to prevent redundant O(N) evaluations per keystroke and severe UI lag."
   - In `P25P1Viewer.java`, `P25P2Viewer.java`, and `DmrViewer.java`, we have this block:
     ```java
     Predicate<MessagePackage> textPredicate = message -> message.toString().toLowerCase().contains(filterText.toLowerCase());
     ```
   - I will hoist `filterText.toLowerCase()` into a `lowerCaseFilterText` variable outside the lambda.

   Example change for `P25P1Viewer.java`:
   ```java
        if(filterText != null && !filterText.isEmpty())
        {
            final String lowerCaseFilterText = filterText.toLowerCase();
            Predicate<MessagePackage> textPredicate = message -> message.toString().toLowerCase().contains(lowerCaseFilterText);
            mFilteredMessagePackages.setPredicate(textPredicate);
        }
   ```
   (And similarly in the `find` methods of these 3 viewers, we should do the same optimization.)

   Let's check `ChannelEditor.java`, `matchesFilter` also has `mFilterText = filterText.toLowerCase();` already outside the loop, but `channel.getName().toLowerCase()` is inside, which is fine since it's the item's own value.

   So the main target is the 3 Viewers: `P25P1Viewer`, `P25P2Viewer`, `DmrViewer`. This is exactly the kind of optimization the agent 'Bolt' should do: Hoist loop invariant out of a lambda.
