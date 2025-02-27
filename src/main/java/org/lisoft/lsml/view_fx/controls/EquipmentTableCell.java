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
package org.lisoft.lsml.view_fx.controls;

import java.util.Collection;
import java.util.Optional;
import javafx.scene.control.TreeTableCell;
import org.lisoft.lsml.model.loadout.EquipResult;
import org.lisoft.lsml.model.loadout.Loadout;
import org.lisoft.lsml.view_fx.Settings;
import org.lisoft.lsml.view_fx.style.ItemToolTipFormatter;
import org.lisoft.lsml.view_fx.style.StyleManager;
import org.lisoft.mwo_data.equipment.Consumable;
import org.lisoft.mwo_data.equipment.Item;
import org.lisoft.mwo_data.equipment.MwoObject;
import org.lisoft.mwo_data.modifiers.Modifier;

/**
 * This cell renders info about an {@link Item} in the equipment panel.
 *
 * @author Li Song
 */
public class EquipmentTableCell extends TreeTableCell<Object, String> {
  private final Loadout loadout;
  private final Settings settings;
  private final boolean showIcon;
  private final ItemToolTipFormatter toolTipFormatter;

  public EquipmentTableCell(
      Settings aSettings,
      Loadout aLoadout,
      boolean aShowIcon,
      ItemToolTipFormatter aToolTipFormatter) {
    settings = aSettings;
    loadout = aLoadout;
    showIcon = aShowIcon;
    toolTipFormatter = aToolTipFormatter;

    setOnMouseEntered(
        e -> {
          setTooltip(null);
          getRowItem()
              .ifPresent(
                  aItem -> {
                    final Collection<Modifier> modifiers;
                    if (settings.getBoolean(Settings.UI_SHOW_TOOL_TIP_QUIRKED).getValue()) {
                      modifiers = loadout.getAllModifiers();
                    } else {
                      modifiers = null;
                    }
                    setTooltip(toolTipFormatter.format(aItem, aLoadout, modifiers));
                    getTooltip().setAutoHide(false);
                    // FIXME: Set timeout to infinite once we're on JavaFX9, see:
                    // https://bugs.openjdk.java.net/browse/JDK-8090477
                  });
        });
  }

  @Override
  protected void updateItem(String aText, boolean aEmpty) {
    super.updateItem(aText, aEmpty);
    setText(aText);

    final Object rowItem = getTableRow().getItem();
    if (rowItem instanceof final Item item) {
      if (EquipResult.SUCCESS == loadout.canEquipDirectly(item)) {
        // Directly equippable
        pseudoClassStateChanged(StyleManager.PC_UNEQUIPPABLE, false);
        pseudoClassStateChanged(StyleManager.PC_SMARTPLACEABLE, false);
      } else if (!loadout.getCandidateLocationsForItem(item).isEmpty()) {
        // Might be smart placeable
        pseudoClassStateChanged(StyleManager.PC_UNEQUIPPABLE, false);
        pseudoClassStateChanged(StyleManager.PC_SMARTPLACEABLE, true);
      } else {
        pseudoClassStateChanged(StyleManager.PC_UNEQUIPPABLE, true);
        pseudoClassStateChanged(StyleManager.PC_SMARTPLACEABLE, false);
      }

      if (showIcon) {
        setGraphic(StyleManager.makeIcon(item));
      }
    } else if (rowItem instanceof final Consumable pilotModule) {
      pseudoClassStateChanged(StyleManager.PC_SMARTPLACEABLE, false);
      final boolean canEquip = EquipResult.SUCCESS == loadout.canAddModule(pilotModule);
      pseudoClassStateChanged(StyleManager.PC_UNEQUIPPABLE, !canEquip);
      // if (showIcon) {
      // Region r = new Region();
      // StyleManager.changeIcon(r, pilotModule);
      // setGraphic(r);
      // }
      setGraphic(null);
      if (showIcon) {
        setGraphic(StyleManager.makeIcon(pilotModule));
      }
    } else {
      setContextMenu(null);
      pseudoClassStateChanged(StyleManager.PC_UNEQUIPPABLE, false);
      pseudoClassStateChanged(StyleManager.PC_SMARTPLACEABLE, false);
      if (showIcon) {
        setGraphic(null);
      }
    }
  }

  private Optional<MwoObject> getRowItem() {
    final Object rowItem = getTableRow().getItem();
    if (rowItem instanceof MwoObject) {
      return Optional.of((MwoObject) rowItem);
    }
    return Optional.empty();
  }
}
