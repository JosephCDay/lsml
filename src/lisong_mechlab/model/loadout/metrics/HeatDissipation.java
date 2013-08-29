package lisong_mechlab.model.loadout.metrics;

import lisong_mechlab.model.loadout.Loadout;

public class HeatDissipation extends Metric{
   private final Loadout loadout;

   public HeatDissipation(Loadout aLoadout){
      loadout = aLoadout;
   }

   @Override
   public double calculate(){
      double ans = 0;
      int enginehs = 0;
      if( loadout.getEngine() != null ){
         enginehs = loadout.getEngine().getNumInternalHeatsinks();
      }

      // Engine internal HS count as true doubles
      ans += enginehs * (loadout.getUpgrades().hasDoubleHeatSinks() ? 0.2 : 0.1);

      // Other doubles count as 1.4
      ans += (loadout.getHeatsinksCount() - enginehs) * (loadout.getUpgrades().hasDoubleHeatSinks() ? 0.14 : 0.1);
      return ans * loadout.getEfficiencies().getHeatDissipationModifier();
   }
}
