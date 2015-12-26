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
package org.lisoft.lsml.view.mechlab.loadoutframe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.lisoft.lsml.command.CmdSetArmor;
import org.lisoft.lsml.command.CmdSetOmniPod;
import org.lisoft.lsml.command.CmdToggleItem;
import org.lisoft.lsml.messages.ArmorMessage;
import org.lisoft.lsml.messages.ArmorMessage.Type;
import org.lisoft.lsml.messages.Message;
import org.lisoft.lsml.messages.MessageReceiver;
import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.model.DynamicSlotDistributor;
import org.lisoft.lsml.model.chassi.ArmorSide;
import org.lisoft.lsml.model.chassi.ComponentBase;
import org.lisoft.lsml.model.chassi.HardPointType;
import org.lisoft.lsml.model.chassi.Location;
import org.lisoft.lsml.model.chassi.OmniPod;
import org.lisoft.lsml.model.datacache.ItemDB;
import org.lisoft.lsml.model.datacache.OmniPodDB;
import org.lisoft.lsml.model.loadout.EquipResult;
import org.lisoft.lsml.model.loadout.LoadoutBase;
import org.lisoft.lsml.model.loadout.LoadoutOmniMech;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponentBase;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponentOmniMech;
import org.lisoft.lsml.util.CommandStack;
import org.lisoft.lsml.view.ProgramInit;
import org.lisoft.lsml.view.models.ArmorSpinnerModel;
import org.lisoft.lsml.view.models.BinaryAttributeModel;
import org.lisoft.lsml.view.render.ItemRenderer;
import org.lisoft.lsml.view.render.OmniPodRenderer;
import org.lisoft.lsml.view.render.StyleManager;
import org.lisoft.lsml.view.render.StyledComboBox;

public class PartPanel extends JPanel implements MessageReceiver {
    class ArmorPopupAdapter extends MouseAdapter {
        private final MessageXBar  xBar;
        private final CommandStack stack;

        public ArmorPopupAdapter(CommandStack aStack, MessageXBar aXBar) {
            stack = aStack;
            xBar = aXBar;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger())
                doPop(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger())
                doPop(e);
        }

        private void doPop(MouseEvent aEvent) {
            JPopupMenu menu = new JPopupMenu("Armor Options");
            menu.add(new JMenuItem(new AbstractAction("Allow automatic adjustment") {
                private static final long serialVersionUID = 7539044187157207692L;

                @Override
                public void actionPerformed(ActionEvent aE) {
                    try {
                        if (component.getInternalComponent().getLocation().isTwoSided()) {
                            stack.pushAndApply(new CmdSetArmor(xBar, loadout, component, ArmorSide.FRONT,
                                    component.getArmor(ArmorSide.FRONT), false));
                        }
                        else {
                            stack.pushAndApply(new CmdSetArmor(xBar, loadout, component, ArmorSide.ONLY,
                                    component.getArmorTotal(), false));
                        }
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(null, e);
                    }
                    xBar.post(new ArmorMessage(component, Type.ARMOR_DISTRIBUTION_UPDATE_REQUEST));
                }
            }));
            menu.show(aEvent.getComponent(), aEvent.getX(), aEvent.getY());
        }
    }

    private static final int              ARMOR_LABEL_WIDTH   = 30;
    private static final int              ARMOR_SPINNER_WIDTH = 20;

    private static final long             serialVersionUID    = -4399442572295284661L;

    private final JLabel                  frontArmorLabel;
    private final JLabel                  backArmorLabel;
    private final JLabel                  armorLabel;

    private final LoadoutBase<?>          loadout;
    private final ConfiguredComponentBase component;

    private final boolean                 canHaveHardpoints;
    private final ArmorPopupAdapter       armorPopupAdapter;
    private JSpinner                      frontSpinner;
    private JSpinner                      backSpinner;
    private JSpinner                      spinner;

    private final JComboBox<OmniPod>      omnipodSelection;
    private JPanel                        hardPointsPanel;

    private final JCheckBox               toggleHA;
    private final JCheckBox               toggleLAA;

    PartPanel(LoadoutBase<?> aLoadout, ConfiguredComponentBase aLoadoutPart, final MessageXBar aXBar,
            boolean aCanHaveHardpoints, DynamicSlotDistributor aSlotDistributor, JCheckBox aSymmetric,
            final CommandStack aStack) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        aXBar.attach(this);
        loadout = aLoadout;
        component = aLoadoutPart;
        canHaveHardpoints = aCanHaveHardpoints;
        armorPopupAdapter = new ArmorPopupAdapter(aStack, aXBar);

        if (aLoadoutPart.getInternalComponent().getLocation().isTwoSided()) {
            frontArmorLabel = new JLabel();
            backArmorLabel = new JLabel();
            armorLabel = null;
        }
        else {
            frontArmorLabel = null;
            backArmorLabel = null;
            armorLabel = new JLabel();
        }

        final Location location = aLoadoutPart.getInternalComponent().getLocation();
        if (aLoadoutPart instanceof ConfiguredComponentOmniMech
                && (location == Location.LeftArm || location == Location.RightArm)) {

            final ConfiguredComponentOmniMech ccom = (ConfiguredComponentOmniMech) aLoadoutPart;

            toggleLAA = new JCheckBox(ItemDB.LAA.getShortName());
            toggleLAA.setModel(new BinaryAttributeModel(aXBar) {
                @Override
                public void receive(Message aMsg) {
                    fireStateChanged();
                }

                @Override
                public boolean isSelected() {
                    return ccom.getToggleState(ItemDB.LAA);
                }

                @Override
                public boolean isEnabled() {
                    return EquipResult.SUCCESS == ccom.canToggleOn(ItemDB.LAA)
                            || ccom.getToggleState(ItemDB.LAA) == true;
                }

                @Override
                public void changeValue(boolean aEnabled) throws Exception {
                    aStack.pushAndApply(new CmdToggleItem(aXBar, loadout, ccom, ItemDB.LAA, aEnabled));
                }
            });
            add(toggleLAA);
            toggleLAA.setAlignmentX(Component.CENTER_ALIGNMENT);

            toggleHA = new JCheckBox(ItemDB.HA.getShortName());
            toggleHA.setModel(new BinaryAttributeModel(aXBar) {
                @Override
                public void receive(Message aMsg) {
                    fireStateChanged();
                }

                @Override
                public boolean isSelected() {
                    return ccom.getToggleState(ItemDB.HA);
                }

                @Override
                public boolean isEnabled() {
                    return EquipResult.SUCCESS == ccom.canToggleOn(ItemDB.HA) || ccom.getToggleState(ItemDB.HA) == true;
                }

                @Override
                public void changeValue(boolean aEnabled) throws Exception {
                    aStack.pushAndApply(new CmdToggleItem(aXBar, loadout, ccom, ItemDB.HA, aEnabled));
                }
            });

            add(toggleHA);
            toggleHA.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        else {
            toggleHA = null;
            toggleLAA = null;
        }

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        if (!ProgramInit.lsml().preferences.uiPreferences.getCompactMode()) {
            ComponentBase internalPart = aLoadoutPart.getInternalComponent();
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            internalPart.getLocation().longName() + " (" + (int) internalPart.getHitPoints() + " hp)"),
                    BorderFactory.createEmptyBorder(0, 2, 2, 4)));
        }

        if (LoadoutOmniMech.class.isAssignableFrom(aLoadout.getClass())) {
            final LoadoutOmniMech omniMech = (LoadoutOmniMech) aLoadout;
            // Omnimech
            final Collection<OmniPod> compatiblePods;
            if (location == Location.CenterTorso) {
                compatiblePods = Arrays.asList(omniMech.getComponent(location).getOmniPod());
            }
            else {
                compatiblePods = OmniPodDB.lookup(omniMech.getChassis(), location);
            }

            omnipodSelection = new JComboBox<>(new DefaultComboBoxModel<OmniPod>(new Vector<>(compatiblePods)) {
                @Override
                public Object getSelectedItem() {
                    return omniMech.getComponent(location).getOmniPod();
                }

                @Override
                public void setSelectedItem(Object aAnObject) {
                    try {
                        aStack.pushAndApply(new CmdSetOmniPod(aXBar, omniMech, (ConfiguredComponentOmniMech) component,
                                (OmniPod) aAnObject));
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(null, e);
                    }
                }
            });
            omnipodSelection.setRenderer(new OmniPodRenderer());
            omnipodSelection.addPopupMenuListener(new StyledComboBox(true, false));
            omnipodSelection.setFocusable(false);

            Dimension max = omnipodSelection.getMaximumSize();
            max.height = ItemRenderer.getItemHeight();
            omnipodSelection.setMaximumSize(max);
            omnipodSelection.setSelectedItem(((ConfiguredComponentOmniMech) component).getOmniPod());
            add(omnipodSelection);
        }
        else {
            omnipodSelection = null;
        }

        add(makeArmorPanel(aXBar, aSymmetric, aStack));

        if (canHaveHardpoints) {
            hardPointsPanel = new JPanel();
            updateHardpointsPanel(hardPointsPanel);
            add(hardPointsPanel);
        }

        // Critical slots
        PartList list = new PartList(aStack, aLoadout, aLoadoutPart, aXBar, aSlotDistributor);
        list.setFixedCellHeight(ItemRenderer.getItemHeight());
        list.setFixedCellWidth(ItemRenderer.getItemWidth());
        list.setFocusable(false);

        add(list);

        updateArmorPanel();
    }

    private void updateHardpointsPanel(JPanel aPanel) {
        BoxLayout layoutManager = new BoxLayout(aPanel, BoxLayout.LINE_AXIS);
        aPanel.removeAll();
        aPanel.setLayout(layoutManager);
        aPanel.add(Box.createVerticalStrut(3 * ItemRenderer.getItemHeight() / 2));

        for (HardPointType hp : HardPointType.values()) {
            JLabel label = new JLabel();
            StyleManager.styleHardpointLabel(label, component, hp);
            aPanel.add(label);
        }

        aPanel.add(Box.createHorizontalGlue());
        updateArmorPanel();
    }

    private JPanel makeArmorPanel(MessageXBar anXBar, JCheckBox aSymmetric, CommandStack aStack) {
        JPanel panel = new JPanel();
        Dimension labelDimension = new Dimension(ARMOR_LABEL_WIDTH, ItemRenderer.getItemHeight());
        Dimension spinnerDimension = new Dimension(ARMOR_SPINNER_WIDTH, 0);

        if (component.getInternalComponent().getLocation().isTwoSided()) {
            frontArmorLabel.setPreferredSize(labelDimension);
            backArmorLabel.setPreferredSize(labelDimension);

            frontSpinner = new JSpinner(
                    new ArmorSpinnerModel(loadout, component, ArmorSide.FRONT, anXBar, aSymmetric, aStack));
            frontSpinner.setMaximumSize(labelDimension);
            frontSpinner.getEditor().setPreferredSize(spinnerDimension);

            backSpinner = new JSpinner(
                    new ArmorSpinnerModel(loadout, component, ArmorSide.BACK, anXBar, aSymmetric, aStack));
            backSpinner.setMaximumSize(labelDimension);
            backSpinner.getEditor().setPreferredSize(spinnerDimension);

            JPanel frontPanel = new JPanel();
            frontPanel.setLayout(new BoxLayout(frontPanel, BoxLayout.LINE_AXIS));
            if (ProgramInit.lsml().preferences.uiPreferences.getCompactMode()) {
                frontPanel.add(new JLabel("F:"));
            }
            else {
                frontPanel.add(new JLabel("Front:"));
            }
            frontPanel.add(Box.createHorizontalGlue());
            frontPanel.add(frontSpinner);
            frontPanel.add(frontArmorLabel);

            JPanel backPanel = new JPanel();
            backPanel.setLayout(new BoxLayout(backPanel, BoxLayout.LINE_AXIS));
            if (ProgramInit.lsml().preferences.uiPreferences.getCompactMode()) {
                backPanel.add(new JLabel("B:"));
            }
            else {
                backPanel.add(new JLabel("Back:"));
            }
            backPanel.add(Box.createHorizontalGlue());
            backPanel.add(backSpinner);
            backPanel.add(backArmorLabel);

            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.add(frontPanel);
            panel.add(backPanel);
        }
        else {
            armorLabel.setPreferredSize(labelDimension);

            spinner = new JSpinner(
                    new ArmorSpinnerModel(loadout, component, ArmorSide.ONLY, anXBar, aSymmetric, aStack));
            spinner.setMaximumSize(labelDimension);
            spinner.getEditor().setPreferredSize(spinnerDimension);

            if (!ProgramInit.lsml().preferences.uiPreferences.getCompactMode()) {
                panel.add(new JLabel("Armor:"));
            }
            panel.add(Box.createHorizontalGlue());
            panel.add(spinner);
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.add(armorLabel);
        }

        panel.addMouseListener(armorPopupAdapter);

        return panel;
    }

    void updateArmorPanel() {
        if (armorLabel != null) {
            armorLabel.setText(" /" + Integer.valueOf(component.getInternalComponent().getArmorMax()));
            JTextField tf = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

            if (!component.hasManualArmor()) {
                armorLabel.setForeground(Color.GRAY);
                tf.setForeground(Color.GRAY);
            }
            else {
                armorLabel.setForeground(Color.BLACK);
                tf.setForeground(Color.BLACK);
            }
        }
        if (backArmorLabel != null && frontArmorLabel != null) {
            frontArmorLabel.setText(" /" + Integer.valueOf(component.getArmorMax(ArmorSide.FRONT)));
            backArmorLabel.setText(" /" + Integer.valueOf(component.getArmorMax(ArmorSide.BACK)));
            JTextField tff = ((JSpinner.DefaultEditor) frontSpinner.getEditor()).getTextField();
            JTextField tfb = ((JSpinner.DefaultEditor) backSpinner.getEditor()).getTextField();

            if (!component.hasManualArmor()) {
                frontArmorLabel.setForeground(Color.GRAY);
                backArmorLabel.setForeground(Color.GRAY);
                tff.setForeground(Color.GRAY);
                tfb.setForeground(Color.GRAY);
            }
            else {
                frontArmorLabel.setForeground(Color.BLACK);
                backArmorLabel.setForeground(Color.BLACK);
                tff.setForeground(Color.BLACK);
                tfb.setForeground(Color.BLACK);
            }
        }
    }

    @Override
    public void receive(Message aMsg) {
        if (aMsg.isForMe(loadout)) {
            if (aMsg instanceof ArmorMessage) {

                ArmorMessage msg = (ArmorMessage) aMsg;
                if (msg.type == Type.ARMOR_CHANGED) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateArmorPanel();
                        }
                    });
                }
                // else if (msg.type == Type.OmniPodChanged) {
                // if (canHaveHardpoints) {
                // updateHardpointsPanel(hardPointsPanel);
                // }
                // }
            }
        }
    }
}
