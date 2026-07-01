/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.util;

import java.util.Comparator;

/**
 * Comparator that sorts strings in "natural" order so that embedded numbers are ordered by their value rather than
 * lexically.  Plain string sorting orders "10" before "2" (comparing the first character '1' against '2'); this
 * comparator orders 1, 2, 3 ... 9, 10, 11 ... 21 as a person would expect.
 *
 * It is intended for table columns whose cell values are rendered as strings but hold numeric data such as counts,
 * talkgroup/radio ids and frequencies.  Behavior:
 * <ul>
 *   <li>Values that are entirely a plain number (including signed and decimal values such as {@code 155.1252}) are
 *       compared numerically.</li>
 *   <li>Mixed alphanumeric values (for example {@code "Site 10"}) are compared segment by segment, with runs of
 *       digits compared as numbers and other characters compared case-insensitively.</li>
 *   <li>Blank/null values sort last so empty cells do not lead an ascending sort.</li>
 * </ul>
 */
public final class NaturalOrderComparator implements Comparator<String>
{
    /** Shared, stateless, thread-safe instance. */
    public static final NaturalOrderComparator INSTANCE = new NaturalOrderComparator();

    @Override
    public int compare(String left, String right)
    {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();

        boolean aEmpty = a.isEmpty();
        boolean bEmpty = b.isEmpty();
        if(aEmpty || bEmpty)
        {
            //Non-empty sorts before empty so blank cells fall to the bottom of an ascending sort.
            return aEmpty == bEmpty ? 0 : (aEmpty ? 1 : -1);
        }

        //Fast path: when both values are entirely plain numbers, compare by value.  This also gives correct
        //ordering for decimals (155.1252 < 155.9) that a digit-run comparison would get wrong.
        Double na = asNumber(a);
        Double nb = asNumber(b);
        if(na != null && nb != null)
        {
            int result = Double.compare(na, nb);
            return result != 0 ? result : a.compareToIgnoreCase(b);
        }

        return naturalCompare(a, b);
    }

    /**
     * Returns the numeric value when the entire string is a plain integer or decimal number (optionally signed),
     * otherwise null.  Guards against {@link Double#valueOf(String)} accepting values such as "NaN", "Infinity",
     * hex floats and trailing type suffixes.
     */
    private static Double asNumber(String s)
    {
        boolean digitSeen = false;
        boolean dotSeen = false;

        for(int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);

            if(c >= '0' && c <= '9')
            {
                digitSeen = true;
            }
            else if(c == '.' && !dotSeen)
            {
                dotSeen = true;
            }
            else if((c == '+' || c == '-') && i == 0)
            {
                //Sign is only valid as the leading character.
            }
            else
            {
                return null;
            }
        }

        if(!digitSeen)
        {
            return null;
        }

        try
        {
            return Double.valueOf(s);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * Compares two non-empty strings segment by segment.  Runs of digits are compared as numbers (ignoring leading
     * zeros); all other characters are compared case-insensitively.
     */
    private static int naturalCompare(String a, String b)
    {
        int ia = 0;
        int ib = 0;
        int la = a.length();
        int lb = b.length();

        while(ia < la && ib < lb)
        {
            char ca = a.charAt(ia);
            char cb = b.charAt(ib);
            boolean da = Character.isDigit(ca);
            boolean db = Character.isDigit(cb);

            if(da && db)
            {
                int startA = ia;
                while(ia < la && Character.isDigit(a.charAt(ia)))
                {
                    ia++;
                }

                int startB = ib;
                while(ib < lb && Character.isDigit(b.charAt(ib)))
                {
                    ib++;
                }

                String numA = stripLeadingZeros(a, startA, ia);
                String numB = stripLeadingZeros(b, startB, ib);

                //With leading zeros removed, the longer digit string is the larger number.
                if(numA.length() != numB.length())
                {
                    return numA.length() - numB.length();
                }

                int cmp = numA.compareTo(numB);
                if(cmp != 0)
                {
                    return cmp;
                }
            }
            else if(da != db)
            {
                //A number sorts before a non-number at the same position.
                return da ? -1 : 1;
            }
            else
            {
                char lca = Character.toLowerCase(ca);
                char lcb = Character.toLowerCase(cb);
                if(lca != lcb)
                {
                    return lca - lcb;
                }
                ia++;
                ib++;
            }
        }

        //Shorter string sorts first when everything else is equal.
        return (la - ia) - (lb - ib);
    }

    private static String stripLeadingZeros(String s, int start, int end)
    {
        while(start < end - 1 && s.charAt(start) == '0')
        {
            start++;
        }

        return s.substring(start, end);
    }
}
