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

import java.awt.Toolkit;

import javax.swing.JCheckBox;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import lisong_mechlab.model.chassi.ArmorSide;
import lisong_mechlab.model.chassi.Part;
import lisong_mechlab.model.loadout.LoadoutPart;
import lisong_mechlab.model.loadout.LoadoutPart.Message.Type;
import lisong_mechlab.model.loadout.OperationStack;
import lisong_mechlab.model.loadout.OperationStack.Operation;
import lisong_mechlab.util.MessageXBar;
import lisong_mechlab.util.MessageXBar.Message;

public class ArmorSpinner extends SpinnerNumberModel implements MessageXBar.Reader{
   private static final long serialVersionUID = 2130487332299251881L;
   private final LoadoutPart part;
   private final ArmorSide   side;
   private final JCheckBox   symmetric;
   private final OperationStack opStack;

   public ArmorSpinner(LoadoutPart aPart, ArmorSide anArmorSide, MessageXBar anXBar, JCheckBox aSymmetric, OperationStack anOperationStack){
      part = aPart;
      side = anArmorSide;
      symmetric = aSymmetric;
      anXBar.attach(this);
      opStack = anOperationStack;
   }

   @Override
   public Object getNextValue(){
      if( part.getArmor(side) < part.getArmorMax(side) ){
         return Integer.valueOf(part.getArmor(side) + 1);
      }
      return Integer.valueOf(part.getArmor(side) + 1);
   }

   @Override
   public Object getPreviousValue(){
      if( part.getArmor(side) < 1 )
         return null;
      return Integer.valueOf(part.getArmor(side) - 1);
   }

   @Override
   public Object getValue(){
      return Integer.valueOf(part.getArmor(side));
   }

   @Override
   public void setValue(Object arg0){
      try{
         int armor = ((Integer)arg0).intValue();
         opStack.pushAndApply(part.new SetArmorOperation(side, armor));
         
         Part otherSide = part.getInternalPart().getType().oppositeSide();
         if( symmetric.isSelected() && otherSide != null ){
            Operation op2 = part.getLoadout().getPart(otherSide). new SetArmorOperation(side, armor);
            opStack.pushAndApply(op2);
         }
         fireStateChanged();
      }
      catch( IllegalArgumentException exception ){
         //TODO: Handle failed case better!
         Toolkit.getDefaultToolkit().beep();
      }
   }

   @Override
   public void receive(Message aMsg){
      if( aMsg.isForMe(part.getLoadout()) && aMsg instanceof LoadoutPart.Message ){
         LoadoutPart.Message message = (LoadoutPart.Message)aMsg;
         if( message.type == Type.ArmorChanged ){
            SwingUtilities.invokeLater(new Runnable(){
               @Override
               public void run(){
                  fireStateChanged();
               }
            });
         }
      }
   }

}
