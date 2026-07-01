package io.github.dsheirer.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NaturalOrderComparatorTest
{
    private final NaturalOrderComparator mComparator = NaturalOrderComparator.INSTANCE;

    @Test
    public void multiDigitNumbersSortByValueNotLexically()
    {
        List<String> values = new ArrayList<>(Arrays.asList(
                "10", "2", "1", "21", "3", "20", "11", "13", "9", "19"));
        Collections.sort(values, mComparator);

        assertEquals(Arrays.asList("1", "2", "3", "9", "10", "11", "13", "19", "20", "21"), values);
    }

    @Test
    public void decimalFrequenciesSortNumerically()
    {
        //A naive digit-run comparison would compare 1252 against 2 and wrongly order 155.2 before 155.1252.
        List<String> values = new ArrayList<>(Arrays.asList("155.9", "155.1252", "155.75", "155.2"));
        Collections.sort(values, mComparator);

        assertEquals(Arrays.asList("155.1252", "155.2", "155.75", "155.9"), values);
    }

    @Test
    public void mixedAlphanumericSortsNaturally()
    {
        List<String> values = new ArrayList<>(Arrays.asList("Site 10", "Site 2", "Site 1", "Site 20"));
        Collections.sort(values, mComparator);

        assertEquals(Arrays.asList("Site 1", "Site 2", "Site 10", "Site 20"), values);
    }

    @Test
    public void blankValuesSortLast()
    {
        List<String> values = new ArrayList<>(Arrays.asList("10", "", "2", null, "1"));
        Collections.sort(values, mComparator);

        assertEquals(Arrays.asList("1", "2", "10", "", null), values);
    }

    @Test
    public void leadingZerosDoNotChangeNumericOrder()
    {
        assertTrue(mComparator.compare("007", "10") < 0);
        assertTrue(mComparator.compare("010", "9") > 0);
    }

    @Test
    public void comparisonIsSymmetric()
    {
        assertTrue(mComparator.compare("2", "10") < 0);
        assertTrue(mComparator.compare("10", "2") > 0);
        assertEquals(0, mComparator.compare("abc", "ABC"));
    }
}
