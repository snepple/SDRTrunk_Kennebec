cat << 'PATCH_EOF' > patch5.diff
--- src/main/java/io/github/dsheirer/gui/playlist/streaming/StreamingEditor.java
+++ src/main/java/io/github/dsheirer/gui/playlist/streaming/StreamingEditor.java
@@ -204,6 +204,7 @@
         }

         getDeleteButton().setDisable(configuredBroadcast == null);
+        getCloneButton().setDisable(configuredBroadcast == null);

         if(configuredBroadcast == null)
         {
PATCH_EOF
patch src/main/java/io/github/dsheirer/gui/playlist/streaming/StreamingEditor.java patch5.diff
