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
package org.lisoft.lsml.model.loadout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lisoft.lsml.model.chassi.Chassis;
import org.lisoft.lsml.model.chassi.Location;
import org.lisoft.lsml.model.datacache.ChassisDB;
import org.lisoft.lsml.model.datacache.OmniPodDB;
import org.lisoft.lsml.model.datacache.UpgradeDB;
import org.lisoft.lsml.model.item.Item;
import org.lisoft.lsml.model.upgrades.ArmorUpgrade;
import org.lisoft.lsml.model.upgrades.GuidanceUpgrade;
import org.lisoft.lsml.model.upgrades.HeatSinkUpgrade;
import org.lisoft.lsml.model.upgrades.StructureUpgrade;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This immutable class defines a stock loadout pattern that can be used for loading stock on a {@link Loadout}.
 * 
 * @author Li Song
 */
@XStreamAlias("StockLoadout")
public class StockLoadout {
    /**
     * This immutable class defines a component in a stock loadout.
     * 
     * @author Li Song
     */
    @XStreamAlias("Component")
    public static class StockComponent {
        public enum ActuatorState {
            NONE, LAA, BOTH;

            public static ActuatorState fromMwoString(String aString) {
                if (aString == null || aString.isEmpty()) {
                    return null;
                }

                switch (aString.toLowerCase()) {
                    case "eactuatorstate_none":
                        return ActuatorState.NONE;
                    case "eactuatorstate_handsandarms":
                        return ActuatorState.BOTH;
                    case "eactuatorstate_armsonly":
                        return ActuatorState.LAA;
                    default:
                        throw new IllegalArgumentException("Unknown actuator state: [" + aString + "]");
                }
            }
        }

        @XStreamAsAttribute
        private final Location      location;
        @XStreamAsAttribute
        private final Integer       armorFront;
        @XStreamAsAttribute
        private final Integer       armorBack;
        @XStreamAsAttribute
        private final Integer       omniPod;
        @XStreamImplicit
        private final List<Integer> items;
        @XStreamAsAttribute
        private final ActuatorState actuatorState;

        @Override
        public String toString() {
            return location.shortName() + " " + armorFront + "/" + armorBack + " (pod: "
                    + OmniPodDB.lookup(omniPod.intValue()) + ") " + items;
        }

        /**
         * Creates a new {@link StockComponent}.
         * 
         * @param aPart
         *            The {@link Location} that this {@link StockComponent} is for.
         * @param aFront
         *            The front armor (or total armor if one sided).
         * @param aBack
         *            The back armor (must be zero if one sided).
         * @param aItems
         *            A {@link List} of items in the component.
         * @param aOmniPod
         *            The ID of the omnipod to use (or 0 if stock/none)
         * @param aActuatorState
         *            The state of the actuators for this component, may be <code>null</code>.
         */
        public StockComponent(Location aPart, int aFront, int aBack, List<Integer> aItems, Integer aOmniPod,
                ActuatorState aActuatorState) {
            location = aPart;
            armorFront = aFront;
            if (location.isTwoSided()) {
                armorBack = aBack;
            }
            else {
                armorBack = null;
            }
            items = Collections.unmodifiableList(aItems);
            omniPod = aOmniPod;
            actuatorState = aActuatorState;
        }

        /**
         * @return The {@link Location} that defines this {@link StockComponent}.
         */
        public Location getLocation() {
            return location;
        }

        /**
         * @return The front armor of this {@link StockComponent}. Or total armor if the component is one sided.
         */
        public int getArmorFront() {
            return armorFront;
        }

        /**
         * @return The back armor of this {@link StockComponent}. Will throw if the component is one sided.
         */
        public int getArmorBack() {
            return armorBack;
        }

        /**
         * @return The {@link Item} IDs that are housed in this {@link StockComponent}.
         */
        public List<Integer> getItems() {
            if (items == null) {
                return new ArrayList<>();
            }
            return items;
        }

        /**
         * @return The omnipod to use for this component or 0 if default/none.
         */
        public Integer getOmniPod() {
            return omniPod;
        }

        /**
         * @return The actuator state for this {@link StockComponent} or <code>null</code> if not applicable.
         */
        public ActuatorState getActuatorState() {
            return actuatorState;
        }
    }

    @XStreamImplicit
    private final List<StockComponent> components;

    @XStreamAsAttribute
    private final Integer              armorId;
    @XStreamAsAttribute
    private final Integer              structureId;
    @XStreamAsAttribute
    private final Integer              heatsinkId;
    @XStreamAsAttribute
    private final Integer              guidanceId;
    @XStreamAsAttribute
    private final Integer              chassisId;

    /**
     * Creates a new {@link StockLoadout}
     * 
     * @param aChassisId
     *            The ID of the chassis that this loadout was originally for.
     * @param aComponents
     *            The list of {@link StockComponent} that make up this {@link StockLoadout}.
     * @param aArmor
     *            The armor upgrade type.
     * @param aStructure
     *            The structure upgrade type.
     * @param aHeatSink
     *            The heat sink upgrade type.
     * @param aGuidance
     *            The guidance upgrade type.
     */
    public StockLoadout(int aChassisId, List<StockComponent> aComponents, int aArmor, int aStructure, int aHeatSink,
            int aGuidance) {
        chassisId = aChassisId;
        armorId = aArmor;
        structureId = aStructure;
        heatsinkId = aHeatSink;
        guidanceId = aGuidance;
        components = Collections.unmodifiableList(aComponents);
    }

    /**
     * @return The {@link Chassis} for this {@link StockLoadout}.
     */
    public Chassis getChassis() {
        return ChassisDB.lookup(chassisId);
    }

    /**
     * @return The {@link ArmorUpgrade} for this {@link StockLoadout}.
     */
    public ArmorUpgrade getArmorType() {
        return (ArmorUpgrade) UpgradeDB.lookup(armorId);
    }

    /**
     * @return The {@link StructureUpgrade} for this {@link StockLoadout}.
     */
    public StructureUpgrade getStructureType() {
        return (StructureUpgrade) UpgradeDB.lookup(structureId);
    }

    /**
     * @return The {@link HeatSinkUpgrade} for this {@link StockLoadout}.
     */
    public HeatSinkUpgrade getHeatSinkType() {
        return (HeatSinkUpgrade) UpgradeDB.lookup(heatsinkId);
    }

    /**
     * @return The {@link GuidanceUpgrade} for this {@link StockLoadout}.
     */
    public GuidanceUpgrade getGuidanceType() {
        return (GuidanceUpgrade) UpgradeDB.lookup(guidanceId);
    }

    /**
     * @return The {@link StockComponent}s in this {@link StockLoadout}.
     */
    public List<StockComponent> getComponents() {
        return components;
    }
}
