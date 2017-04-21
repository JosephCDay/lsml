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
package org.lisoft.lsml.application;

import javax.inject.Named;
import javax.inject.Singleton;

import org.lisoft.lsml.application.modules.datalayer.HeadlessDataModule;
import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.model.datacache.DatabaseProvider;
import org.lisoft.lsml.model.export.Base64LoadoutCoder;
import org.lisoft.lsml.model.loadout.LoadoutFactory;
import org.lisoft.lsml.view_fx.Settings;

import dagger.Component;

/**
 * This dagger {@link Component} provides the services necessary for a headless LSML application (unit tests).
 *
 * @author Li Song
 */
@Singleton
@Component(modules = { HeadlessDataModule.class })
public interface HeadlessApplicationComponent {
    Base64LoadoutCoder loadoutCoder();

    LoadoutFactory loadoutFactory();

    @Named("global")
    MessageXBar messageXBar();

    DatabaseProvider mwoDatabaseProvider();

    Settings settings();
}
