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
package lisong_mechlab.model.loadout;

import lisong_mechlab.model.item.PilotModule;
import lisong_mechlab.util.OperationStack.Operation;
import lisong_mechlab.util.message.MessageXBar;

/**
 * This {@link Operation} adds a module to a loadout.
 * 
 * @author Li Song
 */
public class OpAddModule extends Operation {
	private final PilotModule			module;
	private final LoadoutBase<?>		loadout;
	private final transient MessageXBar	xBar;

	/**
	 * Creates a new {@link OpAddModule}.
	 * 
	 * @param aXBar
	 *            The {@link MessageXBar} to signal changes to the loadout on.
	 * @param aLoadout
	 *            The {@link LoadoutBase} to add the module to.
	 * @param aLookup
	 *            The {@link PilotModule} to add.
	 */
	public OpAddModule(MessageXBar aXBar, LoadoutBase<?> aLoadout, PilotModule aLookup) {
		module = aLookup;
		loadout = aLoadout;
		xBar = aXBar;
	}

	@Override
	public String describe() {
		return "add " + module + " to " + loadout;
	}

	@Override
	protected void apply() {
		if (!loadout.canAddModule(module))
			throw new IllegalArgumentException("Can't add module to loadout!");
		loadout.addModule(module);

		if (xBar != null) {
			xBar.post(new LoadoutMessage(loadout, LoadoutMessage.Type.MODULES_CHANGED));
		}
	}

	@Override
	protected void undo() {
		loadout.removeModule(module);
		if (xBar != null) {
			xBar.post(new LoadoutMessage(loadout, LoadoutMessage.Type.MODULES_CHANGED));
		}
	}
}
