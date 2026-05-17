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

package io.github.dsheirer.gui.control;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text formatter for integer values that constrains values to specified minimum and maximum valid values.
 * Uses unsigned logic for values up to 4294967295.
 */
public class IntegerFormatter extends TextFormatter<Integer>
{
    private static final Logger mLog = LoggerFactory.getLogger(IntegerFormatter.class);

    /**
     * Constructs an instance
     * @param minimum allowed value
     * @param maximum allowed value (can be negative for unsigned representation like 0xFFFFFFFF)
     */
    public IntegerFormatter(int minimum, int maximum)
    {
        super(new UnsignedIntegerStringConverter(), null, new IntegerFilter(minimum, maximum));
    }

    public static class UnsignedIntegerStringConverter extends StringConverter<Integer> {
        @Override
        public String toString(Integer value) {
            if (value == null) {
                return "";
            }
            return Integer.toUnsignedString(value);
        }

        @Override
        public Integer fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseUnsignedInt(value.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Formatted text change filter that only allows characters where the converted decimal value
     * is also constrained within minimum and maximum valid values.
     */
    public static class IntegerFilter implements UnaryOperator<Change>
    {
        private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\-?[0-9].*");
        private int mMinimum;
        private int mMaximum;

        public IntegerFilter(int minimum, int maximum)
        {
            mMinimum = minimum;
            mMaximum = maximum;
        }

        /**
         * Indicates if the value argument is parsable as an integer, or is empty or null.
         */
        private boolean isValid(String value)
        {
            if(value == null || value.isEmpty())
            {
                return true;
            }

            try
            {
                int parsed = Integer.parseUnsignedInt(value);
                return Integer.compareUnsigned(mMinimum, parsed) <= 0 && Integer.compareUnsigned(parsed, mMaximum) <= 0;
            }
            catch(Exception e)
            {
                //no-op
            }

            return false;
        }

        @Override
        public Change apply(Change change)
        {
            //Only validate if the user added text to the control.  Otherwise, allow it to go through
            if(!change.getText().equals(""))
            {
                String updatedText = change.getControlNewText();

                if(updatedText == null || updatedText.isEmpty())
                {
                    return change;
                }
                
                // don't validate yet if input is only a minus sign
                if(updatedText.equals("-"))
                {
                    return change;
                }

                if(!DECIMAL_PATTERN.matcher(updatedText).matches() || !isValid(updatedText))
                {
                    return null;
                }
            }

            return change;
        }
    }
}
