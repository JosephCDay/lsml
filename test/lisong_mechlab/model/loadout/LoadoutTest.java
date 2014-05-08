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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.List;

import lisong_mechlab.model.chassi.ArmorSide;
import lisong_mechlab.model.chassi.ChassisDB;
import lisong_mechlab.model.chassi.Location;
import lisong_mechlab.model.item.Item;
import lisong_mechlab.model.item.ItemDB;
import lisong_mechlab.model.loadout.part.OpAddItem;
import lisong_mechlab.model.loadout.part.ConfiguredComponent;
import lisong_mechlab.model.loadout.part.OpRemoveItem;
import lisong_mechlab.model.loadout.part.OpSetArmor;
import lisong_mechlab.model.upgrades.OpSetArmorType;
import lisong_mechlab.model.upgrades.OpSetGuidanceType;
import lisong_mechlab.model.upgrades.OpSetHeatSinkType;
import lisong_mechlab.model.upgrades.OpSetStructureType;
import lisong_mechlab.model.upgrades.UpgradeDB;
import lisong_mechlab.util.MessageXBar;
import lisong_mechlab.util.OperationStack;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.thoughtworks.xstream.XStream;

public class LoadoutTest{
   @Spy
   MessageXBar    xBar;

   @Mock
   OperationStack undoStack;

   @Before
   public void setup(){
      MockitoAnnotations.initMocks(this);
   }

   @Test
   public void testGetJumpJetCount_noJJCapability() throws Exception{
      Loadout cut = new Loadout("HBK-4J", xBar);
      assertEquals(0, cut.getJumpJetCount());
   }

   @Test
   public void testGetJumpJetCount_noJJEquipped() throws Exception{
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      assertEquals(0, cut.getJumpJetCount());
   }

   @Test
   public void testGetJumpJetCount() throws Exception{
      Loadout cut = new Loadout("SDR-5D", xBar);
      assertEquals(8, cut.getJumpJetCount()); // 8 stock
   }

   @Test
   public void testGetJumpJetType_noJJCapability() throws Exception{
      Loadout cut = new Loadout("HBK-4J", xBar);
      assertNull(cut.getJumpJetType());
   }

   @Test
   public void testGetJumpJetType_noJJEquipped() throws Exception{
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      assertNull(cut.getJumpJetType());
   }

   @Test
   public void testGetJumpJetType() throws Exception{
      Loadout cut = new Loadout("SDR-5D", xBar);
      assertSame(ItemDB.lookup("JUMP JETS - CLASS V"), cut.getJumpJetType());
   }

   /**
    * Even if DHS are serialized before the Engine, they should be added as engine heat sinks.
    */
   @Test
   public void testUnMarshalDhsBeforeEngine(){
      String xml = "<?xml version=\"1.0\" ?><loadout name=\"AS7-BH\" chassi=\"AS7-BH\"><upgrades version=\"2\"><armor>2810</armor><structure>3100</structure><guidance>3051</guidance><heatsinks>3002</heatsinks></upgrades><efficiencies><speedTweak>false</speedTweak><coolRun>false</coolRun><heatContainment>false</heatContainment><anchorTurn>false</anchorTurn><doubleBasics>false</doubleBasics><fastfire>false</fastfire></efficiencies><component part=\"Head\" armor=\"0\" /><component part=\"LeftArm\" armor=\"0\" /><component part=\"LeftLeg\" armor=\"0\" /><component part=\"LeftTorso\" armor=\"0/0\" /><component part=\"CenterTorso\" armor=\"0/0\"><item>3001</item><item>3001</item><item>3001</item><item>3001</item><item>3001</item><item>3001</item><item>3278</item></component><component part=\"RightTorso\" armor=\"0/0\" /><component part=\"RightLeg\" armor=\"0\" /><component part=\"RightArm\" armor=\"0\" /></loadout>";

      XStream stream = Loadout.loadoutXstream(xBar);
      Loadout loadout = (Loadout)stream.fromXML(xml);

      assertEquals(6, loadout.getPart(Location.CenterTorso).getNumEngineHeatsinks());
   }

   @Test
   public void testCanEquip_NoInternals() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("LCT-3M"), xBar);

      // Execute + Verify
      assertFalse(cut.canEquip(ConfiguredComponent.ENGINE_INTERNAL));
   }

   @Test
   public void testCanEquip_TooHeavy() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("LCT-3M"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.CenterTorso), ItemDB.lookup("STD ENGINE 190")));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.lookup("PPC")));
      assertTrue(cut.getFreeMass() < 2.0); // Should be 1.5 tons free

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.lookup("PPC")));
   }

   @Test
   public void testCanEquip_NotSupportedByChassi() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("LCT-3M"), xBar);

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.ECM));
   }

   @Test
   public void testCanEquip_CompatibleUpgrades() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("LCT-3M"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));

      // Execute + Verify
      assertTrue(cut.canEquip(ItemDB.DHS));
   }

   @Test
   public void testCanEquip_NotCompatibleUpgrades() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("LCT-3M"), xBar);

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.DHS));
   }

   @Test
   public void testCanEquip_TooFewSlots() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("LCT-3M"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpSetArmorType(null, cut, UpgradeDB.FERRO_FIBROUS_ARMOR));
      stack.pushAndApply(new OpSetStructureType(null, cut, UpgradeDB.ENDO_STEEL_STRUCTURE));
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.CenterTorso), ItemDB.lookup("XL ENGINE 100")));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.DHS));
      assertTrue(cut.getFreeMass() > 1.5); // Should be 13.5 tons free

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.DHS));
   }

   @Test
   public void testCanEquip_JJ() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      Item jj = ItemDB.lookup("JUMP JETS - CLASS V");

      // Execute + Verify
      assertTrue(cut.canEquip(jj));
   }

   @Test
   public void testCanEquip_TooManyJJ() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      Item jj = ItemDB.lookup("JUMP JETS - CLASS V");
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), jj));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), jj));
      // Make sure test won't fail on wrong condition
      assertTrue(cut.getFreeMass() > 1.5);
      assertTrue(cut.getNumCriticalSlotsFree() > 1);
      assertEquals(cut.getChassi().getMaxJumpJets(), cut.getJumpJetCount());
      assertTrue(cut.getChassi().isAllowed(jj));

      // Execute + Verify
      assertFalse(cut.canEquip(jj));
   }

   @Test
   public void testCanEquip_TooManyEngine() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.CenterTorso), ItemDB.lookup("XL ENGINE 100")));

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.lookup("XL ENGINE 100")));
   }

   @Test
   public void testCanEquip_XLEngineNoSpaceLeftTorso() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.lookup("XL ENGINE 100")));
   }

   @Test
   public void testCanEquip_XLEngineNoSpaceRightTorso() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.lookup("XL ENGINE 100")));
   }

   @Test
   public void testCanEquip_XLEngineNoSpaceCentreTorso() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.CenterTorso), ItemDB.DHS));

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.lookup("XL ENGINE 100")));
   }

   @Test
   public void testCanEquip_XLEngine12SlotsFree() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpSetStructureType(null, cut, UpgradeDB.ENDO_STEEL_STRUCTURE));
      stack.pushAndApply(new OpSetArmorType(null, cut, UpgradeDB.FERRO_FIBROUS_ARMOR));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.CASE));
      assertEquals(12, cut.getNumCriticalSlotsFree());

      // Execute + Verify
      assertTrue(cut.canEquip(ItemDB.lookup("XL ENGINE 100")));
   }

   @Test
   public void testCanEquip_XLEngine11SlotsFree() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpSetStructureType(null, cut, UpgradeDB.ENDO_STEEL_STRUCTURE));
      stack.pushAndApply(new OpSetArmorType(null, cut, UpgradeDB.FERRO_FIBROUS_ARMOR));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.ECM));
      assertEquals(11, cut.getNumCriticalSlotsFree());

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.lookup("XL ENGINE 100")));
   }

   @Test
   public void testCanEquip_NoHardpoints() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5V"), xBar);

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.ECM));
   }

   @Test
   public void testCanEquip_NotEnoughHardpoints() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("SDR-5D"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.ECM));

      // Execute + Verify
      assertFalse(cut.canEquip(ItemDB.ECM));
   }

   @Test
   public void testGetCandidateLocationsForItem_AlreadyHasEngine() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("AS7-D-DC"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.CenterTorso), ItemDB.lookup("STD ENGINE 300")));
      assertTrue(cut.getNumCriticalSlotsFree() > 10);
      assertTrue(cut.getFreeMass() > 40.0);

      // Execute + Verify
      assertTrue(cut.getCandidateLocationsForItem(ItemDB.lookup("STD ENGINE 300")).isEmpty());
   }

   /**
    * When the only hard point that can legally house the item is occupied by another item that can be moved to another
    * hard point, the candidate shall be that only component.
    * 
    * @throws Exception
    */
   @Test
   public void testGetCandidateLocationsForItem_MoveHardpoint() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("CTF-IM"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.lookup("AC/10")));
      assertTrue(cut.getNumCriticalSlotsFree() > 20);
      assertTrue(cut.getFreeMass() > 20.0);

      // Execute + Verify
      List<ConfiguredComponent> candidates = cut.getCandidateLocationsForItem(ItemDB.lookup("AC/20"));
      assertEquals(1, candidates.size());
      assertEquals(Location.RightTorso, candidates.get(0).getInternalPart().getLocation());
   }

   /**
    * Shall only return parts that could possibly contain the item's size
    * 
    * @throws Exception
    */
   @Test
   public void testGetCandidateLocationsForItem_SizeLimit() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("CTF-IM"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      assertTrue(cut.getNumCriticalSlotsFree() > 20);
      assertTrue(cut.getFreeMass() > 20.0);

      // Execute + Verify
      List<ConfiguredComponent> candidates = cut.getCandidateLocationsForItem(ItemDB.DHS);
      assertEquals(5, candidates.size()); // 2x arms + 3x torso
      assertTrue(candidates.remove(cut.getPart(Location.LeftArm)));
      assertTrue(candidates.remove(cut.getPart(Location.RightArm)));
      assertTrue(candidates.remove(cut.getPart(Location.RightTorso)));
      assertTrue(candidates.remove(cut.getPart(Location.LeftTorso)));
      assertTrue(candidates.remove(cut.getPart(Location.CenterTorso)));
   }

   /**
    * Empty list shall be returned if there are no free hardpoints
    * 
    * @throws Exception
    */
   @Test
   public void testGetCandidateLocationsForItem_NoFreeHardpoints() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("CTF-IM"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.lookup("AC/2")));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightArm), ItemDB.lookup("AC/2")));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftArm), ItemDB.lookup("AC/2")));
      assertTrue(cut.getNumCriticalSlotsFree() > 20);
      assertTrue(cut.getFreeMass() > 20.0);

      // Execute + Verify
      List<ConfiguredComponent> candidates = cut.getCandidateLocationsForItem(ItemDB.lookup("AC/2"));
      assertTrue(candidates.isEmpty());
   }

   @Test
   public void testGetCandidateLocationsForItem_NotGloballyFeasible_TooHeavy() throws Exception{
      // Setup
      Loadout cut = new Loadout("CTF-IM", xBar);
      assertTrue(cut.getNumCriticalSlotsFree() > 0);
      assertTrue(cut.getFreeMass() < 0.1);

      // Execute + Verify
      assertTrue(cut.getCandidateLocationsForItem(ItemDB.CASE).isEmpty());
   }

   @Test
   public void testGetCandidateLocationsForItem_NotGloballyFeasible_TooFewSlots() throws Exception{
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("AS7-D-DC"), xBar);
      OperationStack stack = new OperationStack(0);
      stack.pushAndApply(new OpSetHeatSinkType(null, cut, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpSetStructureType(null, cut, UpgradeDB.ENDO_STEEL_STRUCTURE));
      stack.pushAndApply(new OpSetArmorType(null, cut, UpgradeDB.FERRO_FIBROUS_ARMOR));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.LeftTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));
      stack.pushAndApply(new OpAddItem(null, cut.getPart(Location.RightTorso), ItemDB.DHS));

      assertTrue(cut.getNumCriticalSlotsFree() < 3);
      assertTrue(cut.getFreeMass() > 2.0);

      // Execute + Verify
      assertTrue(cut.getCandidateLocationsForItem(ItemDB.DHS).isEmpty());
   }

   /**
    * Will create a new, empty loadout
    * 
    * @throws Exception
    */
   @Test
   public void testLoadout_empty() throws Exception{
      Loadout cut = new Loadout(ChassisDB.lookup("HBK-4J"), xBar);

      assertEquals(0, cut.getArmor());
      assertEquals(ChassisDB.lookup("hbk-4j"), cut.getChassi());
      assertEquals(5.0, cut.getMass(), 0.0);
      assertEquals(53, cut.getNumCriticalSlotsFree());
      assertEquals(5 * 12 + 3 * 6 - 53, cut.getNumCriticalSlotsUsed());

      verify(xBar).post(new Loadout.Message(cut, Loadout.Message.Type.CREATE));
   }

   /**
    * Will create a deep copy of the argument.
    * <p>
    * Note: This is an integration test.
    * 
    * @throws Exception
    */
   @Test
   public void testLoadout_CopyCtor() throws Exception{
      OperationStack stack = new OperationStack(0);
      Loadout cut = new Loadout("HBK-4J", xBar);
      Loadout copy = new Loadout(cut, xBar);
      
      // A copy must be equal :)
      assertEquals(cut, copy);

      // Must be deep
      copy.rename("foo");
      assertFalse(copy.getName().equals(cut.getName()));
      
      assertTrue(copy.getPart(Location.RightTorso).equals(cut.getPart(Location.RightTorso)));
      stack.pushAndApply(new OpRemoveItem(xBar, copy.getPart(Location.RightTorso), ItemDB.lookup("LRM 10")));
      stack.pushAndApply(new OpRemoveItem(xBar, copy.getPart(Location.RightTorso), ItemDB.lookup("LRM 10")));
      assertFalse(copy.getPart(Location.RightTorso).equals(cut.getPart(Location.RightTorso)));
      
      assertTrue(copy.getPart(Location.LeftTorso).equals(cut.getPart(Location.LeftTorso)));
      stack.pushAndApply(new OpSetArmor(xBar, copy.getPart(Location.LeftTorso), ArmorSide.FRONT, 3, true));
      stack.pushAndApply(new OpSetArmor(xBar, copy.getPart(Location.LeftTorso), ArmorSide.BACK, 3, false));
      assertFalse(copy.getPart(Location.LeftTorso).equals(cut.getPart(Location.LeftTorso)));
      
      assertTrue(copy.getUpgrades().equals(cut.getUpgrades()));
      stack.pushAndApply(new OpSetArmorType(xBar, copy, UpgradeDB.FERRO_FIBROUS_ARMOR));
      stack.pushAndApply(new OpSetStructureType(xBar, copy, UpgradeDB.ENDO_STEEL_STRUCTURE));
      stack.pushAndApply(new OpSetHeatSinkType(xBar, copy, UpgradeDB.DOUBLE_HEATSINKS));
      stack.pushAndApply(new OpSetGuidanceType(xBar, copy, UpgradeDB.ARTEMIS_IV));
      assertFalse(copy.getUpgrades().getArmor() == cut.getUpgrades().getArmor());
      assertFalse(copy.getUpgrades().getStructure() == cut.getUpgrades().getStructure());
      assertFalse(copy.getUpgrades().getGuidance() == cut.getUpgrades().getGuidance());
      assertFalse(copy.getUpgrades().getHeatSink() == cut.getUpgrades().getHeatSink());
      assertFalse(copy.getUpgrades().equals(cut.getUpgrades()));

      verify(xBar).post(new Loadout.Message(cut, Loadout.Message.Type.CREATE));
      verify(xBar).post(new Loadout.Message(copy, Loadout.Message.Type.CREATE));
   }

   @Test
   public void testFreeMass(){
      // Setup
      Loadout cut = new Loadout(ChassisDB.lookup("AS7-D-DC"), xBar);

      // Verify
      assertEquals(90, cut.getFreeMass(), 0.0);
   }
}
