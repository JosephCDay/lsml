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

import java.io.File;

import lisong_mechlab.model.chassi.ChassisIS;
import lisong_mechlab.model.chassi.ChassisDB;
import lisong_mechlab.model.chassi.InternalComponent;
import lisong_mechlab.model.item.Item;
import lisong_mechlab.model.loadout.component.ComponentBuilder;
import lisong_mechlab.model.loadout.component.ConfiguredComponent;
import lisong_mechlab.model.loadout.converters.ChassiConverter;
import lisong_mechlab.model.loadout.converters.ItemConverter;
import lisong_mechlab.model.loadout.converters.LoadoutConverter;
import lisong_mechlab.model.loadout.converters.LoadoutPartConverter;
import lisong_mechlab.model.loadout.converters.UpgradeConverter;
import lisong_mechlab.model.loadout.converters.UpgradesConverter;
import lisong_mechlab.util.MessageXBar;
import lisong_mechlab.util.OperationStack;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * This class represents the complete state of a 'mechs configuration.
 * 
 * @author Li Song
 */
public class Loadout extends LoadoutBase<ConfiguredComponent, InternalComponent>{

   public static Loadout load(File aFile, MessageXBar aXBar){
      XStream stream = loadoutXstream(aXBar);
      return (Loadout)stream.fromXML(aFile);
   }

   public static XStream loadoutXstream(MessageXBar aXBar){
      XStream stream = new XStream(new StaxDriver());
      stream.autodetectAnnotations(true);
      stream.setMode(XStream.NO_REFERENCES);
      stream.registerConverter(new ChassiConverter());
      stream.registerConverter(new ItemConverter());
      stream.registerConverter(new LoadoutPartConverter(aXBar, null));
      stream.registerConverter(new LoadoutConverter(aXBar));
      stream.registerConverter(new UpgradeConverter());
      stream.registerConverter(new UpgradesConverter());
      stream.addImmutableType(Item.class);
      stream.alias("component", ConfiguredComponent.class);
      stream.alias("loadout", Loadout.class);
      return stream;
   }

   /**
    * Will create a new, empty load out based on the given chassis. TODO: Is anXBar really needed?
    * 
    * @param aChassi
    *           The chassis to base the load out on.
    * @param aXBar
    *           The {@link MessageXBar} to signal changes to this loadout on.
    */
   public Loadout(ChassisIS aChassi, MessageXBar aXBar){
      super(ComponentBuilder.getISComponentFactory(), aChassi, aXBar);
      if( aXBar != null ){
         aXBar.post(new LoadoutMessage(this, LoadoutMessage.Type.CREATE));
      }
   }

   /**
    * Will load a stock load out for the given variation name.
    * 
    * @param aString
    *           The name of the stock variation to load.
    * @param aXBar
    * @throws Exception
    */
   public Loadout(String aString, MessageXBar aXBar) throws Exception{
      this(ChassisDB.lookup(aString), aXBar);
      OperationStack operationStack = new OperationStack(0);
      operationStack.pushAndApply(new OpLoadStock(getChassis(), this, aXBar));
   }

   public Loadout(Loadout aLoadout, MessageXBar aXBar){
      super(ComponentBuilder.getISComponentFactory(), aLoadout);
      if( aXBar != null ){
         aXBar.post(new LoadoutMessage(this, LoadoutMessage.Type.CREATE));
      }
   }

   @Override
   public String toString(){
      if( getName().contains(getChassis().getNameShort()) )
         return getName();
      return getName() + " (" + getChassis().getNameShort() + ")";
   }

}
