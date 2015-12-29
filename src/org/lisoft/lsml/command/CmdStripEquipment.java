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
package org.lisoft.lsml.command;

import org.lisoft.lsml.messages.MessageDelivery;
import org.lisoft.lsml.model.item.HeatSink;
import org.lisoft.lsml.model.item.Internal;
import org.lisoft.lsml.model.item.Item;
import org.lisoft.lsml.model.loadout.EquipException;
import org.lisoft.lsml.model.loadout.LoadoutBase;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponentBase;
import org.lisoft.lsml.util.CommandStack.CompositeCommand;

/**
 * This operation removes all {@link Item}s from a {@link LoadoutStandard}.
 * 
 * @author Li Song
 */
public class CmdStripEquipment extends CompositeCommand {
    private final LoadoutBase<?> loadout;

    /**
     * Creates a new strip operation that optionally removes armor, and always removes equipment and modules.
     * 
     * @param aLoadout
     *            The loadout to strip.
     * @param aMessageDelivery
     *            Where to deliver message changes.
     */
    public CmdStripEquipment(LoadoutBase<?> aLoadout, MessageDelivery aMessageDelivery) {
        super("strip mech", aMessageDelivery);
        loadout = aLoadout;
    }

    @Override
    public void buildCommand() throws EquipException {
        for (ConfiguredComponentBase component : loadout.getComponents()) {
            int hsSkipp = component.getEngineHeatSinks();
            for (Item item : component.getItemsEquipped()) {
                if (!(item instanceof Internal)) {
                    if (item instanceof HeatSink) {
                        if (hsSkipp > 0) {
                            hsSkipp--;
                            continue;
                        }
                    }
                    addOp(new CmdRemoveItem(messageBuffer, loadout, component, item));
                }
            }
        }

    }
}
