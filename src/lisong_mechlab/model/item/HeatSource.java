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
package lisong_mechlab.model.item;

import java.util.Collection;

import lisong_mechlab.model.chassi.HardPointType;
import lisong_mechlab.model.quirks.Attribute;
import lisong_mechlab.model.quirks.Modifier;

/**
 * This is a base class for all items that can generate heat.
 * <p>
 * TODO: This class should contain all necessary information for heat calculations including heat period and impulse
 * length etc.
 * 
 * @author Li Song
 */
public class HeatSource extends Item {
    private final Attribute heat;

    protected HeatSource(String aName, String aDesc, String aMwoName, int aMwoId, int aSlots, double aTons,
            HardPointType aHardPointType, int aHP, Faction aFaction, Attribute aHeat) {
        super(aName, aDesc, aMwoName, aMwoId, aSlots, aTons, aHardPointType, aHP, aFaction);
        heat = aHeat;
    }

    /**
     * @param aModifiers
     *            A {@link Collection} of {@link Modifier}s that could affect the heat generation.
     * @return The amount of heat generated by each "action" of this heat source. The action is defined by the subclass.
     */
    public double getHeat(Collection<Modifier> aModifiers) {
        return heat.value(aModifiers);
    }
}
