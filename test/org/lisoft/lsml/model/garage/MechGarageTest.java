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
package org.lisoft.lsml.model.garage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lisoft.lsml.command.CmdAddModule;
import org.lisoft.lsml.command.CmdLoadStock;
import org.lisoft.lsml.messages.GarageMessage;
import org.lisoft.lsml.messages.GarageMessage.Type;
import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.model.chassi.Location;
import org.lisoft.lsml.model.datacache.ChassisDB;
import org.lisoft.lsml.model.datacache.ItemDB;
import org.lisoft.lsml.model.datacache.PilotModuleDB;
import org.lisoft.lsml.model.item.Faction;
import org.lisoft.lsml.model.loadout.DefaultLoadoutFactory;
import org.lisoft.lsml.model.loadout.LoadoutBase;
import org.lisoft.lsml.model.loadout.LoadoutOmniMech;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.util.CommandStack;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.thoughtworks.xstream.XStream;

public class MechGarageTest {
    File        testFile = null;

    @Mock
    MessageXBar xBar;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testFile = new File("test_mechgarage_" + Math.random() + ".xml");
    }

    @After
    public void teardown() {
        testFile.delete();
    }

    /**
     * Default constructing a mech garage gives an unnamed, empty garage.
     */
    @Test
    public void testMechGarage() {
        // Execute
        MechGarage cut = new MechGarage(xBar);

        // Verify
        verify(xBar).post(new GarageMessage(GarageMessage.Type.NewGarage, cut));

        assertTrue(cut.getDropShips().isEmpty());
        assertTrue(cut.getMechs().isEmpty());
        assertNull(cut.getFile());
    }

    /**
     * Loading an empty garage shall produce an empty garage with the correct file path set.
     * 
     * @throws IOException
     */
    @Test
    public void testOpen() throws IOException {
        // Setup
        MechGarage savedGarage = new MechGarage(xBar);
        savedGarage.saveas(testFile);
        reset(xBar);

        // Execute
        MechGarage cut = MechGarage.open(testFile, xBar);

        // Verify
        verify(xBar).post(new GarageMessage(GarageMessage.Type.NewGarage, cut));

        assertTrue(cut.getDropShips().isEmpty());
        assertTrue(cut.getMechs().isEmpty());
        assertSame(testFile, cut.getFile());
    }

    /**
     * Saving a mech garage that has not been loaded or saveas-ed before shall throw an error as it has no associated
     * file name.
     */
    @Test
    public void testSaveWithoutName() {
        // Setup
        MechGarage cut = new MechGarage(xBar);
        reset(xBar);

        // Execute
        try {
            cut.save();
            fail();
        }

        // Verify
        catch (IOException exception) {/* Expected exception */
        }
        verifyZeroInteractions(xBar);
    }

    /**
     * Attempting to use {@link MechGarage#saveas(File)} on a file that already exist shall throw without editing the
     * file.
     * 
     * @throws IOException
     *             Shouldn't be thrown.
     */
    @Test
    public void testSaveOverwrite() throws IOException {
        // Setup
        MechGarage cut = new MechGarage(xBar);
        cut.saveas(testFile);
        testFile.setLastModified(0);
        reset(xBar);

        // Execute
        try {
            cut.saveas(testFile); // File already exists
            fail(); // Must throw!
        }

        // Verify
        catch (IOException e) {
            assertEquals(0, testFile.lastModified()); // Must not have been modified
        }
        verifyZeroInteractions(xBar);
    }

    /**
     * This is in reality an integration test as the test has to verify that the loadouts and dropships can be properly
     * serialised.
     * 
     * {@link MechGarage#saveas(File)} shall produce a file that can be subsequently
     * {@link MechGarage#open(File, MessageXBar)}ed to restore the contents of the garage before the call to
     * {@link MechGarage#saveas(File)}
     * 
     * @throws Exception
     *             Shouldn't be thrown.
     */
    @Test
    public void testSaveAsOpen() throws Exception {
        // Setup
        LoadoutBase<?> lo1 = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("as7-d-dc"));
        LoadoutBase<?> lo2 = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("as7-k"));
        LoadoutBase<?> lo3 = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("nva-prime"));
        LoadoutBase<?> lo4 = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("tbr-c"));

        DropShip ds1 = new DropShip(Faction.InnerSphere);
        DropShip ds2 = new DropShip(Faction.Clan);
        ds1.setMech(0, lo1);
        ds1.setMech(1, lo2);
        ds2.setMech(0, lo3);
        ds2.setMech(1, lo4);

        // Add some pilot modules to make sure they are serialised
        CommandStack stack = new CommandStack(0);
        stack.pushAndApply(new CmdAddModule(null, lo1, PilotModuleDB.lookup("ADVANCED UAV")));
        stack.pushAndApply(new CmdAddModule(null, lo4, PilotModuleDB.lookup("COOL SHOT 6")));

        MechGarage cut = new MechGarage(xBar);
        cut.add(lo1);
        cut.add(lo2);
        cut.add(lo3);
        cut.add(lo4);
        cut.add(ds1);
        cut.add(ds2);
        reset(xBar);

        // Execute
        cut.saveas(testFile);
        MechGarage loadedGarage = MechGarage.open(testFile, xBar);

        // Verify
        verify(xBar).post(new GarageMessage(Type.Saved, cut));
        verify(xBar).post(new GarageMessage(Type.NewGarage, loadedGarage));
        assertEquals(4, loadedGarage.getMechs().size());
        assertEquals(lo1, loadedGarage.getMechs().get(0));
        assertEquals(lo2, loadedGarage.getMechs().get(1));
        assertEquals(lo3, loadedGarage.getMechs().get(2));
        assertEquals(lo4, loadedGarage.getMechs().get(3));

        assertEquals(2, loadedGarage.getDropShips().size());
        assertEquals(ds1, loadedGarage.getDropShips().get(0));
        assertEquals(ds2, loadedGarage.getDropShips().get(1));
        assertEquals(loadedGarage.getMechs().get(0), loadedGarage.getDropShips().get(0).getMech(0));
        assertEquals(loadedGarage.getMechs().get(1), loadedGarage.getDropShips().get(0).getMech(1));
        assertEquals(loadedGarage.getMechs().get(2), loadedGarage.getDropShips().get(1).getMech(0));
        assertEquals(loadedGarage.getMechs().get(3), loadedGarage.getDropShips().get(1).getMech(1));
    }

    /**
     * {@link MechGarage#save()} shall overwrite previously saved garage.
     * 
     * @throws Exception
     *             Shouldn't be thrown.
     */
    @Test
    public void testSave() throws Exception {
        // Setup
        LoadoutBase<?> lo1 = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("as7-d-dc"));
        LoadoutBase<?> lo2 = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("as7-k"));
        MechGarage cut = new MechGarage(xBar);
        cut.add(lo1);
        cut.saveas(testFile); // Create garage with one mech and save it.
        cut = MechGarage.open(testFile, xBar);
        cut.add(lo2); // Add a mech and use the save() function. The same file should be overwritten.
        reset(xBar);

        // Execute
        cut.save();

        // Open the garage to verify.
        verify(xBar).post(new GarageMessage(Type.Saved, cut));

        cut = MechGarage.open(testFile, xBar);
        assertEquals(2, cut.getMechs().size());
        assertEquals(lo1, cut.getMechs().get(0));
        assertEquals(lo2, cut.getMechs().get(1));
    }

    /**
     * add(Loadout, boolean) shall add a loadout to the garage that can subsequently be removed with remove(Loadout,
     * boolean).
     * 
     * @throws Exception
     *             Shouldn't be thrown.
     */
    @Test
    public void testAddRemoveLoadout() throws Exception {
        // Setup
        LoadoutBase<?> loadout = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("as7-d-dc"));
        MechGarage cut = new MechGarage(xBar);

        // Execute
        cut.add(loadout);

        // Verify
        assertEquals(1, cut.getMechs().size());
        assertSame(loadout, cut.getMechs().get(0));
        verify(xBar).post(new GarageMessage(GarageMessage.Type.LoadoutAdded, cut, loadout));

        // Execute
        cut.remove(loadout);

        // Verify
        assertTrue(cut.getMechs().isEmpty());
        verify(xBar).post(new GarageMessage(GarageMessage.Type.LoadoutRemoved, cut, loadout));
    }

    /**
     * Removing an nonexistent loadout is a no-op.
     * 
     * @throws Exception
     *             Shouldn't be thrown.
     */
    @Test
    public void testRemoveLoadoutNonexistent() throws Exception {
        // Setup
        LoadoutBase<?> loadout = DefaultLoadoutFactory.instance.produceStock(ChassisDB.lookup("as7-d-dc"));
        MechGarage cut = new MechGarage(xBar);
        reset(xBar);
        cut.remove(loadout);

        verifyZeroInteractions(xBar);
    }

    /**
     * Make sure that we can load many of the stock builds saved from 1.5.0.
     * <p>
     * Note, this is a backwards compatibility test.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadStockBuilds_150() throws Exception {
        MechGarage garage = MechGarage.open(new File("resources/resources/stock1.5.0.xml"), xBar);
        CommandStack stack = new CommandStack(0);
        assertEquals(64, garage.getMechs().size());

        for (LoadoutBase<?> loadout : garage.getMechs()) {
            LoadoutStandard loadoutStandard = (LoadoutStandard) loadout;

            LoadoutBase<?> clone = DefaultLoadoutFactory.instance.produceClone(loadoutStandard);
            stack.pushAndApply(new CmdLoadStock(clone.getChassis(), clone, xBar));

            assertEquals(clone, loadout);
        }
    }

    /**
     * Issue #337. Actuator state is not saved properly.
     * 
     * @throws IOException
     */
    @Test
    public void testActuatorStateSaved() throws IOException {
        LoadoutOmniMech loadout = (LoadoutOmniMech) DefaultLoadoutFactory.instance
                .produceEmpty(ChassisDB.lookup("WHK-B"));

        loadout.getComponent(Location.RightArm).setToggleState(ItemDB.LAA, false);

        MechGarage garage = new MechGarage(xBar);
        garage.add(loadout);
        garage.saveas(testFile);
        garage = null;
        garage = MechGarage.open(testFile, xBar);

        LoadoutOmniMech loaded = (LoadoutOmniMech) garage.getMechs().get(0);

        assertFalse(loaded.getComponent(Location.RightArm).getToggleState(ItemDB.LAA));
    }

    /**
     * Even if DHS are serialized before the Engine, they should be added as engine heat sinks.
     */
    @Test
    public void testUnMarshalDhsBeforeEngine() {
        String xml = "<?xml version=\"1.0\" ?><loadout name=\"AS7-BH\" chassi=\"AS7-BH\"><upgrades version=\"2\"><armor>2810</armor><structure>3100</structure><guidance>3051</guidance><heatsinks>3002</heatsinks></upgrades><efficiencies><speedTweak>false</speedTweak><coolRun>false</coolRun><heatContainment>false</heatContainment><anchorTurn>false</anchorTurn><doubleBasics>false</doubleBasics><fastfire>false</fastfire></efficiencies><component part=\"Head\" armor=\"0\" /><component part=\"LeftArm\" armor=\"0\" /><component part=\"LeftLeg\" armor=\"0\" /><component part=\"LeftTorso\" armor=\"0/0\" /><component part=\"CenterTorso\" armor=\"0/0\"><item>3001</item><item>3001</item><item>3001</item><item>3001</item><item>3001</item><item>3001</item><item>3278</item></component><component part=\"RightTorso\" armor=\"0/0\" /><component part=\"RightLeg\" armor=\"0\" /><component part=\"RightArm\" armor=\"0\" /></loadout>";

        XStream stream = LoadoutBase.loadoutXstream();
        LoadoutStandard loadout = (LoadoutStandard) stream.fromXML(xml);

        assertEquals(6, loadout.getComponent(Location.CenterTorso).getEngineHeatsinks());
    }
}
