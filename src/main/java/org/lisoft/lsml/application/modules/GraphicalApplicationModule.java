/*
 * Li Song Mechlab - A 'mech building tool for PGI's MechWarrior: Online.
 * Copyright (C) 2013-2023  Li Song
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
package org.lisoft.lsml.application.modules;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javax.inject.Named;
import org.lisoft.lsml.application.*;
import org.lisoft.lsml.application.UpdateChecker.UpdateCallback;
import org.lisoft.lsml.messages.MessageXBar;
import org.lisoft.lsml.model.export.Base64LoadoutCoder;
import org.lisoft.lsml.model.export.LsmlProtocolIPC;
import org.lisoft.lsml.util.CommandStack;
import org.lisoft.lsml.view_fx.DialogLinkPresenter;
import org.lisoft.lsml.view_fx.Settings;
import org.lisoft.lsml.view_fx.controls.LsmlAlert;
import org.lisoft.lsml.view_fx.style.FilteredModifierFormatter;
import org.lisoft.mwo_data.modifiers.AffectsWeaponPredicate;

/**
 * Provides the requirements for the application and main window.
 *
 * @author Li Song
 */
@Module
public abstract class GraphicalApplicationModule {

  @ApplicationSingleton
  @Provides
  static Optional<LsmlProtocolIPC> provideIPC(
      Settings aSettings,
      @Named("global") MessageXBar aXBar,
      Base64LoadoutCoder aCoder,
      ErrorReporter aErrorReporter) {
    final Property<Integer> portSetting = aSettings.getInteger(Settings.CORE_IPC_PORT);
    if (portSetting.getValue() < LsmlProtocolIPC.MIN_PORT) {
      final LsmlAlert notice = new LsmlAlert(null, AlertType.INFORMATION);
      notice.setTitle("Invalid port defined in settings");
      notice.setHeaderText("Port number will be reset to: " + LsmlProtocolIPC.DEFAULT_PORT);
      notice.setContentText(
          "The port specified in the settings is: "
              + portSetting.getValue()
              + " which is less than 1024. All ports lower than 1024 are reserved for administrator/root use.");
      portSetting.setValue(LsmlProtocolIPC.DEFAULT_PORT);
      notice.showAndWait();
    }

    final SecureRandom rng = new SecureRandom();

    int quietRetries = 2; // Quietly retry twice before prompting the user.
    while (true) {
      try {
        // FIXME: Solve this mess somehow
        final LsmlProtocolIPC ipc =
            new LsmlProtocolIPC(portSetting.getValue(), aXBar, aCoder, aErrorReporter);
        return Optional.of(ipc);
      } catch (final IOException e) {
        if (quietRetries-- > 0) {
          portSetting.setValue(LsmlProtocolIPC.randomPort(rng));
        } else {
          final LsmlAlert alert = new LsmlAlert(null, AlertType.ERROR);
          alert.setTitle("Unable to open local socket!");
          alert.setHeaderText(
              "LSML was unable to open a local socket on port: " + portSetting.getValue());
          alert.setContentText(
              "LSML uses a local socket connection to implement IPC necessary for opening of LSML links. "
                  + "You can try again with a new (random) port or disable LSML links for this session.");

          final ButtonType tryAgain = new ButtonType("Try again");
          final ButtonType disableLinks = new ButtonType("Disable links");

          alert.getButtonTypes().setAll(disableLinks, tryAgain);
          final ButtonType pressedButton = alert.showAndWait().orElse(disableLinks);

          if (pressedButton == tryAgain) {
            portSetting.setValue(LsmlProtocolIPC.randomPort(rng));
          } else {
            return Optional.empty();
          }
        }
      }
    }
  }

  @Provides
  @Named("global")
  @ApplicationSingleton
  static MessageXBar provideMessageXBar() {
    return new MessageXBar();
  }

  @ApplicationSingleton
  @Provides
  static CommandStack provideCommandStack(@Named("undoDepth") int undo) {
    return new CommandStack(undo);
  }

  @Binds
  abstract LinkPresenter provideLinkPresenter(DialogLinkPresenter aDialogLinkPresenter);

  @ApplicationSingleton
  @Named("mainWindowFilterFormatter")
  @Provides
  static FilteredModifierFormatter provideMainWindowModifierFilterFormatter(
      AffectsWeaponPredicate aPredicate) {
    return new FilteredModifierFormatter(aPredicate);
  }

  @ApplicationSingleton
  @Provides
  static OSIntegration provideOSIntegration(DefaultOSIntegration aOsIntegration) {
    return aOsIntegration;
  }

  @Provides
  @Named("undoDepth")
  static int provideUndoDepth() {
    return 128;
  }

  @ApplicationSingleton
  @Provides
  static UpdateCallback provideUpdateCallback(ErrorReporter aErrorReporter) {
    return (aReleaseData) -> {
      if (aReleaseData != null) {
        Platform.runLater(
            () -> {
              final LsmlAlert alert = new LsmlAlert(null, AlertType.INFORMATION);
              alert.setTitle("Update available!");
              alert.setHeaderText("A new version of LSML is available: " + aReleaseData.name);
              alert.setContentText("For more information about whats new, see the download page.");
              final ButtonType download = new ButtonType("Download");
              final ButtonType later = new ButtonType("Remind me again in 3 days");
              alert.getButtonTypes().setAll(later, download);
              alert
                  .showAndWait()
                  .ifPresent(
                      aButton -> {
                        if (aButton == download) {
                          try {
                            Desktop.getDesktop().browse(new URI(aReleaseData.html_url));
                          } catch (final Exception e) {
                            aErrorReporter.error(
                                "Cannot open link",
                                "Unable to open the link in the system default browser, please open the link manually.",
                                e);
                          }
                        }
                      });
            });
      }
    };
  }

  @ApplicationSingleton
  @Provides
  static Optional<UpdateChecker> provideUpdateChecker(
      Settings aSettings, UpdateCallback aUpdateCallback, @Named("version") String aVersion) {
    if (!aSettings.getBoolean(Settings.CORE_CHECK_FOR_UPDATES).getValue()) {
      return Optional.empty();
    }

    final Property<Long> lastUpdate = aSettings.getLong(Settings.CORE_LAST_UPDATE_CHECK);
    final Instant now = Instant.now();
    if (ChronoUnit.DAYS.between(Instant.ofEpochMilli(lastUpdate.getValue()), now) < 3) {
      return Optional.empty();
    }
    lastUpdate.setValue(now.toEpochMilli());

    final boolean acceptBeta = aSettings.getBoolean(Settings.CORE_ACCEPT_BETA_UPDATES).getValue();

    try {
      return Optional.of(
          new UpdateChecker(
              new URL(UpdateChecker.GITHUB_RELEASES_ADDRESS),
              aVersion,
              aUpdateCallback,
              acceptBeta));
    } catch (final MalformedURLException e) {
      // MalformedURL is a programmer error, promote to unchecked, let
      // default exception handler report it.
      throw new RuntimeException(e);
    }
  }
}
