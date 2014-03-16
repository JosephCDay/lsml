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
package lisong_mechlab.view.mechlab;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import lisong_mechlab.model.DynamicSlotDistributor;
import lisong_mechlab.model.item.Engine;
import lisong_mechlab.model.item.HeatSink;
import lisong_mechlab.model.item.Internal;
import lisong_mechlab.model.item.Item;
import lisong_mechlab.model.loadout.part.AddItemOperation;
import lisong_mechlab.model.loadout.part.LoadoutPart;
import lisong_mechlab.model.loadout.part.LoadoutPart.Message.Type;
import lisong_mechlab.model.loadout.part.RemoveItemOperation;
import lisong_mechlab.model.metrics.CriticalItemDamage;
import lisong_mechlab.model.metrics.CriticalStrikeProbability;
import lisong_mechlab.model.metrics.ItemEffectiveHP;
import lisong_mechlab.model.upgrades.Upgrades;
import lisong_mechlab.util.MessageXBar;
import lisong_mechlab.util.MessageXBar.Message;
import lisong_mechlab.util.OperationStack;
import lisong_mechlab.util.Pair;
import lisong_mechlab.view.ItemTransferHandler;
import lisong_mechlab.view.render.StyleManager;

public class PartList extends JList<Item>{
   private static final long               serialVersionUID = 5995694414450060827L;
   private final LoadoutPart               part;
   private final DynamicSlotDistributor    slotDistributor;
   private OperationStack                  opStack;

   private final DecimalFormat             df               = new DecimalFormat("###.#");
   private final DecimalFormat             df2               = new DecimalFormat("###.##");
   private final ItemEffectiveHP           effectiveHP;
   private final CriticalItemDamage        criticalItemDamage;
   private final CriticalStrikeProbability criticalStrikeProbability;

   private enum ListEntryType{
      Empty, MultiSlot, Item, EngineHeatSink, LastSlot
   }

   private class Renderer extends JLabel implements ListCellRenderer<Object>{
      private static final long serialVersionUID = -8157859670319431469L;

      void setTooltipForItem(Item aItem){
         if( aItem instanceof Internal ){
            setToolTipText("");
            return;
         }

         StringBuilder sb = new StringBuilder();

         sb.append("<html>");
         sb.append(aItem.getName());
         if( !aItem.getName().equals(aItem.getShortName(null)) ){
            sb.append(" (").append(aItem.getShortName(null)).append(")");
         }

         sb.append("<p>");
         sb.append("Critical victim probability: ").append(df.format(100 * criticalStrikeProbability.calculate(aItem))).append("%");
         sb.append("<br/>");
         sb.append("Critical victim multiplicity: ").append(df2.format(criticalItemDamage.calculate(aItem)));
         sb.append("</p>");

         sb.append("<p>");
         sb.append("HP: ").append(aItem.getHealth());
         sb.append("<br/>");
         sb.append("SI-EHP: ").append(df.format(effectiveHP.calculate(aItem)));
         sb.append("</p>");

         sb.append("<br/>");
         sb.append("<div style='width:300px'>")
           .append("<p>")
           .append("<b>Critical victim probability</b> is the chance that any one hit on the component will critically hit this item dealing damage to it.")
           .append("If the weapon shooting does equal to, or more damage than the HP of this item, the item will break.")
           .append("</p><p>")
           .append("<b>Critical victim multiplicity</b> is the amount of damage the item will take (statistically) for every one damage dealt to the component. ")
           .append("This mainly applies to lasers and does not include increased chance to critically hit from MG and LB 10-X AC and Flamers.")
           .append("</p><p>")
           .append("<b>SI-EHP</b> is the Statistical, Infinitesmal Effective-HP of this component. Under the assumption that damage is ")
           .append("applied in small chunks (lasers) this is how much damage the component can take before this item breaks (statistically).")
           .append("</p>").append("</div>");

         sb.append("</html>");
         setToolTipText(sb.toString());
      }

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
         JList.DropLocation dropLocation = list.getDropLocation();
         if( dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index ){
            setCursor(null);
         }

         Pair<ListEntryType, Item> pair = ((Model)getModel()).getElementTypeAt(index);
         setBorder(BorderFactory.createEmptyBorder());
         Item item = pair.second;
         switch( pair.first ){
            case Empty:{
               if( isDynArmor(index) ){
                  StyleManager.styleDynamicEntry(this);
                  setText(Model.DYN_ARMOR);
               }
               else if( isDynStructure(index) ){
                  StyleManager.styleDynamicEntry(this);
                  setText(Model.DYN_STRUCT);
               }
               else{
                  StyleManager.styleItem(this);
                  setText(Model.EMPTY);
               }
               setToolTipText("");
               break;
            }
            case Item:{
               setTooltipForItem(item);
               setText(item.getName());
               if( item.getNumCriticalSlots(null) == 1 ){
                  StyleManager.styleItem(this, item);
               }
               else{
                  StyleManager.styleItemTop(this, item);
               }
               break;
            }
            case LastSlot:{
               setText(Model.MULTISLOT);
               setTooltipForItem(item);
               StyleManager.styleItemBottom(this, item);
               break;
            }
            case MultiSlot:{
               setText(Model.MULTISLOT);
               setTooltipForItem(item);
               StyleManager.styleItemMiddle(this, item);
               break;
            }
            case EngineHeatSink:{
               setTooltipForItem(item);
               setText(Model.HEATSINKS_STRING + part.getNumEngineHeatsinks() + "/" + part.getNumEngineHeatsinksMax());
               StyleManager.styleItemBottom(this, item);
               break;
            }
         }
         /*
          * if( isSelected && pair.first != ListEntryType.Empty ){ setBackground(getBackground().brighter()); }
          */
         return this;
      }

      private boolean isDynStructure(int aIndex){
         int freeSlotOrdinal = aIndex - part.getNumCriticalSlotsUsed() - slotDistributor.getDynamicArmorSlots(part);
         int dynStructNum = slotDistributor.getDynamicStructureSlots(part);
         return freeSlotOrdinal >= 0 && freeSlotOrdinal < dynStructNum;
      }

      private boolean isDynArmor(int aIndex){
         int freeSlotOrdinal = aIndex - part.getNumCriticalSlotsUsed();
         int dynArmorNum = slotDistributor.getDynamicArmorSlots(part);
         return freeSlotOrdinal < dynArmorNum;
      }
   }

   private class Model extends AbstractListModel<Item> implements MessageXBar.Reader{
      private static final String HEATSINKS_STRING = "HEATSINKS: ";
      private static final String EMPTY            = "EMPTY";
      private static final String MULTISLOT        = "";
      private static final String DYN_ARMOR        = "DYNAMIC ARMOR";
      private static final String DYN_STRUCT       = "DYNAMIC STRUCTURE";
      private static final long   serialVersionUID = 2438473891359444131L;
      private final MessageXBar   xBar;

      Model(MessageXBar aXBar){
         xBar = aXBar;
         xBar.attach(this);
      }

      boolean putElement(Item anItem, int anIndex, boolean aShouldReplace){
         Pair<ListEntryType, Item> target = getElementTypeAt(anIndex);
         switch( target.first ){
            case EngineHeatSink:{
               if( anItem instanceof HeatSink && part.getLoadout().canEquip(anItem) && part.canEquip(anItem) ){
                  opStack.pushAndApply(new AddItemOperation(xBar, part, anItem));
                  return true;
               }
               return false;
            }
            case LastSlot: // Fall through
            case Item: // Fall through
            case MultiSlot:{
               // Drop on existing component, try to replace it if we should, otherwise just add it to the component.
               if( aShouldReplace && !(anItem instanceof HeatSink && target.second instanceof Engine) ){
                  opStack.pushAndApply(new RemoveItemOperation(xBar, part, target.second));
               }
               // Fall through
            }
            case Empty:{
               if(part.getLoadout().canEquip(anItem) && part.canEquip(anItem) ){
                  opStack.pushAndApply(new AddItemOperation(xBar, part, anItem));
                  return true;
               }
               return false;
            }
            default:
               break;
         }
         return false;
      }

      Pair<ListEntryType, Item> getElementTypeAt(int arg0){
         List<Item> items = new ArrayList<>(part.getItems());
         int numEngineHs = part.getNumEngineHeatsinks();
         boolean foundhs = true;
         while( numEngineHs > 0 && !items.isEmpty() && foundhs ){
            foundhs = false;
            for(Item item : items){
               if( item instanceof HeatSink ){
                  items.remove(item);
                  numEngineHs--;
                  foundhs = true;
                  break;
               }
            }
         }

         if( items.isEmpty() )
            return new Pair<ListEntryType, Item>(ListEntryType.Empty, null);

         int itemsIdx = 0;
         Item item = items.get(itemsIdx);
         itemsIdx++;

         int spaceLeft = item.getNumCriticalSlots(null);
         for(int slot = 0; slot < arg0; ++slot){
            spaceLeft--;
            if( spaceLeft == 0 ){
               if( itemsIdx < items.size() ){
                  item = items.get(itemsIdx);
                  itemsIdx++;
                  spaceLeft = item.getNumCriticalSlots(null);
               }
               else
                  return new Pair<ListEntryType, Item>(ListEntryType.Empty, null);
            }
         }
         if( spaceLeft == 1 && item.getNumCriticalSlots(null) > 1 ){
            if( item instanceof Engine )
               return new Pair<ListEntryType, Item>(ListEntryType.EngineHeatSink, item);
            return new Pair<ListEntryType, Item>(ListEntryType.LastSlot, item);
         }
         if( spaceLeft == item.getNumCriticalSlots(null) )
            return new Pair<ListEntryType, Item>(ListEntryType.Item, item);
         if( spaceLeft > 0 )
            return new Pair<ListEntryType, Item>(ListEntryType.MultiSlot, item);
         return new Pair<ListEntryType, Item>(ListEntryType.Empty, null);
      }

      @Override
      public Item getElementAt(int arg0){
         Pair<ListEntryType, Item> target = getElementTypeAt(arg0);
         if( target.first == ListEntryType.Item )
            return getElementTypeAt(arg0).second;
         return null;
      }

      @Override
      public int getSize(){
         return part.getInternalPart().getNumCriticalslots();
      }

      @Override
      public void receive(Message aMsg){
         if( !aMsg.isForMe(PartList.this.part.getLoadout()) ){
            return;
         }

         // Only update on item changes or upgrades
         if( aMsg instanceof LoadoutPart.Message || aMsg instanceof Upgrades.Message ){
            if( aMsg instanceof LoadoutPart.Message && ((LoadoutPart.Message)aMsg).type == Type.ArmorChanged ){
               return; // Don't react to armor changes
            }
            fireContentsChanged(this, 0, part.getInternalPart().getNumCriticalslots());
         }
      }
   }

   PartList(OperationStack aStack, final LoadoutPart aLoadoutPart, final MessageXBar anXBar, DynamicSlotDistributor aSlotDistributor){
      slotDistributor = aSlotDistributor;
      opStack = aStack;
      part = aLoadoutPart;
      effectiveHP = new ItemEffectiveHP(part);
      criticalItemDamage = new CriticalItemDamage(part);
      criticalStrikeProbability = new CriticalStrikeProbability(part);
      setModel(new Model(anXBar));
      setDragEnabled(true);
      setDropMode(DropMode.ON);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setTransferHandler(new ItemTransferHandler());
      setCellRenderer(new Renderer());

      addFocusListener(new FocusAdapter(){
         @Override
         public void focusLost(FocusEvent e){
            clearSelection();
         }
      });

      addKeyListener(new KeyAdapter(){
         @Override
         public void keyPressed(KeyEvent aArg0){
            if( aArg0.getKeyCode() == KeyEvent.VK_DELETE ){
               for(Pair<Item, Integer> itemPair : getSelectedItems()){
                  opStack.pushAndApply(new RemoveItemOperation(anXBar, aLoadoutPart, itemPair.first));
               }
            }
         }
      });

      addMouseListener(new MouseAdapter(){
         @Override
         public void mouseClicked(MouseEvent e){
            if( SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2 ){
               for(Pair<Item, Integer> itemPair : getSelectedItems()){
                  opStack.pushAndApply(new RemoveItemOperation(anXBar, aLoadoutPart, itemPair.first));
               }
            }
         }
      });
   }

   public List<Pair<Item, Integer>> getSelectedItems(){
      List<Pair<Item, Integer>> items = new ArrayList<>();
      int[] idxs = getSelectedIndices();
      for(int i : idxs){
         Pair<ListEntryType, Item> pair = ((Model)getModel()).getElementTypeAt(i);
         int rootId = i;
         while( rootId >= 0 && ((Model)getModel()).getElementAt(rootId) == null )
            rootId--;

         switch( pair.first ){
            case Empty:
               break;
            case EngineHeatSink:
               if( part.getNumEngineHeatsinks() > 0 ){
                  Item heatSink = part.getLoadout().getUpgrades().getHeatSink().getHeatSinkType();
                  items.add(new Pair<Item, Integer>(heatSink, i));
               }
               break;
            case Item:
            case LastSlot:
            case MultiSlot:
               items.add(new Pair<Item, Integer>(pair.second, rootId));
               break;
            default:
               break;

         }
      }
      return items;
   }

   public LoadoutPart getPart(){
      return part;
   }

   public void putElement(Item aItem, int aDropIndex, boolean aFirst){
      ((Model)getModel()).putElement(aItem, aDropIndex, aFirst);
   }
}
