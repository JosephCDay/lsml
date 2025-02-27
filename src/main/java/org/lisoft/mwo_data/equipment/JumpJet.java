/*
 * Li Song Mechlab - A 'mech building tool for PGI's MechWarrior: Online.
 * Copyright (C) 2013-2023  Li Song
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
package org.lisoft.mwo_data.equipment;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.util.List;
import org.lisoft.mwo_data.Faction;
import org.lisoft.mwo_data.mechs.ChassisClass;
import org.lisoft.mwo_data.mechs.HardPointType;
import org.lisoft.mwo_data.mechs.Location;

public class JumpJet extends Module {
  @XStreamAsAttribute private final double boost_z;
  @XStreamAsAttribute private final double duration;
  @XStreamAsAttribute private final double heat;
  @XStreamAsAttribute private final double maxTons;
  @XStreamAsAttribute private final double minTons;

  public JumpJet(
      String aName,
      String aDesc,
      String aMwoName,
      int aMwoId,
      int aSlots,
      double aTons,
      HardPointType aHardpointType,
      double aHP,
      Faction aFaction,
      List<Location> aAllowedLocations,
      List<ChassisClass> aAllowedChassisClasses,
      double aMinTons,
      double aMaxTons,
      double aBoost,
      double aDuration,
      double aHeat) {
    super(
        aName,
        aDesc,
        aMwoName,
        aMwoId,
        aSlots,
        aTons,
        aHardpointType,
        aHP,
        aFaction,
        aAllowedLocations,
        aAllowedChassisClasses,
        null);

    minTons = aMinTons;
    maxTons = aMaxTons;
    boost_z = aBoost;
    duration = aDuration;
    heat = aHeat;
    // TODO: Parse extra heat and make use of it somehow.
  }

  public double getDuration() {
    return duration;
  }

  public double getForce() {
    return boost_z;
  }

  public double getJumpHeat() {
    return heat;
  }

  public double getMaxTons() {
    return maxTons;
  }

  public double getMinTons() {
    return minTons;
  }
}
