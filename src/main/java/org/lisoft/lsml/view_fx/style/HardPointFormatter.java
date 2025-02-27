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
package org.lisoft.lsml.view_fx.style;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.lisoft.lsml.view_fx.util.EquipmentCategory;
import org.lisoft.mwo_data.equipment.JumpJet;
import org.lisoft.mwo_data.mechs.HardPoint;
import org.lisoft.mwo_data.mechs.HardPointType;

/**
 * This class will format a {@link HardPoint} to a {@link Label}.
 *
 * @author Li Song
 */
public class HardPointFormatter {

  public Node format(int aNumHardPoints, HardPointType aHardPointType) {
    Label label = new Label();
    if (aNumHardPoints == 1) {
      label.setText(aHardPointType.shortName());
    } else {
      label.setText(aNumHardPoints + aHardPointType.shortName());
    }

    label.getStyleClass().add(StyleManager.CLASS_HARDPOINT);
    StyleManager.changeStyle(label, EquipmentCategory.classify(aHardPointType));

    return label;
  }

  public Node format(int aNumHardPoints, JumpJet aJumpJet) {
    Label label = new Label();
    if (aNumHardPoints == 1) {
      label.setText("JJ");
    } else {
      label.setText(aNumHardPoints + "JJ");
    }

    label.getStyleClass().add(StyleManager.CLASS_HARDPOINT);
    StyleManager.changeStyle(label, EquipmentCategory.classify(aJumpJet));

    return label;
  }
}
