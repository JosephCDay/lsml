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

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;
import org.lisoft.lsml.model.ItemDB;
import org.lisoft.mwo_data.Faction;
import org.lisoft.mwo_data.mechs.HardPointType;
import org.lisoft.mwo_data.modifiers.Modifier;
import org.lisoft.mwo_data.modifiers.ModifierDescription;
import org.lisoft.mwo_data.modifiers.ModifierType;
import org.lisoft.mwo_data.modifiers.Operation;

/**
 * Test suite for {@link AmmoWeapon}.
 *
 * @author Li Song
 */
public class AmmoWeaponTest {

  @Test
  public final void testHasBuiltinAmmo() throws NoSuchItemException {
    assertTrue(((AmmoWeapon) ItemDB.lookup("ROCKET LAUNCHER 20")).hasBuiltInAmmo());
    assertFalse(((AmmoWeapon) ItemDB.lookup("LRM 20")).hasBuiltInAmmo());
  }

  @Test
  public final void testIsCompatibleAmmo() throws Exception {
    final BallisticWeapon ac20 = (BallisticWeapon) ItemDB.lookup("AC/20");

    final Ammunition ac20ammo = (Ammunition) ItemDB.lookup("AC/20 AMMO");
    final Ammunition ac20ammoHalf = (Ammunition) ItemDB.lookup("AC/20 AMMO (1/2)");

    assertTrue(ac20.isCompatibleAmmo(ac20ammoHalf));
    assertTrue(ac20.isCompatibleAmmo(ac20ammo));
  }

  @Test
  public final void testIsCompatibleAmmoBuiltinAmmo() {
    final AmmoWeapon builtInAmmo =
        new AmmoWeapon(
            "",
            "",
            "",
            0,
            0,
            0.0,
            HardPointType.ENERGY,
            0,
            Faction.CLAN,
            null,
            null,
            null,
            1,
            1,
            1,
            1,
            null,
            0,
            0.0,
            null,
            0.0,
            0.0,
            null,
            false,
            1);
    final Ammunition ac20ammo =
        new Ammunition(
            "", "", "", 0, 0, 0.0, HardPointType.NONE, 0.0, Faction.CLAN, 10, "ammotype", 0.0);

    assertFalse(builtInAmmo.isCompatibleAmmo(ac20ammo));
  }

  @Test
  public final void testIsOneShotNegative() {
    final AmmoWeapon cut =
        new AmmoWeapon(
            "",
            "",
            "",
            0,
            0,
            0.0,
            HardPointType.ENERGY,
            0,
            Faction.CLAN,
            null,
            null,
            null,
            1,
            1,
            1,
            1,
            null,
            0,
            0.0,
            null,
            0.0,
            0.0,
            null,
            false,
            1);
    assertFalse(cut.isOneShot());
  }

  @Test
  public final void testIsOneShotPositive() {
    final AmmoWeapon cut =
        new AmmoWeapon(
            "",
            "",
            "",
            0,
            0,
            0.0,
            HardPointType.ENERGY,
            0,
            Faction.CLAN,
            null,
            null,
            null,
            1,
            1,
            1,
            1,
            null,
            0,
            0.0,
            null,
            0.0,
            0.0,
            null,
            true,
            1);
    assertTrue(cut.isOneShot());
  }

  @Test
  public final void testOneShotDPS() throws NoSuchItemException {
    final AmmoWeapon cut = (AmmoWeapon) ItemDB.lookup("ROCKET LAUNCHER 20");

    assertEquals(0.0, cut.getStat("d/s", null), 0.0);
    assertEquals(0.0, cut.getStat("d/sh", null), 0.0);
  }

  @Test
  public void testRawFiringPeriod() throws Exception {
    final AmmoWeapon ac20 = (AmmoWeapon) ItemDB.lookup("AC/20");
    final AmmoWeapon cuac10 = (AmmoWeapon) ItemDB.lookup("C-ULTRA AC/10");
    final AmmoWeapon lrm10 = (AmmoWeapon) ItemDB.lookup("LRM 10");
    final AmmoWeapon clrm10 = (AmmoWeapon) ItemDB.lookup("C-LRM 10");
    final AmmoWeapon srm6 = (AmmoWeapon) ItemDB.lookup("SRM6");

    assertEquals(ac20.getCoolDown(null), ac20.getRawFiringPeriod(null), 0.0);
    assertEquals(2 * 0.11 + cuac10.getCoolDown(null), cuac10.getRawFiringPeriod(null), 0.0);
    assertEquals(lrm10.getCoolDown(null), lrm10.getRawFiringPeriod(null), 0.0);
    assertEquals(0.05 * 9 + clrm10.getCoolDown(null), clrm10.getRawFiringPeriod(null), 0.0);
    assertEquals(srm6.getCoolDown(null), srm6.getRawFiringPeriod(null), 0.0);
  }

  @Test
  public final void testSpreadQuirks() throws Exception {
    final ModifierDescription quirkDescription =
        new ModifierDescription(
            null,
            "key",
            Operation.MUL,
            ModifierDescription.SEL_ALL,
            ModifierDescription.SPEC_WEAPON_SPREAD,
            ModifierType.POSITIVE_GOOD);
    final Modifier modifier = new Modifier(quirkDescription, 1.0);

    final AmmoWeapon cut = (AmmoWeapon) ItemDB.lookup("SRM6");

    final double normal = cut.getRangeProfile().getSpread().value(null);
    final double quirked = cut.getRangeProfile().getSpread().value(Arrays.asList(modifier));

    assertEquals(normal * 2, quirked, 0.0);
  }
}
