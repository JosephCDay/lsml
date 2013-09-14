package lisong_mechlab.model.loadout.metrics;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import lisong_mechlab.model.item.BallisticWeapon;
import lisong_mechlab.model.item.Item;
import lisong_mechlab.model.item.ItemDB;
import lisong_mechlab.model.item.MissileWeapon;
import lisong_mechlab.model.loadout.Loadout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MaxDPSTest{
   @Mock
   private Loadout loadout;
   @InjectMocks
   private MaxDPS  cut;

   @Test
   public void testCalculate_AMS() throws Exception{
      BallisticWeapon mg = (BallisticWeapon)ItemDB.lookup("MACHINE GUN");
      List<Item> items = new ArrayList<>();
      when(loadout.getAllItems()).thenReturn(items);
      items.add(ItemDB.AMS);
      items.add(mg);

      assertEquals(mg.getStat("d/s", null), cut.calculate(), 0.0);
   }
   
   @Test
   public void testCalculate() throws Exception{
      BallisticWeapon mg = (BallisticWeapon)ItemDB.lookup("MACHINE GUN");
      MissileWeapon lrm20 = (MissileWeapon)ItemDB.lookup("LRM20");
      List<Item> items = new ArrayList<>();
      when(loadout.getAllItems()).thenReturn(items);
      items.add(ItemDB.AMS);
      items.add(mg);
      items.add(lrm20);
      items.add(ItemDB.lookup("STD ENGINE 300")); // Unrelated items shall not skew the values
      items.add(ItemDB.lookup("AMS AMMO"));

      assertEquals(mg.getStat("d/s", null) + lrm20.getStat("d/s", null), cut.calculate(), 0.0);
   }

}
