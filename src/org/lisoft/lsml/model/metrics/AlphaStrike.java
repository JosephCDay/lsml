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

import org.lisoft.lsml.model.item.Weapon;
import org.lisoft.lsml.model.loadout.LoadoutBase;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.model.modifiers.Modifier;

/**
 * This metric calculates the alpha strike for a given {@link LoadoutStandard}.
 * 
 * @author Li Song
 */
public class AlphaStrike extends RangeMetric {
    private final int weaponGroup;

    /**
     * Creates a new {@link AlphaStrike} that calculates the alpha strike damage for a given loadout using all weapons.
     * 
     * @param aLoadout
     *            The loadout to calculate for.
     */
    public AlphaStrike(final LoadoutBase<?> aLoadout) {
        this(aLoadout, -1);
    }

    /**
     * Creates a new {@link AlphaStrike} metric that calculates the alpha strike for the given weapon group.
     * 
     * @param aLoadout
     *            The loadout to calculate for.
     * @param aGroup
     *            The weapon group to calculate for.
     */
    public AlphaStrike(LoadoutBase<?> aLoadout, int aGroup) {
        super(aLoadout);
        weaponGroup = aGroup;
    }

    @Override
    public double calculate(double aRange) {
        double ans = 0;
        Collection<Modifier> modifiers = loadout.getModifiers();

        final Iterable<Weapon> weapons;
        if (weaponGroup < 0) {
            weapons = loadout.items(Weapon.class);
        }
        else {
            weapons = loadout.getWeaponGroups().getWeapons(weaponGroup);
        }

        for (Weapon weapon : weapons) {
            if (weapon.isOffensive())
                ans += weapon.getDamagePerShot() * weapon.getRangeEffectivity(aRange, modifiers);
        }
        return ans;
    }
}
