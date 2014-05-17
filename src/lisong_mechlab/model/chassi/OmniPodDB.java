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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lisong_mechlab.model.DataCache;

/**
 * This class acts as a database for all {@link OmniPod}s.
 * 
 * @author Li Song
 */
public class OmniPodDB{
   private static final Map<ChassisOmniMech, List<OmniPod>> chassis2pod;

   /**
    * @param aChassis
    *           The chassis to get the pod for.
    * @param aLocation
    *           The location to get the pod for.
    * @return The {@link OmniPod}s that is "original" to the given chassis and {@link Location}.
    */
   public static OmniPod lookupOriginal(ChassisOmniMech aChassis, Location aLocation){
      for(OmniPod omniPod : lookup(aChassis, aLocation)){
         if( omniPod.getOriginalChassisId() == aChassis.getMwoId() )
            return omniPod;
      }
      throw new IllegalArgumentException("There exists no original omnipod for " + aChassis + " at " + aLocation);
   }

   /**
    * @param aChassis
    *           A chassis to get all compatible pods for.
    * @param aLocation
    *           A location on the chassis to get all compatible pods for.
    * @return A {@link Collection} of {@link OmniPod}s that are compatible with the given chassis and {@link Location}.
    */
   public static Collection<OmniPod> lookup(ChassisOmniMech aChassis, Location aLocation){
      List<OmniPod> ans = new ArrayList<>();
      for(OmniPod omniPod : chassis2pod.get(aChassis)){
         if( omniPod.getLocation() == aLocation )
            ans.add(omniPod);
      }
      return ans;
   }

   /**
    * A decision has been made to rely on static initializers for *DB classes. The motivation is that all items are
    * immutable, and this is the only way that allows providing global item constants such as ItemDB.AMS.
    */
   static{
      DataCache dataCache;
      try{
         dataCache = DataCache.getInstance();
      }
      catch( IOException e ){
         throw new RuntimeException(e); // Promote to unchecked. This is a critical failure.
      }

      chassis2pod = new HashMap<ChassisOmniMech, List<OmniPod>>();

      for(OmniPod omniPod : dataCache.getOmniPods()){
         ChassisOmniMech originalChassis = (ChassisOmniMech)ChassisDB.lookup(omniPod.getOriginalChassisId());

         List<OmniPod> list = chassis2pod.get(originalChassis);
         if( list == null ){
            list = new ArrayList<>();
            chassis2pod.put(originalChassis, list);
         }
         list.add(omniPod);
      }

   }
}
