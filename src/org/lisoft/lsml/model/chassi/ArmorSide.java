/*
 * @formatter:off
 * Li Song Mechlab - A 'mech building tool for PGI's MechWarrior: Online.
 * Copyright (C) 2013  Li Song
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
//@formatter:on
package org.lisoft.lsml.model.chassi;

import java.util.Arrays;
import java.util.List;

/**
 * This enumeration names the sides of an {@link ComponentStandard} for use with armor.
 * 
 * @author Li Song
 */
public enum ArmorSide {
    ONLY, FRONT, BACK;

    private static List<ArmorSide> BOTH_SIDES = Arrays.asList(FRONT, BACK);
    private static List<ArmorSide> ONLY_SIDE  = Arrays.asList(ONLY);

    public static Iterable<ArmorSide> allSides(Component aComponent) {
        if (aComponent.getLocation().isTwoSided()) {
            return BOTH_SIDES;
        }
        return ONLY_SIDE;
    }
}
