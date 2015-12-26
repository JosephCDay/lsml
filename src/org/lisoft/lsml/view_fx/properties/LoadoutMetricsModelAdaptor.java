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
package org.lisoft.lsml.view_fx.properties;

import java.util.Collection;
import java.util.function.Predicate;

import org.lisoft.lsml.messages.EfficienciesMessage;
import org.lisoft.lsml.messages.ItemMessage;
import org.lisoft.lsml.messages.Message;
import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.messages.OmniPodMessage;
import org.lisoft.lsml.model.chassi.MovementProfile;
import org.lisoft.lsml.model.environment.Environment;
import org.lisoft.lsml.model.loadout.LoadoutBase;
import org.lisoft.lsml.model.loadout.LoadoutMetrics;
import org.lisoft.lsml.model.modifiers.Modifier;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;

/**
 * This class adapts {@link LoadoutMetrics} for JavaFX.
 * 
 * @author Li Song
 */
public class LoadoutMetricsModelAdaptor {
    private final LoadoutMetrics metrics;
    public final DoubleBinding   topSpeed;
    public final DoubleBinding   turnSpeed;
    public final DoubleBinding   torsoPitchSpeed;
    public final DoubleBinding   torsoYawSpeed;
    public final DoubleBinding   armPitchSpeed;
    public final DoubleBinding   armYawSpeed;
    public final DoubleBinding   torsoPitch;
    public final DoubleBinding   torsoYaw;
    public final DoubleBinding   armPitch;
    public final DoubleBinding   armYaw;
    public final IntegerBinding  jumpJetCount;
    public final IntegerBinding  jumpJetMax;

    public final IntegerBinding  heatSinkCount;
    public final DoubleBinding   heatCapacity;
    public final DoubleBinding   coolingRatio;
    public final DoubleBinding   timeToCool;
    private final MessageXBar    xBar;

    public LoadoutMetricsModelAdaptor(LoadoutMetrics aMetrics, LoadoutBase<?> aLoadout, MessageXBar aRcv) {
        metrics = aMetrics;
        xBar = aRcv;

        MovementProfile mp = aLoadout.getMovementProfile();
        Collection<Modifier> modifiers = aLoadout.getModifiers();

        // Update predicates
        Predicate<Message> itemsChanged = (aMsg) -> aMsg instanceof ItemMessage;
        Predicate<Message> effsChanged = (aMsg) -> aMsg instanceof EfficienciesMessage;
        Predicate<Message> omniPodChanged = (aMsg) -> aMsg instanceof OmniPodMessage;
        Predicate<Message> affectsHeatOrDamage = (aMsg) -> aMsg.affectsHeatOrDamage();
        Predicate<Message> engineOrEffsChanged = (aMsg) -> itemsChanged.test(aMsg) || effsChanged.test(aMsg);
        Predicate<Message> itemsOrPodsChanged = (aMsg) -> itemsChanged.test(aMsg) || omniPodChanged.test(aMsg);

        // Mobility
        topSpeed = new LsmlDoubleBinding(aRcv, () -> metrics.topSpeed.calculate(), engineOrEffsChanged);
        turnSpeed = new LsmlDoubleBinding(aRcv, () -> metrics.turningSpeed.calculate(), engineOrEffsChanged);
        torsoPitchSpeed = new LsmlDoubleBinding(aRcv, () -> metrics.torsoPitchSpeed.calculate(), engineOrEffsChanged);
        torsoYawSpeed = new LsmlDoubleBinding(aRcv, () -> metrics.torsoYawSpeed.calculate(), engineOrEffsChanged);
        armPitchSpeed = new LsmlDoubleBinding(aRcv, () -> metrics.armPitchSpeed.calculate(), engineOrEffsChanged);
        armYawSpeed = new LsmlDoubleBinding(aRcv, () -> metrics.armYawSpeed.calculate(), engineOrEffsChanged);
        jumpJetCount = new LsmlIntegerBinding(aRcv, () -> aLoadout.getJumpJetCount(), itemsOrPodsChanged);
        jumpJetMax = new LsmlIntegerBinding(aRcv, () -> aLoadout.getJumpJetsMax(), itemsOrPodsChanged);
        torsoPitch = new LsmlDoubleBinding(aRcv, () -> mp.getTorsoPitchMax(modifiers), engineOrEffsChanged);
        torsoYaw = new LsmlDoubleBinding(aRcv, () -> mp.getTorsoYawMax(modifiers), engineOrEffsChanged);
        armPitch = new LsmlDoubleBinding(aRcv, () -> mp.getArmPitchMax(modifiers), engineOrEffsChanged);
        armYaw = new LsmlDoubleBinding(aRcv, () -> mp.getArmYawMax(modifiers), engineOrEffsChanged);

        // Heat
        heatSinkCount = new LsmlIntegerBinding(aRcv, () -> aLoadout.getHeatsinksCount(), itemsOrPodsChanged);
        heatCapacity = new LsmlDoubleBinding(aRcv, () -> metrics.heatCapacity.calculate(), affectsHeatOrDamage);
        coolingRatio = new LsmlDoubleBinding(aRcv, () -> metrics.coolingRatio.calculate(), affectsHeatOrDamage);
        timeToCool = new LsmlDoubleBinding(aRcv, () -> metrics.timeToCool.calculate(), affectsHeatOrDamage);
    }

    /**
     * Changes the environment to use for heat calculations.
     * 
     * @param aEnvironment
     */
    public void changeEnvironment(Environment aEnvironment) {
        metrics.changeEnvironment(aEnvironment);

        // Provoke bindings that depend on heat to update.
        xBar.post(new Message() {
            @Override
            public boolean isForMe(LoadoutBase<?> aLoadout) {
                return true;
            }

            @Override
            public boolean affectsHeatOrDamage() {
                return true;
            }
        });
    }
}
