package lisong_mechlab.model.loadout;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import lisong_mechlab.model.loadout.Efficiencies.Message.Type;
import lisong_mechlab.util.MessageXBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EfficienciesTest{
   @Mock
   private MessageXBar  xBar;
   private Efficiencies cut;

   @Before
   public void setup(){
      cut = new Efficiencies(xBar);
   }

   @Test
   public void testSetHasSpeedTweak() throws Exception{
      // Default false
      assertEquals(false, cut.hasSpeedTweak());
      verifyZeroInteractions(xBar);

      // We want messages too!
      for(boolean b : new boolean[] {true, false}){
         cut.setSpeedTweak(b);
         assertEquals(b, cut.hasSpeedTweak());
         verify(xBar).post(new Efficiencies.Message(cut, Type.Changed));
         reset(xBar);
      }

      // No messages if there was no change.
      for(boolean b : new boolean[] {true, false}){
         cut.setSpeedTweak(b);
         reset(xBar);
         cut.setSpeedTweak(b);
         verifyZeroInteractions(xBar);
      }
   }

   @Test
   public void testSetHasCoolRun() throws Exception{
      // Default false
      assertEquals(false, cut.hasCoolRun());
      verifyZeroInteractions(xBar);

      // We want messages too!
      for(boolean b : new boolean[] {true, false}){
         cut.setCoolRun(b);
         assertEquals(b, cut.hasCoolRun());
         verify(xBar).post(new Efficiencies.Message(cut, Type.Changed));
         reset(xBar);
      }

      // No messages if there was no change.
      for(boolean b : new boolean[] {true, false}){
         cut.setCoolRun(b);
         reset(xBar);
         cut.setCoolRun(b);
         verifyZeroInteractions(xBar);
      }
   }

   @Test
   public void testSetHasHeatContainment() throws Exception{
      // Default false
      assertEquals(false, cut.hasHeatContainment());
      verifyZeroInteractions(xBar);

      // We want messages too!
      for(boolean b : new boolean[] {true, false}){
         cut.setHeatContainment(b);
         assertEquals(b, cut.hasHeatContainment());
         verify(xBar).post(new Efficiencies.Message(cut, Type.Changed));
         reset(xBar);
      }

      // No messages if there was no change.
      for(boolean b : new boolean[] {true, false}){
         cut.setHeatContainment(b);
         reset(xBar);
         cut.setHeatContainment(b);
         verifyZeroInteractions(xBar);
      }
   }

   @Test
   public void testSetHasDoubleBasics() throws Exception{
      // Default false
      assertEquals(false, cut.hasDoubleBasics());
      verifyZeroInteractions(xBar);

      // We want messages too!
      for(boolean b : new boolean[] {true, false}){
         cut.setDoubleBasics(b);
         assertEquals(b, cut.hasDoubleBasics());
         verify(xBar).post(new Efficiencies.Message(cut, Type.Changed));
         reset(xBar);
      }

      // No messages if there was no change.
      for(boolean b : new boolean[] {true, false}){
         cut.setDoubleBasics(b);
         reset(xBar);
         cut.setDoubleBasics(b);
         verifyZeroInteractions(xBar);
      }
   }

   @Test
   public void testGetHeatCapacityModifier() throws Exception{
      assertEquals(1.0, cut.getHeatCapacityModifier(), 0.0);

      // These don't affect heat capacity
      cut.setCoolRun(true);
      cut.setSpeedTweak(true);
      cut.setDoubleBasics(true); // Only if we have heat containment
      assertEquals(1.0, cut.getHeatCapacityModifier(), 0.0);

      cut.setCoolRun(false);
      cut.setSpeedTweak(false);
      cut.setDoubleBasics(false);

      // These do
      cut.setHeatContainment(true);
      assertEquals(1.1, cut.getHeatCapacityModifier(), 0.0);
      cut.setDoubleBasics(true);
      assertEquals(1.2, cut.getHeatCapacityModifier(), 0.0);
   }

   @Test
      public void testGetHeatDissipationModifier() throws Exception{
         assertEquals(1.0, cut.getHeatDissipationModifier(), 0.0);
   
         // These don't affect heat capacity
         cut.setHeatContainment(true);
         cut.setSpeedTweak(true);
         cut.setDoubleBasics(true); // Only if we have heat containment
         assertEquals(1.0, cut.getHeatDissipationModifier(), 0.0);
   
         cut.setHeatContainment(false);
         cut.setSpeedTweak(false);
         cut.setDoubleBasics(false);
   
         // These do
         cut.setCoolRun(true);
         assertEquals(1.075, cut.getHeatDissipationModifier(), 0.0);
         cut.setDoubleBasics(true);
         assertEquals(1.15, cut.getHeatDissipationModifier(), 0.0);
      }

   @Test
   public void testGetSpeedModifier() throws Exception{
      assertEquals(1.0, cut.getSpeedModifier(), 0.0);

      // These don't affect heat capacity
      cut.setHeatContainment(true);
      cut.setCoolRun(true);
      cut.setDoubleBasics(true);
      assertEquals(1.0, cut.getSpeedModifier(), 0.0);

      // These do
      cut.setSpeedTweak(true);
      assertEquals(1.1, cut.getSpeedModifier(), 0.0);
      cut.setDoubleBasics(false);
      assertEquals(1.1, cut.getSpeedModifier(), 0.0);
   }

}
