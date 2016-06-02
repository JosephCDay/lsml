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
package org.lisoft.lsml.model.upgrades;

import org.lisoft.lsml.model.item.Faction;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents an upgrade to a 'mechs armour.
 *
 * @author Li Song
 */
public class ArmourUpgrade extends Upgrade {
    @XStreamAsAttribute
    private final int slots;
    @XStreamAsAttribute
    private final double armourPerTon;

    public ArmourUpgrade(String aName, String aDescription, int aMwoId, Faction aFaction, int aExtraSlots,
            double aArmourPerTon) {
        super(aName, aDescription, aMwoId, aFaction);
        slots = aExtraSlots;
        armourPerTon = aArmourPerTon;
    }

    /**
     * Calculates the mass of the given amount of armour points.
     *
     * @param aArmour
     *            The amount of armour.
     * @return The mass of the given armour amount.
     */
    public double getArmourMass(int aArmour) {
        return aArmour / armourPerTon;
    }

    /**
     * @return The number of points of armour per ton from this armour type.
     */
    public double getArmourPerTon() {
        return armourPerTon;
    }

    /**
     * @return The number of extra slots required by this upgrade.
     */
    public int getExtraSlots() {
        return slots;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.ARMOUR;
    }
}
