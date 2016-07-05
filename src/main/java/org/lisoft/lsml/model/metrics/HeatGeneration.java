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
package org.lisoft.lsml.model.metrics;

import java.util.Collection;

import org.lisoft.lsml.model.item.Engine;
import org.lisoft.lsml.model.item.HeatSource;
import org.lisoft.lsml.model.item.Weapon;
import org.lisoft.lsml.model.loadout.Loadout;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.model.modifiers.Modifier;

/**
 * This {@link Metric} calculates the asymptotic heat generation per second for a {@link LoadoutStandard}.
 * <p>
 * It accounts for the heat generated by the engine at 100% throttle but not for jump jets.
 *
 * @author Li Song
 */
public class HeatGeneration implements Metric {
    private final Loadout loadout;
    private final int group;

    /**
     * Creates a new metric that calculates the total, maximal heat generation.
     *
     * @param aLoadout
     *            The loadout to calculate the metric for.
     */
    public HeatGeneration(final Loadout aLoadout) {
        this(aLoadout, -1);
    }

    /**
     * Creates a new metric that calculates the heat generation for a given weapon group, including fixed heat sources
     * such as engine.
     *
     * @param aLoadout
     *            The loadout to calculate the heat generation for.
     * @param aGroup
     *            The weapon group to calculate for.
     */
    public HeatGeneration(final Loadout aLoadout, final int aGroup) {
        loadout = aLoadout;
        group = aGroup;
    }

    @Override
    public double calculate() {
        double heat = 0;
        final Collection<Modifier> modifiers = loadout.getModifiers();
        for (final HeatSource item : loadout.items(HeatSource.class)) {
            if (item instanceof Weapon && group < 0) {
                heat += ((Weapon) item).getStat("h/s", modifiers);
            }
            else if (item instanceof Engine) {
                heat += item.getHeat(modifiers);
            }
        }
        if (group >= 0) {
            for (final Weapon w : loadout.getWeaponGroups().getWeapons(group, loadout)) {
                heat += w.getStat("h/s", modifiers);
            }
        }
        return heat;
    }
}
