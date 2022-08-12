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
package org.lisoft.lsml.model.database.gamedata.helpers;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.lisoft.lsml.model.chassi.HardPointType;
import org.lisoft.lsml.model.database.gamedata.Localisation;
import org.lisoft.lsml.model.database.gamedata.QuirkModifiers;
import org.lisoft.lsml.model.item.*;
import org.lisoft.lsml.model.item.WeaponRangeProfile.RangeNode;
import org.lisoft.lsml.model.item.WeaponRangeProfile.RangeNode.InterpolationType;
import org.lisoft.lsml.model.modifiers.Attribute;
import org.lisoft.lsml.model.modifiers.ModifierDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemStatsWeapon extends ItemStats {

    public static class ArtemisTag {
        @XStreamAsAttribute
        public int RestrictedTo;
    }

    /**
     * Note that the <code>&ltRange&gt</code> tag appears in different contexts with different attributes.
     * <p>
     * I can't find a way to instruct XStream to use different classes for different context so w
     *
     * @author Li Song
     */
    @XStreamAlias("Range")
    public static class Range {
        @XStreamAsAttribute
        public double damageModifier;
        @XStreamAsAttribute
        public Double exponent;
        @XStreamAsAttribute
        public String interpolationToNextRange;
        // The following attributes are valid when read in the context of a <RANGE> on a <TARGETINCOMPUTER>
        @XStreamAsAttribute
        public double multiplier;
        // The following attributes are valid when read in the context of a <RANGE> tag on a <RANGES> list in a <WEAPON>
        @XStreamAsAttribute
        public double start;
    }

    public static class WeaponStatsTag extends ItemStatsModuleStats {
        @XStreamAsAttribute
        public double JammedTime;
        @XStreamAsAttribute
        public double JammingChance;
        @XStreamAsAttribute
        public double RampDownDelay;
        @XStreamAsAttribute
        public int ShotsDuringCooldown;
        // although ammoPerShot has an equvalent attribute in PGI files, numFiring is really how the ammo tracks.
        @XStreamAsAttribute
        public int ammoPerShot;
        @XStreamAsAttribute
        public String ammoType;
        @XStreamAsAttribute
        public String artemisAmmoType;
        @XStreamAsAttribute
        public double chargeTime;
        @XStreamAsAttribute
        public double cooldown;
        @XStreamAsAttribute
        public double damage;
        @XStreamAsAttribute
        public double duration;
        @XStreamAsAttribute
        public double heat;
        @XStreamAsAttribute
        public int heatPenaltyID;
        @XStreamAsAttribute
        public double heatdamage;
        @XStreamAsAttribute
        public double heatpenalty;
        @XStreamAsAttribute
        public double impulse;
        @XStreamAsAttribute
        public int isOneShot;
        @XStreamAsAttribute
        public double jamRampDownTime;
        @XStreamAsAttribute
        public double jamRampUpTime;
        @XStreamAsAttribute
        public int minheatpenaltylevel;
        /**
         * The number of ammunition rounds expelled in one shot.
         */
        @XStreamAsAttribute
        public int numFiring;
        @XStreamAsAttribute
        public int volleysize;
        /**
         * The number of projectile in one round of ammo. Fired simultaneously (only LB type AC).
         */
        @XStreamAsAttribute
        public int numPerShot;
        @XStreamAsAttribute
        public String projectileclass;
        @XStreamAsAttribute
        public double rampDownTime;
        @XStreamAsAttribute
        public double rampUpTime;
        @XStreamAsAttribute
        public double rof;
        @XStreamAsAttribute
        public double speed;
        @XStreamAsAttribute
        public double spread;
        @XStreamAsAttribute
        public String type;
        @XStreamAsAttribute
        public double volleydelay;
    }
    public ArtemisTag Artemis;
    @XStreamAsAttribute
    public String HardpointAliases;
    @XStreamAsAttribute
    public int InheritFrom; // Special case handling of inherit from
    public List<Range> Ranges;
    public WeaponStatsTag WeaponStats;

    public Weapon asWeapon(List<ItemStatsWeapon> aWeaponList) throws IOException {
        final int baseType = updateThisFromParentWeapon(aWeaponList);
        final int mwoId = Integer.parseInt(id);
        final int slots = WeaponStats.slots;
        final int roundsPerShot = WeaponStats.numFiring;
        final int projectilesPerRound = WeaponStats.numPerShot > 0 ? WeaponStats.numPerShot : 1;
        final int volleySize = WeaponStats.volleysize > 0 ? WeaponStats.volleysize : roundsPerShot;
        final double damagePerProjectile = determineDamage();
        final double cooldownValue = determineCooldown();
        final double mass = WeaponStats.tons;
        final double hp = WeaponStats.health;
        final boolean isOneShot = WeaponStats.isOneShot != 0;
        // For weapons with cooldown=0, the heat is per second. Convert to per shot as LSML expects.
        final double heatPerShot = WeaponStats.heat * (WeaponStats.cooldown <= 0 ? cooldownValue : 1);
        final String uiName = Localisation.key2string(Loc.nameTag);
        final String uiDesc = Localisation.key2string(Loc.descTag);
        final String mwoName = name;
        final Faction itemFaction = Faction.fromMwo(faction);
        final List<String> selectors = computeSelectors(mwoName);
        final Attribute spread = computeSpreadAttribute(selectors);
        final Attribute heat = new Attribute(heatPerShot, selectors, ModifierDescription.SPEC_WEAPON_HEAT);

        int ghostHeatGroupId;
        double ghostHeatMultiplier;
        final Attribute ghostHeatFreeAlpha;
        if (WeaponStats.minheatpenaltylevel != 0) {
            ghostHeatGroupId = WeaponStats.heatPenaltyID;
            ghostHeatMultiplier = WeaponStats.heatpenalty;
            ghostHeatFreeAlpha = new Attribute(WeaponStats.minheatpenaltylevel - 1, selectors,
                                               ModifierDescription.SPEC_WEAPON_MAX_FREE_ALPHA);
        } else {
            ghostHeatGroupId = -1;
            ghostHeatMultiplier = 0;
            ghostHeatFreeAlpha = new Attribute(-1, selectors, ModifierDescription.SPEC_WEAPON_MAX_FREE_ALPHA);
        }

        final List<RangeNode> rangeNodes = Ranges.stream().map(r -> new RangeNode(
                                                         new Attribute(r.start, selectors, ModifierDescription.SPEC_WEAPON_RANGE),
                                                         InterpolationType.fromMwo(r.interpolationToNextRange), r.damageModifier, r.exponent))
                                                 .collect(Collectors.toList());

        final Attribute projectileSpeed = new Attribute(computeSpeed(), selectors,
                                                        ModifierDescription.SPEC_WEAPON_PROJECTILE_SPEED);
        final Attribute cooldown = new Attribute(cooldownValue, selectors, ModifierDescription.SPEC_WEAPON_COOL_DOWN);
        final WeaponRangeProfile rangeProfile = new WeaponRangeProfile(spread, rangeNodes);

        switch (HardPointType.fromMwoType(WeaponStats.type)) {
            case AMS:
                return new AmmoWeapon(
                        // Item Arguments
                        uiName, uiDesc, mwoName, mwoId, slots, mass, HardPointType.AMS, hp, itemFaction,
                        // HeatSource Arguments
                        heat,
                        // Weapon Arguments
                        cooldown, rangeProfile, roundsPerShot, volleySize, damagePerProjectile, projectilesPerRound,
                        projectileSpeed, ghostHeatGroupId, ghostHeatMultiplier, ghostHeatFreeAlpha,
                        WeaponStats.volleydelay, WeaponStats.impulse,
                        // AmmoWeapon Arguments
                        getAmmoType(), isOneShot, roundsPerShot);
            case BALLISTIC:
                final Attribute jamChanceAttrib = new Attribute(WeaponStats.JammingChance, selectors,
                                                                ModifierDescription.SPEC_WEAPON_JAM_PROBABILITY);
                final Attribute jamTimeAttrib = new Attribute(WeaponStats.JammedTime, selectors,
                                                              ModifierDescription.SPEC_WEAPON_JAM_DURATION);
                final Attribute jamRampDownTime = new Attribute(WeaponStats.jamRampDownTime, selectors,
                                                                ModifierDescription.SPEC_WEAPON_JAM_RAMP_DOWN_TIME);

                return new BallisticWeapon(
                        // Item Arguments
                        uiName, uiDesc, mwoName, mwoId, slots, mass, hp, itemFaction,
                        // HeatSource Arguments
                        heat,
                        // Weapon Arguments
                        cooldown, rangeProfile, roundsPerShot, damagePerProjectile, projectilesPerRound, 
                        projectileSpeed, ghostHeatGroupId, ghostHeatMultiplier, ghostHeatFreeAlpha,
                        WeaponStats.volleydelay, WeaponStats.impulse,
                        // AmmoWeapon Arguments
                        getAmmoType(), isOneShot, roundsPerShot,
                        // BallisticWeapon Arguments
                        jamChanceAttrib, jamTimeAttrib, WeaponStats.ShotsDuringCooldown, WeaponStats.chargeTime,
                        WeaponStats.rampUpTime, WeaponStats.rampDownTime, WeaponStats.RampDownDelay,
                        WeaponStats.jamRampUpTime, jamRampDownTime);
            case ENERGY:
                final Attribute burntime = new Attribute(
                        WeaponStats.duration < 0 ? Double.POSITIVE_INFINITY : WeaponStats.duration, selectors,
                        ModifierDescription.SPEC_WEAPON_DURATION);
                return new EnergyWeapon(
                        // Item Arguments
                        uiName, uiDesc, mwoName, mwoId, slots, mass, hp, itemFaction,
                        // HeatSource Arguments
                        heat,
                        // Weapon Arguments
                        cooldown, rangeProfile, roundsPerShot, damagePerProjectile, projectilesPerRound,
                        projectileSpeed, ghostHeatGroupId, ghostHeatMultiplier, ghostHeatFreeAlpha,
                        WeaponStats.volleydelay, WeaponStats.impulse,
                        // EnergyWeapon Arguments
                        burntime);
            case MISSILE:
                final int requiredGuidance;
                if (null != Artemis) {
                    requiredGuidance = Artemis.RestrictedTo;
                } else {
                    requiredGuidance = -1;
                }

                final int baseItemId = baseType == -1 ? requiredGuidance != -1 ? mwoId : -1 : baseType;
                return new MissileWeapon(
                        // Item Arguments
                        uiName, uiDesc, mwoName, mwoId, slots, mass, hp, itemFaction,
                        // HeatSource Arguments
                        heat,
                        // Weapon Arguments
                        cooldown, rangeProfile, roundsPerShot, volleySize, damagePerProjectile, projectilesPerRound,
                        projectileSpeed, ghostHeatGroupId, ghostHeatMultiplier, ghostHeatFreeAlpha,
                        WeaponStats.volleydelay, WeaponStats.impulse,
                        // AmmoWeapon Arguments
                        getAmmoType(), isOneShot, roundsPerShot, 
                        // MissileWeapon Arguments
                        requiredGuidance, baseItemId);
            case ECM: // Fall through, not a weapon
            case NONE: // Fall through, not a weapon
            default:
                throw new IOException("Unknown value for type field in ItemStatsXML. Please update the program!");
        }
    }

    public boolean isUsable() {
        // Stupid dropshiplargepulselaser and testing machinegun screwing stuff up
        return !id.equals("1998") && !id.equals("1999");
    }

    private List<String> computeSelectors(final String mwoName) {
        final List<String> selectors = new ArrayList<>(Arrays.asList(HardpointAliases.toLowerCase().split(",")));
        selectors.add(QuirkModifiers.SPECIFIC_ITEM_PREFIX + mwoName.toLowerCase());
        return selectors;
    }

    private double computeSpeed() {
        return WeaponStats.speed == 0 ? Double.POSITIVE_INFINITY : WeaponStats.speed;
    }

    private Attribute computeSpreadAttribute(final List<String> selectors) {
        final Attribute spread;
        // For now, don't use the spread attribute on javelin type weapons #691.
        if (WeaponStats.spread > 0 && !"javelin".equalsIgnoreCase(WeaponStats.projectileclass)) {
            spread = new Attribute(WeaponStats.spread, selectors, ModifierDescription.SPEC_WEAPON_SPREAD);
        } else {
            spread = null;
        }
        return spread;
    }

    private double determineCooldown() {
        if (WeaponStats.cooldown <= 0.0) {
            // All weapons have a cooldown, some of them are zero. If it's zero there's usually a rate of fire
            // attribute "rof". But when that's not present, damage and heat is per second.
            if (WeaponStats.rof > 0.0) {
                return 1.0 / WeaponStats.rof;
            } else {
                return 0.10;
            }
        }
        return WeaponStats.cooldown;
    }

    private double determineDamage() {
        if (WeaponStats.cooldown <= 0.0 && WeaponStats.rof <= 0.0) {
            // Flamers and TAG have damage per second, normalize this
            return WeaponStats.damage * determineCooldown();
        }
        return WeaponStats.damage;
    }

    private String getAmmoType() {
        final String regularAmmo = WeaponStats.ammoType;
        if (WeaponStats.artemisAmmoType == null) {
            return regularAmmo;
        }

        if (Artemis == null) {
            return regularAmmo;
        }

        if (Artemis.RestrictedTo == 3051) {
            return regularAmmo;
        }
        return WeaponStats.artemisAmmoType;
    }

    private int updateThisFromParentWeapon(List<ItemStatsWeapon> aWeaponList) throws IOException {
        int baseType = -1;
        if (InheritFrom > 0) {
            baseType = InheritFrom;
            for (final ItemStatsWeapon w : aWeaponList) {
                try {
                    if (Integer.parseInt(w.id) == InheritFrom) {
                        WeaponStats = w.WeaponStats;
                        Ranges = w.Ranges;
                        if (Loc.descTag == null) {
                            Loc.descTag = w.Loc.descTag;
                        }
                        break;
                    }
                } catch (final NumberFormatException e) {
                    continue;
                }
            }
            if (WeaponStats == null) {
                throw new IOException(
                        "Unable to find referenced item in \"inherit statement from clause\" for: " + name);
            }
        }
        return baseType;
    }
}
