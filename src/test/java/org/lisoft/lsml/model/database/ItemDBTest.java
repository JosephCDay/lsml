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
package org.lisoft.lsml.model.database;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.lisoft.lsml.model.database.ItemDB;
import org.lisoft.lsml.model.item.EnergyWeapon;
import org.lisoft.lsml.model.item.TargetingComputer;
import org.lisoft.lsml.model.modifiers.Modifier;

/**
 * This test suite doesn't as much test the behaviour of ItemDB but rather performs checks on the data stored in the
 * ItemDB.
 * 
 * @author Li Song
 */
public class ItemDBTest {

    @Test
    public void testBug505() {
        double expectedMaxRangeMod = 1.04;
        double expectedLongRangeMod = 1.04;

        TargetingComputer tc1 = (TargetingComputer) ItemDB.lookup("TARGETING COMP. MK I");
        EnergyWeapon erllas = (EnergyWeapon) ItemDB.lookup("ER LARGE LASER");

        Collection<Modifier> modifiers = tc1.getModifiers();
        double maxRangeMod = erllas.getRangeMax(modifiers) / erllas.getRangeMax(null);
        double longRangeMod = erllas.getRangeLong(modifiers) / erllas.getRangeLong(null);

        assertEquals(expectedMaxRangeMod, maxRangeMod, 0.00001);
        assertEquals(expectedLongRangeMod, longRangeMod, 0.00001);
    }
}
