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
package org.lisoft.lsml.model.metrics.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.lisoft.lsml.math.probability.BinomialDistribution;
import org.lisoft.lsml.model.item.BallisticWeapon;
import org.lisoft.lsml.model.modifiers.Modifier;
import org.lisoft.lsml.util.Pair;

import static java.lang.Math.*;

/**
 * This class calculates the burst damage to a time for a weapon that is capable of double fire, such as the Ultra AC/5.
 *
 * @author Li Song
 */
public class DoubleFireBurstSignal implements IntegratedSignal {
    private final BallisticWeapon weapon;
    private final double range;
    private final Collection<Modifier> modifiers;
    private double sumPk;


    /**
     * @param aWeapon
     *            The weapon to generate the signal for.
     * @param aPilotModules
     *            A {@link Collection} of modifiers that could affect the signal.
     * @param aRange
     *            The range of the weapon to calculate the signal for.
     */
    public DoubleFireBurstSignal(BallisticWeapon aWeapon, Collection<Modifier> aPilotModules, double aRange) {
        if (!aWeapon.canDoubleFire()) {
            throw new IllegalArgumentException(
                    "DoubleFireBurstSignal is only usable with weapons that can actually double fire!");
        }
        if (aRange < 0.0) {
            throw new IllegalArgumentException("Range must be larger than or equal to 0.0m!");
        }
        weapon = aWeapon;
        range = aRange;
        modifiers = aPilotModules;
    }

    @Override
    public double integrateFromZeroTo(double aTime) {
        final double shots = shots(aTime);
        final double damage = weapon.getDamagePerShot();
        final double rangeFactor = weapon.getRangeEffectiveness(range, modifiers);
        return shots * damage * rangeFactor;
    }

    /*
      Big ooof, math time:
      If jam time and cooldown were the same, then we'd have a binomial distribution of jams and normal shots.
      Then we'd compute the number of simulation events as: n=duration/cooldown, and we'd compute the probability Pk,
      to have exactly k jams as the outcome of n events for k=0...n as: Pk = n_choose_k * p^(k)*(1-p)^(n-k).

      Then for each k, we then compute the probable shots taken as Zk = (shotsWhenJammed * k + shotsNotJammed * (n-k))*Pk.

      The total shots taken during the time is then, sum(Zk, for all k).

      Unfortunately, jam time and cooldown are not identical most of the time and that means that the binomial
      distribution is cut short on either end. This means that we need to do some additional maths...

      We need an iterative process that truncates the binomial distribution as appropriate (assuming cooldown<jam):
      Start with n = time/cooldown.  That means that all shots hit, compute the probability of this as Pk above,
      compute Zk and add to the sum. Now compute n1 = (n*cooldown - jamTime), this is the number of events we get
      if we have one jam, compute Pk using n1, instead of n, and then Zk and add to the sum. Repeat by increasing the
      number of jams and reducing number on normal shots, computed Zk and repeat until all shots are jams.
     */

    double shots(double aDuration){
        // RAC type weapons have a jam-free period when they first spin up, we account for that here:
        final int jamFreeCooldowns = jamFreeCooldowns();
        final int jamFreeShots = jamFreeCooldowns * (1 + weapon.getShotsDuringCooldown());
        // ...and shorten the simulation duration by the matching time (ignoring ramp-up here, as the player is assumed
        // to have pre-spun the weapon entering the burst).
        aDuration -= jamFreeCooldowns * weapon.getRawSecondsPerShot(modifiers);

        // A jam consists of clearing the jam, followed by a jam free period, plus the cooldown of the weapon that
        // gets interrupted by the jam, and resumed when the jam clears.
        final double jammedEventDuration = weapon.getJamTime(modifiers) + weapon.getJamRampUpTime(modifiers) + weapon.getRawSecondsPerShot(modifiers);
        final double normalEventDuration = weapon.getRawSecondsPerShot(modifiers);

        // Note that we must ceil here, if 0<duration<cooldown, we still get a shot off, but if duration=cooldown
        // we chose to get only one shot. So in that situation using +1 instead of ceil is wrong.
        final int maxShots = (int) ceil(aDuration/normalEventDuration);

        // We're summing potentially many small floating point numbers, in order to preserve accuracy we need to
        // recursively sum them, small to big.
        final PriorityQueue<Double> sumZk = new PriorityQueue<>();

        // For verification purposes we also compute the sum of the probabilities to use in tests to do invasive
        // verification.
        final PriorityQueue<Double> sumPkPq = new PriorityQueue<>();

        final double epsilon = ulp(aDuration)*100;

        // We iterate over all the numbers of possible normal events, this automatically gets us all the combinations
        // of jam events as well.
        int normalEvents = maxShots;
        while(normalEvents >= 0){
            // There are three cases we must consider:
            // 1) The last event was a jam and the remaining time was less than the jam event duration
            // 2) The last event was a normal shot and the remaining time was less than the cooldown
            // 3) The sum time of jams and normals exactly divides the time frame we're looking at.

            final double sumNormalDuration = normalEvents * normalEventDuration;
            final double sumRemainderDuration = max(0.0, aDuration - sumNormalDuration);
            final int jammedEvents = (int) floor(sumRemainderDuration / jammedEventDuration);
            final double sumJammedDuration = jammedEvents * jammedEventDuration;
            final double tailTime = aDuration - (sumNormalDuration + sumJammedDuration);

            if(tailTime <= epsilon){
                // The normal and jammed events perfectly fill the available time
                Pair<Double,Double> result = pShots(jammedEvents, normalEvents, false, false,0);
                sumZk.add(result.first);
                sumPkPq.add(result.second);
            }else{
                // After considering the sum whole events, there is time left in the timeline, tailTime, i.e. we have
                // not accounted for all events that took place yet.
                //
                // Given that the number on normal events must be constant the following could have happened:
                // 1) The last event was a partial jam (always possible)
                // 2) The last event was actually a normal event, and the slack time is actually larger than calculated
                //    and there is an additional jam event that we initially didn't consider. Only possible if
                //    slack time + normalDuration >= jam duration


                // Case 1) We have "normalEvents" whole normal events, and a tail jam
                {
                    Pair<Double, Double> result = pShots(jammedEvents + 1, normalEvents, true, false, tailTime);
                    sumZk.add(result.first);
                    sumPkPq.add(result.second);
                }

                if (normalEvents>0 && aDuration - ((normalEvents-1) * normalEventDuration + sumJammedDuration+epsilon) >= jammedEventDuration){
                    // Case 2) is possible and must be included
                    Pair<Double,Double> result = pShots(jammedEvents+1, normalEvents, false, true,0);
                    sumZk.add(result.first);
                    sumPkPq.add(result.second);
                }
            }
            normalEvents--;
        }

        // ACCURATELY compute the sum of many small floating point values
        while(sumZk.size()>1){ sumZk.add(sumZk.remove() + sumZk.remove()); }
        while(sumPkPq.size()>1){ sumPkPq.add(sumPkPq.remove()+sumPkPq.remove()); }

        // sumPk *should* always be 1.0 if everything is implemented correctly. However, in the case that it isn't,
        // normalizing the statistical result by it will empirically produce slightly less wrong results.
        sumPk = sumPkPq.remove();
        return jamFreeShots + sumZk.remove()/sumPk;
    }

    Pair<Double, Double> pShots(int jams, int normals, boolean tailJam, boolean tailShot, double tailTime){
        // Probability of a branch with the given number of jams and normal shots.
        final double jamProbability = weapon.getJamProbability(modifiers);
        final BigDecimal probabilityOfJams = BigDecimal.valueOf(jamProbability).pow(jams);
        final BigDecimal probabilityOfNormals = BigDecimal.valueOf(1.0 - jamProbability).pow(normals);
        final BigDecimal probabilityOfBranch = probabilityOfJams.multiply(probabilityOfNormals);
        final BigInteger numberOfBranches;

        if(tailJam){
            // Because we have a tail jam, we actually only permute k-1 of the jams, and the last of
            // the n events is the tail jam. So we need n-1 choose k-1.
            numberOfBranches = BinomialDistribution.nChooseKLargeNumbers(normals+jams-1, jams-1);
        }else if(tailShot){
            numberOfBranches = BinomialDistribution.nChooseKLargeNumbers(normals+jams-1, jams);
        }else{
            numberOfBranches = BinomialDistribution.nChooseKLargeNumbers(normals+jams, jams);
        }

        final double Pk = new BigDecimal(numberOfBranches).multiply(probabilityOfBranch).doubleValue();

        final int jamFreeCooldowns = jamFreeCooldowns();
        final int shotsNormally = 1 + weapon.getShotsDuringCooldown();
        final int shotsDuringJam = 1 + jamFreeCooldowns*shotsNormally;
        double Zk = Pk*(jams*shotsDuringJam + normals*shotsNormally);
        if(tailJam){
            // The jamEventDuration can include a jam free period where shots are fired,
            // this computes how many (if any) of those could be taken.

            final double tailTimeAfterJamAndRampUp = max(0.0,tailTime - weapon.getJamTime(modifiers) - weapon.getRampUpTime(modifiers));
            Zk += Pk*jamFreeCooldowns(tailTimeAfterJamAndRampUp)*shotsNormally;
        }
        return new Pair<>(Zk, Pk);
    }

    private int jamFreeCooldowns(){
        return jamFreeCooldowns(Double.POSITIVE_INFINITY);
    }

    private int jamFreeCooldowns(double maxJamFreeTime){
        final double jamFreeTime = min(maxJamFreeTime, max(0.0, weapon.getJamRampUpTime(modifiers) - weapon.getRampUpTime(modifiers)));
        final int jamFreeCooldowns = (int) ceil(jamFreeTime / weapon.getRawSecondsPerShot(modifiers));
        return jamFreeCooldowns;
    }

    protected double getProbabilityMass(){
        return sumPk;
    }
}
