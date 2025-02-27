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
package org.lisoft.lsml.view_fx.util;

import java.util.ArrayList;
import org.lisoft.mwo_data.mechs.Chassis;

/**
 * This class is an arbitrary grouping of chassis into a group.
 *
 * @author Li Song
 */
public class ChassisGroup extends ArrayList<Chassis> {
  private static final long serialVersionUID = -1940531764773538218L;
  private final String groupName;

  /**
   * Creates a new chassis group with the given name.
   *
   * @param aGroupName The name of the group.
   */
  public ChassisGroup(String aGroupName) {
    groupName = aGroupName;
  }

  public String getName() {
    return groupName;
  }

  @Override
  public String toString() {
    return getName();
  }
}
