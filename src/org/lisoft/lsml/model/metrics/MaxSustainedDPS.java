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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lisoft.lsml.model.item.Engine;
import org.lisoft.lsml.model.item.Weapon;
import org.lisoft.lsml.model.loadout.LoadoutBase;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.model.modifiers.Modifier;

/**
 * This {@link Metric} calculates the maximal DPS that a {@link LoadoutStandard} can sustain indefinitely assuming that
 * the pilot is moving at full throttle.
 * 
 * @author Li Song
 */
public class MaxSustainedDPS extends RangeMetric {
    private final HeatDissipation dissipation;
    private final int             weaponGroup;

    /**
     * Creates a new {@link MaxSustainedDPS} that calculates the maximal possible sustained DPS for the given loadout
     * using all weapons.
     * 
     * @param aLoadout
     *            The loadout to calculate for.
     * @param aHeatDissipation
     *            A metric that calculates the effective heat dissipation for the loadout.
     */
    public MaxSustainedDPS(final LoadoutBase<?> aLoadout, final HeatDissipation aHeatDissipation) {
        this(aLoadout, aHeatDissipation, -1);
    }

    /**
     * Creates a new {@link MaxSustainedDPS} that calculates the maximal possible sustained DPS for the given weapon
     * group.
     * 
     * @param aLoadout
     *            The loadout to calculate for.
     * @param aHeatDissipation
     *            A metric that calculates the effective heat dissipation for the loadout.
     * @param aGroup
     *            The weapon group to calculate the metric for.
     */
    public MaxSustainedDPS(final LoadoutBase<?> aLoadout, final HeatDissipation aHeatDissipation, int aGroup) {
        super(aLoadout);
        dissipation = aHeatDissipation;
        weaponGroup = aGroup;
    }

    @Override
    public double calculate(double aRange) {
        double ans = 0.0;
        Map<Weapon, Double> dd = getWeaponRatios(aRange);
        Collection<Modifier> modifiers = loadout.getModifiers();
        for (Map.Entry<Weapon, Double> entry : dd.entrySet()) {
            Weapon weapon = entry.getKey();
            double ratio = entry.getValue();
            double rangeEffectivity = weapon.getRangeEffectivity(aRange, modifiers);
            ans += rangeEffectivity * weapon.getStat("d/s", modifiers) * ratio;
        }
        return ans;
    }

    /**
     * Calculates the ratio with each weapon should be fired to obtain the maximal sustained DPS. A ratio of 0.0 means
     * the weapon is never fired and a ratio of 0.5 means the weapon is fired every 2 cool downs and a ratio of 1.0
     * means the weapon is fired every time it is available. This method assumes that the engine is at full throttle.
     * 
     * @param aRange
     *            The range to calculate for.
     * @return A {@link Map} with {@link Weapon} as key and a {@link Double} as value representing a % of how often the
     *         weapon is used.
     */
    public Map<Weapon, Double> getWeaponRatios(final double aRange) {
        final Collection<Modifier> modifiers = loadout.getModifiers();
        double heatleft = dissipation.calculate();
        Engine engine = loadout.getEngine();
        if (null != engine) {
            heatleft -= engine.getHeat(modifiers);
        }

        List<Weapon> weapons = new ArrayList<>(15);

        final Iterable<Weapon> weaponsToUse;
        if (weaponGroup < 0) {
            weaponsToUse = loadout.items(Weapon.class);
        }
        else {
            weaponsToUse = loadout.getWeaponGroups().getWeapons(weaponGroup, loadout);
        }

        for (Weapon weapon : weaponsToUse) {
            if (weapon.isOffensive()) {
                weapons.add(weapon);
            }
        }
        if (aRange >= 0) {
            Collections.sort(weapons, new Comparator<Weapon>() {
                @Override
                public int compare(Weapon aO1, Weapon aO2) {
                    // Note: D/H == DPS / HPS so we're ordering by highest dps per hps.
                    double dps2 = aO2.getRangeEffectivity(aRange, modifiers) * aO2.getStat("d/h", modifiers);
                    double dps1 = aO1.getRangeEffectivity(aRange, modifiers) * aO1.getStat("d/h", modifiers);
                    if (aO1.getRangeMax(modifiers) < aRange)
                        dps1 = 0;
                    if (aO2.getRangeMax(modifiers) < aRange)
                        dps2 = 0;
                    return Double.compare(dps2, dps1);
                }
            });
        }
        else {
            Collections.sort(weapons, new Comparator<Weapon>() {
                @Override
                public int compare(Weapon aO1, Weapon aO2) {
                    return Double.compare(aO2.getStat("d/h", modifiers), aO1.getStat("d/h", modifiers));
                }
            });
        }

        Map<Weapon, Double> ans = new HashMap<>();
        while (!weapons.isEmpty()) {
            Weapon weapon = weapons.remove(0);
            final double heat = weapon.getStat("h/s", modifiers);
            final double ratio;

            if (heatleft == 0) {
                ratio = 0;
            }
            else if (heat < heatleft) {
                ratio = 1.0;
                heatleft -= heat;
            }
            else {
                ratio = heatleft / heat;
                heatleft = 0;
            }

            if (ans.containsKey(weapon))
                ans.put(weapon, Double.valueOf(ans.get(weapon).doubleValue() + ratio));
            else
                ans.put(weapon, Double.valueOf(ratio));
        }
        return ans;
    }
}
