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
package org.lisoft.lsml.view_fx.loadout.component;

import java.util.Arrays;
import java.util.Collection;

import org.lisoft.lsml.command.CmdAddItem;
import org.lisoft.lsml.command.CmdRemoveItem;
import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.model.DynamicSlotDistributor;
import org.lisoft.lsml.model.chassi.ChassisOmniMech;
import org.lisoft.lsml.model.chassi.HardPointType;
import org.lisoft.lsml.model.chassi.Location;
import org.lisoft.lsml.model.chassi.OmniPod;
import org.lisoft.lsml.model.datacache.ItemDB;
import org.lisoft.lsml.model.datacache.OmniPodDB;
import org.lisoft.lsml.model.item.Item;
import org.lisoft.lsml.model.loadout.EquipResult;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponentBase;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponentOmniMech;
import org.lisoft.lsml.util.CommandStack;
import org.lisoft.lsml.view_fx.LiSongMechLab;
import org.lisoft.lsml.view_fx.drawers.ComponentItemsCell;
import org.lisoft.lsml.view_fx.drawers.OmniPodListCell;
import org.lisoft.lsml.view_fx.properties.LoadoutModelAdaptor;
import org.lisoft.lsml.view_fx.properties.LoadoutModelAdaptor.ComponentModel;
import org.lisoft.lsml.view_fx.style.HardPointFormatter;
import org.lisoft.lsml.view_fx.style.StyleManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * This is the controller for the component pane. The component pane shows the state of one component in the loadout.
 * 
 * @author Li Song
 */
public class ComponentPaneController {
    public static final int         ITEM_WIDTH = 150;

    @FXML
    private TitledPane              rootPane;
    @FXML
    private ItemView<Item>          itemView;
    @FXML
    private ToggleButton            toggleLAA;
    @FXML
    private ToggleButton            toggleHA;
    @FXML
    private VBox                    container;
    @FXML
    private HBox                    armorBox;
    @FXML
    private HBox                    armorBoxBack;
    @FXML
    private Label                   armorLabel;
    @FXML
    private Label                   armorLabelBack;
    @FXML
    private Label                   armorMax;
    @FXML
    private Label                   armorMaxBack;
    @FXML
    private Spinner<Integer>        armorSpinner;
    @FXML
    private Spinner<Integer>        armorSpinnerBack;

    private CommandStack            stack;
    private LoadoutModelAdaptor     model;
    private MessageXBar             xBar;
    private Location                location;
    private ConfiguredComponentBase component;

    @FXML
    private HBox                    hardPointContainer;

    @FXML
    private ComboBox<OmniPod>       omniPodSelection;

    /**
     * Sets up this component. Must be called before this component is usable.
     * 
     * @param aMessageXBar
     *            A {@link MessageXBar} to send and receive messages on.
     * @param aStack
     *            The {@link CommandStack} to use for doing commands.
     * @param aModel
     *            The loadout to get the component from.
     * @param aLocation
     *            The location of the loadout to get component for.
     * @param aDistributor
     */
    public void setComponent(MessageXBar aMessageXBar, CommandStack aStack, LoadoutModelAdaptor aModel,
            Location aLocation, DynamicSlotDistributor aDistributor) {
        stack = aStack;
        model = aModel;
        location = aLocation;
        xBar = aMessageXBar;
        component = model.loadout.getComponent(location);

        setupToggles();
        setupItemView(aDistributor);
        setupTitle();
        setupArmors();
        setupHardPoints();
        setupOmniPods();
    }

    private void setupOmniPods() {
        if (component instanceof ConfiguredComponentOmniMech) {
            ConfiguredComponentOmniMech componentOmniMech = (ConfiguredComponentOmniMech) component;

            final Collection<OmniPod> allPods;
            if (location == Location.CenterTorso) {
                allPods = Arrays.asList(componentOmniMech.getOmniPod());
            }
            else {
                allPods = OmniPodDB.lookup((ChassisOmniMech) model.loadout.getChassis(), location);
            }

            omniPodSelection.getItems().addAll(allPods);
            omniPodSelection.getSelectionModel().select(componentOmniMech.getOmniPod());
            omniPodSelection.setCellFactory(aListView -> new OmniPodListCell());

            DoubleBinding padding = Bindings.selectDouble(container.paddingProperty(), "left")
                    .add(Bindings.selectDouble(container.paddingProperty(), "right"));

            omniPodSelection.maxWidthProperty().bind(container.widthProperty().subtract(padding));
        }
        else {
            container.getChildren().remove(omniPodSelection);
            omniPodSelection = null;
        }
    }

    private void setupHardPoints() {
        hardPointContainer.getChildren().clear();
        if (location != Location.LeftLeg && location != Location.RightLeg && location != Location.Head
                && location != Location.CenterTorso) {
            // This spaces out components that don't have any hard points to be as tall
            // as their opposite component that may or may not have a hard point.
            Label noHardPoint = new Label();
            noHardPoint.getStyleClass().add(StyleManager.CSS_CLASS_HARDPOINT);
            noHardPoint.setVisible(false);
            hardPointContainer.getChildren().add(noHardPoint);
        }

        HardPointFormatter hardPointFormatter = new HardPointFormatter();
        for (HardPointType hardPointType : HardPointType.values()) {
            int num = component.getHardPointCount(hardPointType);
            if (num > 0) {
                hardPointContainer.getChildren().add(hardPointFormatter.format(num, hardPointType));
            }
        }
    }

    private void setupArmors() {
        ComponentModel componentModel = model.components.get(location);
        if (location.isTwoSided()) {
            IntegerSpinnerValueFactory frontFactory = new IntegerSpinnerValueFactory(0, 100, 10);
            frontFactory.valueProperty().bindBidirectional(componentModel.armor.asObject());
            frontFactory.maxProperty().bind(componentModel.armorMax);
            armorSpinner.setValueFactory(frontFactory);
            armorLabel.setText("Front:");
            armorMax.textProperty().bind(Bindings.format("/%.0f", componentModel.armorMax));

            IntegerSpinnerValueFactory factory = new IntegerSpinnerValueFactory(0, 100, 10);
            factory.valueProperty().bindBidirectional(componentModel.armorBack.asObject());
            factory.maxProperty().bind(componentModel.armorMaxBack);
            armorSpinnerBack.setValueFactory(factory);
            armorLabelBack.setText("Back:");
            armorMaxBack.textProperty().bind(Bindings.format("/%.0f", componentModel.armorMaxBack));
        }
        else {
            IntegerSpinnerValueFactory factory = new IntegerSpinnerValueFactory(0, 100, 10);
            factory.valueProperty().bindBidirectional(componentModel.armor.asObject());
            factory.maxProperty().bind(componentModel.armorMax);
            armorSpinner.setValueFactory(factory);
            armorLabel.setText("Armor:");
            armorMax.textProperty().bind(Bindings.format("/%.0f", componentModel.armorMax));

            container.getChildren().remove(armorBoxBack);
        }
    }

    private void setupToggles() {
        if (Location.LeftArm == location) {
            setupTogglable(toggleLAA, model.hasLeftLAA);
            setupTogglable(toggleHA, model.hasLeftHA);
        }
        else if (Location.RightArm == location) {
            setupTogglable(toggleLAA, model.hasRightLAA);
            setupTogglable(toggleHA, model.hasRightHA);
        }
        else {
            container.getChildren().remove(toggleLAA);
            container.getChildren().remove(toggleHA);
        }
    }

    private void setupTitle() {
        rootPane.setText(location.longName() + " (" + (int) component.getInternalComponent().getHitPoints() + " hp)");
    }

    private void setupItemView(DynamicSlotDistributor aDistributor) {
        itemView.setVisibleRows(component.getInternalComponent().getSlots());
        itemView.setItems(new ComponentItemsList(xBar, model.loadout, location, aDistributor));
        itemView.setCellFactory((aList) -> {
            return new ComponentItemsCell((ItemView<Item>) aList, component, model.loadout, stack, xBar);
        });

        itemView.setPrefWidth(ITEM_WIDTH);
    }

    private void setupTogglable(ToggleButton aButton, BooleanProperty aToggleProperty) {
        if (aToggleProperty == null) {
            container.getChildren().remove(aButton);
            return;
        }
        aButton.selectedProperty().bindBidirectional(aToggleProperty);
    }

    @FXML
    void onEquipmentClicked(MouseEvent aEvent) throws EquipResult, Exception {
        if (aEvent.getButton() == MouseButton.PRIMARY && aEvent.getClickCount() == 2) {
            if (aEvent.getSource() == itemView) {
                Item item = itemView.getSelectionModel().getSelectedItem();
                if (item != null && component.canRemoveItem(item)) {
                    stack.pushAndApply(new CmdRemoveItem(xBar, model.loadout, component, item));
                }
            }
        }
    }

    @FXML
    void onDragStart(MouseEvent aMouseEvent) throws EquipResult, Exception {
        Item item = itemView.getSelectionModel().getSelectedItem();
        if (component.canRemoveItem(item)) {
            Dragboard db = itemView.startDragAndDrop(TransferMode.MOVE);
            LiSongMechLab.addItemDrag(db, item);
            stack.pushAndApply(new CmdRemoveItem(xBar, model.loadout, component, item));
        }
        aMouseEvent.consume();
    }

    @FXML
    void onDragOver(DragEvent aDragEvent) {
        Dragboard db = aDragEvent.getDragboard();
        if (db.hasString()) {
            try {
                Item item = ItemDB.lookup(Integer.parseInt(db.getString()));
                if (EquipResult.SUCCESS == model.loadout.canEquipDirectly(item)
                        && EquipResult.SUCCESS == component.canEquip(item)) {
                    aDragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            catch (Throwable t) {
                // User dragging junk, ignore it.
                // Sue: Why you always bring me junk?!
            }
        }
        aDragEvent.consume();
    }

    @FXML
    void onDragDropped(DragEvent aDragEvent) {
        Dragboard db = aDragEvent.getDragboard();
        boolean success = false;
        if (db.hasString()) {
            try {
                Item item = ItemDB.lookup(Integer.parseInt(db.getString()));
                stack.pushAndApply(new CmdAddItem(xBar, model.loadout, component, item));
                success = true;
            }
            catch (Exception e) {
                LiSongMechLab.showError(e);
            }
        }
        aDragEvent.setDropCompleted(success);
        aDragEvent.consume();
    }
}
