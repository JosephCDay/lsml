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
package lisong_mechlab.model.item;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * A test suite for {@link PilotModuleDB}. The primary purpose is to test lookup and correct parsing of
 * {@link PilotModule}s.
 * 
 * @author Li Song
 */
public class PilotModuleDBTest{

   @Test
   public void testLookup_ByID(){
      WeaponModule module = (WeaponModule)PilotModuleDB.lookup(4234);

      MissileWeapon srm2 = (MissileWeapon)ItemDB.lookup("SRM2");
      MissileWeapon srm2artemis = (MissileWeapon)ItemDB.lookup("SRM2_Artemis");

      assertTrue(module.affectsWeapon(srm2));
      assertTrue(module.affectsWeapon(srm2artemis));
      
      assertEquals(6, module.applyLongRange(0, 1), 0.0);
      assertEquals(6, module.applyMaxRange(0, 1), 0.0);
      assertEquals(0.04, module.applyHeat(0, 1), 0.0);

      assertEquals(30, module.applyLongRange(0, 5), 0.0);
      assertEquals(30, module.applyMaxRange(0, 5), 0.0);
      assertEquals(0.2, module.applyHeat(0, 5), 0.0);
   }
}
