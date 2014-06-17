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
package lisong_mechlab.model.chassi;

import java.util.List;

import lisong_mechlab.model.item.Item;
import lisong_mechlab.model.item.ItemDB;
import lisong_mechlab.model.item.Weapon;

/**
 * A component specific to omnimechs.
 * 
 * @author Li Song
 */
public class ComponentOmniMech extends ComponentBase{
   private final OmniPod fixedOmniPod;
   private final int     dynamicArmor;
   private final int     dynamicStructure;

   /**
    * Creates a new {@link ComponentOmniMech}.
    * 
    * @param aCriticalSlots
    *           The number of critical slots in the component.
    * @param aHitPoints
    *           The number of internal hit points on the component (determines armor too).
    * @param aLocation
    *           The location of the component.
    * @param aFixedOmniPod
    *           If this component has a fixed {@link OmniPod}, a reference to said {@link OmniPod} otherwise
    *           <code>null</code> if the {@link OmniPod} can be changed.
    * @param aFixedItems
    *           An array of fixed {@link Item}s for this component.
    * @param aDynamicStructureSlots
    *           An array where each element represents the ordinal of a {@link Location} and how many dynamic structure
    *           slots are fixed at that location.
    * @param aDynamicArmorSlots
    *           An array where each element represents the ordinal of a {@link Location} and how many dynamic armor
    *           slots are fixed at that location.
    */
   public ComponentOmniMech(Location aLocation, int aCriticalSlots, double aHitPoints, List<Item> aFixedItems, OmniPod aFixedOmniPod,
                            int aDynamicStructureSlots, int aDynamicArmorSlots){
      super(aCriticalSlots, aHitPoints, aLocation, aFixedItems);
      fixedOmniPod = aFixedOmniPod;
      dynamicArmor = aDynamicArmorSlots;
      dynamicStructure = aDynamicStructureSlots;
   }

   /**
    * @return True if this {@link ComponentOmniMech} has a fixed {@link OmniPod} that can't be changed.
    */
   public boolean hasFixedOmniPod(){
      return null != fixedOmniPod;
   }

   /**
    * @return If this component has a fixed {@link OmniPod}, it returns the {@link OmniPod}. Otherwise it returns
    *         <code>null</code>.
    */
   public OmniPod getFixedOmniPod(){
      return fixedOmniPod;
   }

   @Override
   public boolean isAllowed(Item aItem){

      final int usedSlots;
      if( shouldRemoveArmActuators(aItem) ){
         int fixedSlots = 0;
         for(Item item : getFixedItems()){
            if( item != ItemDB.LAA && item != ItemDB.HA ){
               fixedSlots += item.getNumCriticalSlots();
            }
         }
         usedSlots = fixedSlots + getDynamicArmorSlots() + getDynamicStructureSlots();
      }
      else{
         usedSlots = getFixedItemSlots() + getDynamicArmorSlots() + getDynamicStructureSlots();
      }

      if( aItem.getNumCriticalSlots() > getSlots() - usedSlots ){
         return false;
      }

      return super.isAllowed(aItem);
   }

   /**
    * @return The number of dynamic armor slots in the given location.
    */
   public int getDynamicArmorSlots(){
      return dynamicArmor;
   }

   /**
    * @return The number of dynamic structure slots in the given location.
    */
   public int getDynamicStructureSlots(){
      return dynamicStructure;
   }

   /**
    * @param aItem
    *           The item to check with.
    * @return <code>true</code> if the Lower Arm Actuator (LAA) and/or Hand Actuator (HA) should be removed if the given
    *         item is equipped.
    */
   public boolean shouldRemoveArmActuators(Item aItem){
      if( aItem instanceof Weapon ){
         boolean isLargeBore = false;
         isLargeBore |= aItem.getName().toLowerCase().contains("ppc");
         isLargeBore |= aItem.getName().toLowerCase().contains("gauss");
         isLargeBore |= aItem.getName().toLowerCase().contains("ac/");
         isLargeBore |= aItem.getName().toLowerCase().contains("x ac");
         isLargeBore |= aItem.getName().toLowerCase().contains("10-x");
         return isLargeBore;
      }
      return false;
   }
}
