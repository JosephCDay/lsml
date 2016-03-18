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
import org.lisoft.lsml.model.NamedObject;
import org.lisoft.lsml.model.garage.GarageDirectory;
import org.lisoft.lsml.model.loadout.EquipException;
import org.lisoft.lsml.util.CommandStack.Command;
import org.lisoft.lsml.util.CommandStack.CompositeCommand;

/**
 * This {@link Command} will take the contents from the source {@link GarageDirectory} and merge them into the
 * destination such that all folders and values from the source are added to the destination unless they already exist.
 * 
 * Folders are compared as case insensitive and values are compared by <code>equals(Object)</code>.
 * 
 * @author Li Song
 * @param <T>
 *            The type of the values in the garage directories to merge.
 * 
 */
public class CmdMergeGarageDirectories<T extends NamedObject> extends CompositeCommand {
    private final GarageDirectory<T> dst;
    private final GarageDirectory<T> src;

    /**
     * @param aDescription
     * @param aMessageTarget
     * @param aSrcRoot
     * @param aDstRoot
     */
    public CmdMergeGarageDirectories(String aDescription, MessageDelivery aMessageTarget, GarageDirectory<T> aDstRoot,
            GarageDirectory<T> aSrcRoot) {
        super(aDescription, aMessageTarget);
        dst = aDstRoot;
        src = aSrcRoot;
    }

    @Override
    protected void buildCommand() throws EquipException {
        merge(dst, src);
    }

    void merge(GarageDirectory<T> aDst, GarageDirectory<T> aSrc) {
        for (T value : aSrc.getValues()) {
            if (!aDst.getValues().contains(value)) {
                addOp(new CmdAddToGarage<>(messageBuffer, aDst, value));
            }
        }

        for (GarageDirectory<T> srcChild : aSrc.getDirectories()) {
            boolean found = false;
            for (GarageDirectory<T> dstChild : aDst.getDirectories()) {
                if (dstChild.getName().equals(srcChild.getName())) {
                    merge(dstChild, srcChild);
                    found = true;
                    break;
                }
            }
            if (!found) {
                GarageDirectory<T> dstChild = new GarageDirectory<>(srcChild.getName());
                addOp(new CmdAddGarageDirectory<>(messageBuffer, dstChild, aDst));
                merge(dstChild, srcChild);
            }
        }
    }
}
