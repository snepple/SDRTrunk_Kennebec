/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.health.HealthDiagnostics.Diagnosis;
import io.github.dsheirer.health.HealthDiagnostics.Severity;
import org.junit.jupiter.api.Test;

/**
 * Tests the deterministic {@link HealthDiagnostics} signature catalog - that recognized SDRTrunk log signatures
 * map to the correct severity and a useful remediation, and unknown lines return null.
 */
class HealthDiagnosticsTest
{
    @Test
    void unknownAndNullReturnNull()
    {
        assertNull(HealthDiagnostics.diagnose(null));
        assertNull(HealthDiagnostics.diagnose("20260622 nothing-interesting here"));
        assertNull(HealthDiagnostics.explain("just a normal info line"));
    }

    @Test
    void nativeThreadExhaustionIsDistinctFromHeap()
    {
        Diagnosis nativeThread = HealthDiagnostics.diagnose(
            "java.lang.OutOfMemoryError: unable to create native thread: possibly out of memory or process/resource limits reached");
        assertNotNull(nativeThread);
        assertEquals(Severity.CRITICAL, nativeThread.severity());
        assertEquals("Out of Native Threads", nativeThread.title());
        assertTrue(nativeThread.action().toLowerCase().contains("max traffic channels")
            || nativeThread.action().toLowerCase().contains("threads-max"));

        Diagnosis heap = HealthDiagnostics.diagnose("java.lang.OutOfMemoryError: Java heap space");
        assertNotNull(heap);
        assertEquals("Critical Memory Exhaustion", heap.title());
        assertTrue(heap.action().contains("-Xmx"));
    }

    @Test
    void libUsbNoDeviceRecommendsZadig()
    {
        Diagnosis d = HealthDiagnostics.diagnose("Unable to open device [-4/LIBUSB_ERROR_NO_DEVICE]");
        assertNotNull(d);
        assertEquals(Severity.CRITICAL, d.title().isEmpty() ? null : d.severity());
        assertTrue(d.action().toLowerCase().contains("zadig"));
    }

    @Test
    void usbSaturationAndProcessorOverloadAreWarnings()
    {
        Diagnosis overrun = HealthDiagnostics.diagnose("data over run on USB, throwing away samples");
        assertNotNull(overrun);
        assertEquals(Severity.WARNING, overrun.severity());

        Diagnosis overload = HealthDiagnostics.diagnose(
            "polyphase buffer processor [101.1000 MHz] processor overloaded - dropped 19 element(s)");
        assertNotNull(overload);
        assertEquals(Severity.WARNING, overload.severity());
    }

    @Test
    void syncLossExplainsPpmAndAgc()
    {
        Diagnosis d = HealthDiagnostics.diagnose("[P25P1] SYNC LOSS - BITS PROCESSED [9600]");
        assertNotNull(d);
        String text = d.toInspectorText().toLowerCase();
        assertTrue(text.contains("ppm") || text.contains("drift"));
        assertTrue(text.contains("gain"));
    }

    @Test
    void jmbeAndAliasNpeAreCritical()
    {
        Diagnosis jmbe = HealthDiagnostics.diagnose(
            "NullPointerException at io.github.dsheirer.module.decode.p25.audio.P25P1AudioModule.getAudioCodec");
        assertNotNull(jmbe);
        assertEquals(Severity.CRITICAL, jmbe.severity());
        assertTrue(jmbe.action().toLowerCase().contains("jmbe"));

        Diagnosis alias = HealthDiagnostics.diagnose(
            "java.lang.NullPointerException at alias.AliasDirectory.getAliasList");
        assertNotNull(alias);
        assertEquals(Severity.CRITICAL, alias.severity());
        assertTrue(alias.title().toLowerCase().contains("alias"));
    }

    @Test
    void benignParsingDropIsInfoWithNoFix()
    {
        Diagnosis d = HealthDiagnostics.diagnose("Error invoking getMessage() on null AMBTC data block");
        assertNotNull(d);
        assertEquals(Severity.INFO, d.severity());
        //An informational drop has no remediation step, so the inspector text omits the Fix line.
        assertTrue(!d.toInspectorText().contains("Fix:"));
    }

    @Test
    void inspectorTextIncludesFixForActionableDiagnoses()
    {
        String text = HealthDiagnostics.explain("Mixer:Port Speakers - audio format not supported");
        assertNotNull(text);
        assertTrue(text.contains("Fix:"));
    }
}
