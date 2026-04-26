package io.github.dsheirer.alias.action.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptActionTest {

    @TempDir
    Path tempDir;

    @Test
    public void testPlay_NullScript() throws Exception {
        ScriptAction action = new ScriptAction();
        action.setScript(null);
        // Should not throw any exception
        action.play();
    }

    @Test
    public void testPlay_NonExistentFile() {
        ScriptAction action = new ScriptAction();
        action.setScript(tempDir.resolve("non_existent.sh").toString());
        assertThrows(FileNotFoundException.class, action::play);
    }

    @Test
    public void testPlay_IsDirectory() throws IOException {
        Path dir = tempDir.resolve("some_dir");
        Files.createDirectory(dir);
        ScriptAction action = new ScriptAction();
        action.setScript(dir.toString());
        assertThrows(IOException.class, action::play);
    }

    @Test
    public void testPlay_ValidScript() throws Exception {
        Path scriptFile = tempDir.resolve("test_script.sh");
        // Create a simple script
        Files.write(scriptFile, "#!/bin/sh\nexit 0".getBytes());
        scriptFile.toFile().setExecutable(true);

        ScriptAction action = new ScriptAction();
        action.setScript(scriptFile.toString());

        try {
            action.play();
        } catch (Exception e) {
            // We want to ensure it didn't fail at our new validation checks
            if (e instanceof FileNotFoundException) {
                fail("Should not have thrown FileNotFoundException");
            }
            if (e.getMessage() != null && e.getMessage().contains("is not a regular file")) {
                fail("Should not have thrown IOException for not being a regular file");
            }
            // Other exceptions (like exit code != 0 or ProcessBuilder failing to start the script) are acceptable
            // as they mean the validation passed.
        }
    }
}
