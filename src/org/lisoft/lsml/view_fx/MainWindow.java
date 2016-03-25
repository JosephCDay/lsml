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
package org.lisoft.lsml.view_fx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.model.export.Base64LoadoutCoder;
import org.lisoft.lsml.model.export.BatchImportExporter;
import org.lisoft.lsml.model.export.LsmlLinkProtocol;
import org.lisoft.lsml.model.export.SmurfyImportExport;
import org.lisoft.lsml.model.garage.Garage;
import org.lisoft.lsml.model.garage.GaragePath;
import org.lisoft.lsml.model.garage.GarageSerialiser;
import org.lisoft.lsml.model.item.Faction;
import org.lisoft.lsml.model.loadout.Loadout;
import org.lisoft.lsml.util.CommandStack;
import org.lisoft.lsml.view_fx.util.FxmlHelpers;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This class is the controller for the main window.
 * 
 * @author Li Song
 */
public class MainWindow extends StackPane {
    private final static ExtensionFilter  LSML_EXT         = new FileChooser.ExtensionFilter("LSML Garage 1.0",
            "*.xml");
    private final static ExtensionFilter  LSML_EXT2        = new FileChooser.ExtensionFilter("LSML Garage 2.0",
            "*.lsxml");
    @FXML
    private StackPane                     block_content;
    private final CommandStack            cmdStack         = new CommandStack(100);
    private ObjectProperty<Faction>       factionFilter    = new SimpleObjectProperty<>();
    @FXML
    private CheckBox                      filterClan;
    @FXML
    private CheckBox                      filterIS;
    private Garage                        garage;
    private File                          garageFile;
    private final GarageSerialiser        garageSerialiser = new GarageSerialiser();
    @FXML
    private ListView<Loadout>             loadout_pills;
    @FXML
    private TreeView<GaragePath<Loadout>> loadout_tree;
    @FXML
    private Toggle                        nav_chassis;
    @FXML
    private Toggle                        nav_dropships;
    @FXML
    private ToggleGroup                   nav_group;
    @FXML
    private ToggleButton                  nav_imexport;
    @FXML
    private Toggle                        nav_loadouts;
    @FXML
    private ToggleButton                  nav_settings;
    @FXML
    private Toggle                        nav_weapons;
    @FXML
    private BorderPane                    overlayPane;
    private BorderPane                    page_chassis;
    @FXML
    private Pane                          page_dropships;
    private BorderPane                    page_imexport;
    @FXML
    private BorderPane                    page_loadouts;
    @FXML
    private ScrollPane                    page_settings;
    @FXML
    private ScrollPane                    page_weapons;
    private final Settings                settings         = Settings.getSettings();
    private final MessageXBar             xBar             = new MessageXBar();
    @FXML
    private BorderPane                    base;

    public MainWindow() {
        // This function will be called outside of the JavaFX thread, only do stuff that doesn't
        // require the JavaFX thread. Other work to be done in #prepareShow.
        FxmlHelpers.loadFxmlControl(this);
        setupFactionFilter();

        getChildren().remove(overlayPane);
    }

    /**
     * Selects a new garage file and creates an empty garage. Sets the {@link Settings#CORE_GARAGE_FILE} property to the
     * new file. If successful, the {@link Settings#CORE_GARAGE_FILE} property is updated.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    @FXML
    public void newGarage() throws FileNotFoundException, IOException {
        garage = new Garage();
        writeGarageDialog("Create new garage...");
    }

    @FXML
    public void openNewMechOverlay() {
        overlayPane.setCenter(new NewMechPane(() -> {
            getChildren().remove(overlayPane);
            overlayPane.setCenter(null);
            base.setDisable(false);
        }));
        getChildren().add(overlayPane);
        base.setDisable(true);
    }

    /**
     * Opens a garage after confirming save with the user. If a new garage is loaded the
     * {@link Settings#CORE_GARAGE_FILE} property will be updated.
     * 
     * @throws IOException
     */
    @FXML
    public void openGarage() throws IOException {
        if (null != garage) {
            boolean saved = false;
            boolean cancel = false;
            while (!saved && !cancel) {
                Alert saveConfirm = new Alert(AlertType.CONFIRMATION, "Save current garage?");
                Optional<ButtonType> result = saveConfirm.showAndWait();
                if (result.isPresent()) {
                    if (ButtonType.OK == result.get()) {
                        if (null != garageFile) {
                            saved = saveGarageAs();
                        }
                        else {
                            saveGarage();
                            saved = true;
                        }
                    }
                    else {
                        cancel = true;
                        saved = false;
                    }
                }
            }
        }

        FileChooser fileChooser = garageFileChooser("Open Garage");
        fileChooser.getExtensionFilters().add(LSML_EXT);

        Scene scene = getScene();
        File file = fileChooser.showOpenDialog(scene == null ? null : scene.getWindow());

        if (null != file) {
            try (FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);) {
                garage = garageSerialiser.load(bis, DefaultLoadoutErrorReporter.instance);
                garageFile = file;
                settings.getProperty(Settings.CORE_GARAGE_FILE, String.class).setValue(garageFile.getAbsolutePath());
            }
        }
    }

    /**
     * @param aCoder
     * @throws IOException
     * 
     */
    public void prepareShow(Base64LoadoutCoder aCoder) throws IOException {
        autoLoadLastGarage();
        // FIXME: If a new garage is opened the chassisPage will have a pointer to the wrong one!
        page_chassis = new ChassisPage(factionFilter, xBar, garage);
        // FIXME: These really should be constructed through DI
        BatchImportExporter importer = new BatchImportExporter(aCoder, LsmlLinkProtocol.LSML,
                DefaultLoadoutErrorReporter.instance);
        SmurfyImportExport smurfyImportExport = new SmurfyImportExport(aCoder, DefaultLoadoutErrorReporter.instance);
        page_imexport = new ImportExportPage(xBar, garage, importer, smurfyImportExport, cmdStack);
        setupNavigationBar();
        setupLoadoutPage();
        page_weapons.setContent(new WeaponsPage(factionFilter));
    }

    @FXML
    public void saveGarage() throws IOException {
        if (null != garageFile) {
            writeGarage(garageFile);
        }
    }

    /**
     * Will save the current garage as a new file. If successful, the {@link Settings#CORE_GARAGE_FILE} property is
     * updated.
     * 
     * @return <code>true</code> if the garage was written to a file, <code>false</code> otherwise.
     * @throws IOException
     */
    @FXML
    public boolean saveGarageAs() throws IOException {
        return writeGarageDialog("Save garage as...");
    }

    private void autoLoadLastGarage() throws IOException {
        do {
            String garageFileName = settings.getProperty(Settings.CORE_GARAGE_FILE, String.class).getValue();
            garageFile = new File(garageFileName);
            if (garageFile.exists()) {
                try (FileInputStream fis = new FileInputStream(garageFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);) {
                    garage = garageSerialiser.load(bis, DefaultLoadoutErrorReporter.instance);
                }
            }
            else {
                ButtonType openGarage = new ButtonType("Open Garage...");
                ButtonType newGarage = new ButtonType("New Garage...");
                ButtonType exit = new ButtonType("Exit", ButtonData.CANCEL_CLOSE);

                Alert alert = new Alert(AlertType.NONE);
                alert.setTitle("Select Garage...");
                alert.setHeaderText("Please select or create a new garage to use.");
                alert.setContentText("LSML stores your 'Mechs and Drop Ships in a 'garage'. "
                        + "Your garage is automatically loaded when you open"
                        + " LSML and automatically saved when you close LSML.");
                alert.getButtonTypes().setAll(newGarage, openGarage, exit);
                Optional<ButtonType> selection = alert.showAndWait();
                if (selection.isPresent()) {
                    if (openGarage == selection.get()) {
                        openGarage();
                    }
                    else if (newGarage == selection.get()) {
                        newGarage();
                    }
                    else {
                        System.exit(0);
                    }
                }
                else {
                    System.exit(0);
                }
            }
        } while (garageFile == null || !garageFile.exists());
    }

    private boolean confirmOverwrite() {
        Alert confirmOverwrite = new Alert(AlertType.CONFIRMATION, "Overwrite selected garage?");
        Optional<ButtonType> result = confirmOverwrite.showAndWait();
        return result.isPresent() && ButtonType.OK != result.get();
    }

    private FileChooser garageFileChooser(String aTitle) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(aTitle);
        fileChooser.getExtensionFilters().addAll(LSML_EXT2);

        if (null != garageFile && garageFile.exists()) {
            fileChooser.setInitialDirectory(garageFile);
        }
        else {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        return fileChooser;
    }

    private void setupFactionFilter() {
        InvalidationListener listener = (aObs) -> {
            if (filterClan.isSelected()) {
                if (filterIS.isSelected()) {
                    factionFilter.set(Faction.ANY);
                }
                else {
                    factionFilter.set(Faction.CLAN);
                }
            }
            else {
                if (filterIS.isSelected()) {
                    factionFilter.set(Faction.INNERSPHERE);
                }
                else {
                    factionFilter.set(Faction.ANY);
                }
            }
        };
        filterIS.selectedProperty().addListener(listener);
        filterClan.selectedProperty().addListener(listener);
        listener.invalidated(null);
    }

    private void setupLoadoutPage() {
        FxmlHelpers.prepareGarageTree(loadout_tree, garage.getLoadoutRoot(), xBar, cmdStack, false);
        loadout_tree.getSelectionModel().selectedItemProperty().addListener((aObservable, aOld, aNew) -> {
            if (null == aNew)
                loadout_pills.getItems().clear();
            else
                loadout_pills
                        .setItems(FXCollections.observableArrayList(aNew.getValue().getTopDirectory().getValues()));
        });
        loadout_pills.setCellFactory(aView -> new LoadoutPillCell(garage, xBar, loadout_tree, aView));
        loadout_pills.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void setupNavigationBar() {
        page_settings.setContent(new SettingsPage());

        nav_group.selectedToggleProperty().addListener((aObservable, aOld, aNew) -> {
            if (aNew == nav_loadouts) {
                block_content.getChildren().setAll(page_loadouts);
                page_loadouts.setVisible(true);
            }
            else if (aNew == nav_dropships) {
                block_content.getChildren().setAll(page_dropships);
                page_dropships.setVisible(true);
            }
            else if (aNew == nav_chassis) {
                block_content.getChildren().setAll(page_chassis);
                page_chassis.setVisible(true);
            }
            else if (aNew == nav_weapons) {
                block_content.getChildren().setAll(page_weapons);
                page_weapons.setVisible(true);
            }
            else if (aNew == nav_imexport) {
                block_content.getChildren().setAll(page_imexport);
                page_imexport.setVisible(true);
            }
            else if (aNew == nav_settings) {
                block_content.getChildren().setAll(page_settings);
                page_settings.setVisible(true);
            }
            else {
                throw new IllegalArgumentException("Unknown toggle value! " + aNew);
            }
        });
        nav_group.selectToggle(nav_loadouts);
    }

    private void writeGarage(File file) throws IOException, FileNotFoundException {
        try (FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);) {
            garageSerialiser.save(bos, garage, DefaultLoadoutErrorReporter.instance);
            garageFile = file;
            Property<String> garageProp = settings.getProperty(Settings.CORE_GARAGE_FILE, String.class);
            garageProp.setValue(file.getAbsolutePath());
        }
    }

    private boolean writeGarageDialog(String aTitle) throws IOException, FileNotFoundException {
        FileChooser fileChooser = garageFileChooser(aTitle);
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (null != file && (!file.exists() || confirmOverwrite())) {
            writeGarage(file);
            return true;
        }
        return false;
    }
}
