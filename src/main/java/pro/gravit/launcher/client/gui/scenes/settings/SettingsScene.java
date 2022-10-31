package pro.gravit.launcher.client.gui.scenes.settings;

import animatefx.animation.SlideInUp;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import oshi.SystemInfo;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.function.Consumer;

public class SettingsScene extends AbstractScene {
    private Pane componentList;
    private Label ramLabel;
    ClientProfile profile;
    private Slider ramSlider;
    private RuntimeSettings.ProfileSettingsView profileSettings;
    private JavaSelectorComponent javaSelector;
    private ImageView avatar;
    private Image originalAvatarImage;

    public SettingsScene(JavaFXApplication application) {
        super("scenes/settings/settings.fxml", application);
    }

    @Override
    protected void doInit() {
        LookupHelper.lookupIfPossible(layout, "#web").ifPresent((e) -> e.setOnMouseClicked(event ->
                application.openURL("https://soulder.space/")));
        LookupHelper.lookupIfPossible(layout, "#discord").ifPresent((e) -> e.setOnMouseClicked(event ->
                application.openURL("https://discord.gg/P64RFYNzf3")));
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname").ifPresent((e) -> e.setText(application.stateService.getUsername()));
        avatar = LookupHelper.lookup(layout, "#avatar");
        originalAvatarImage = avatar.getImage();
        LookupHelper.<ImageView>lookupIfPossible(layout, "#avatar").ifPresent(
                (h) -> {
                    try {
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(h.getFitWidth(), h.getFitHeight());
                        clip.setArcWidth(h.getFitWidth());
                        clip.setArcHeight(h.getFitHeight());
                        h.setClip(clip);
                        h.setImage(originalAvatarImage);
                    } catch (Throwable e) {
                        LogHelper.warning("Skin head error");
                    }
                }
        );
        componentList = (Pane) LookupHelper.<ScrollPane>lookup(layout, "#settingslist").getContent();

        ramSlider = LookupHelper.lookup(layout, "#ramSlider");
        ramLabel = LookupHelper.lookup(layout, "#ramLabel");
        try {
            SystemInfo systemInfo = new SystemInfo();
            ramSlider.setMax(systemInfo.getHardware().getMemory().getTotal() >> 20);
        } catch (Throwable e) {
            ramSlider.setMax(2048);
        }

        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramSlider.setMinorTickCount(1);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setBlockIncrement(1024);
        ramSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format("%.0fG", object / 1024);
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        reset();

    }

    @Override
    public void reset() {
        avatar.setImage(originalAvatarImage);
        new SlideInUp(LookupHelper.lookup(layout, "#rams")).setSpeed(1.5).play();
        new SlideInUp(LookupHelper.lookup(layout, "#settings-detail")).setSpeed(1.5).play();
        new SlideInUp(LookupHelper.lookup(layout, "#panel2")).setSpeed(1.5).play();
        profile = application.stateService.getProfile();
        ClientProfile profiles = application.stateService.getProfile();
        LookupHelper.lookup(layout, "#moreInfo").setOnMouseClicked((e) -> {
            application.openURL(profile.getProperty("moreInfoURL"));
        });
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> e.setText(profile.getTitle()));
        profileSettings = new RuntimeSettings.ProfileSettingsView(application.getProfileSettings());
        javaSelector = new JavaSelectorComponent(application.javaService, layout, profileSettings, application.stateService.getProfile());
        ramSlider.setValue(profileSettings.ram);
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            profileSettings.ram = newValue.intValue();
            updateRamLabel();
        });
        LookupHelper.<ButtonBase>lookupIfPossible(header, "#deauth").ifPresent(b -> b.setOnAction((e) ->
                application.messageManager.showApplyDialog(application.getTranslation("runtime.scenes.settings.exitDialog.header"),
                        application.getTranslation("runtime.scenes.settings.exitDialog.description"), this::userExit
                        , () -> {
                        }, true)));
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#back").ifPresent(a -> a.setOnAction((e) -> {
            try {
                profileSettings.apply();
                application.triggerManager.process(profiles, application.stateService.getOptionalView());
                profileSettings = null;
                switchScene(application.gui.serverInfoScene);
                application.gui.serverInfoScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        LookupHelper.<Button>lookup(layout, "#clientSettings").setOnAction((e) -> {
            try {
                profileSettings.apply();
                application.triggerManager.process(profiles, application.stateService.getOptionalView());
                profileSettings = null;
                if (application.stateService.getProfile() == null)
                    return;
                switchScene(application.gui.optionsScene);
                application.gui.optionsScene.reset();
                application.gui.optionsScene.addProfileOptionals(application.stateService.getOptionalView());
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });
        updateRamLabel();
//        Pane serverButtonContainer = LookupHelper.lookup(layout, "#serverButton");
//        serverButtonContainer.getChildren().clear();
//        ClientProfile profile = application.stateService.getProfile();
//        ServerButtonComponent serverButton = ServerMenuScene.getServerButton(application, profile);
//        serverButton.addTo(serverButtonContainer);
//        serverButton.enableSaveButton(null, (e) -> {
//            try {
//                profileSettings.apply();
//                application.triggerManager.process(profile, application.stateService.getOptionalView());
//                switchScene(application.gui.serverInfoScene);
//            } catch (Exception exception) {
//                errorHandle(exception);
//            }
//        });
//        serverButton.enableResetButton(null, (e) -> {
//            reset();
//        });
        componentList.getChildren().clear();
        add("Debug", profileSettings.debug, (value) -> profileSettings.debug = value);
        add("AutoEnter", profileSettings.autoEnter, (value) -> profileSettings.autoEnter = value);
        add("Fullscreen", profileSettings.fullScreen, (value) -> profileSettings.fullScreen = value);
        ServerMenuScene.putAvatarToImageView(application, application.stateService.getUsername(), avatar);
    }

    @Override
    public String getName() {
        return "settings";
    }

    public void add(String languageName, boolean value, Consumer<Boolean> onChanged) {
        String nameKey = String.format("runtime.scenes.settings.properties.%s.name", languageName.toLowerCase());
        String descriptionKey = String.format("runtime.scenes.settings.properties.%s.description", languageName.toLowerCase());
        add(application.getTranslation(nameKey, languageName), application.getTranslation(descriptionKey, languageName), value, onChanged);
    }

    public void add(String name, String description, boolean value, Consumer<Boolean> onChanged) {
        VBox vBox = new VBox();
        CheckBox checkBox = new CheckBox();
        Pane pane = new Pane();
        pane.setMinWidth(246);
        pane.setMaxWidth(246);
        pane.setMinHeight(62);
        pane.setMaxHeight(62);
        Label label = new Label();
        Label maintext = new Label();
        vBox.getChildren().add(checkBox);
        pane.getChildren().add(label);
        pane.getChildren().add(maintext);
        checkBox.setGraphic(pane);
        VBox.setMargin(vBox, new Insets(0, 0, 10, 0));
        vBox.getStyleClass().add("settings-container");
        checkBox.setSelected(value);
        checkBox.setMinWidth(246);
        checkBox.setMaxWidth(246);
        checkBox.setMinHeight(62);
        checkBox.setMaxHeight(62);
        checkBox.setCursor(Cursor.HAND);
        checkBox.setOnAction((e) -> {
            onChanged.accept(checkBox.isSelected());
        });
        checkBox.getStyleClass().add("settings-checkbox");
        maintext.setText(name);
        maintext.setWrapText(true);
        maintext.getStyleClass().add("maintext");
        label.setPrefWidth(220);
        label.setText(description);
        label.setWrapText(true);
        label.getStyleClass().add("descriptiontext");
        componentList.getChildren().add(vBox);
    }

    public void updateRamLabel() {
        ramLabel.setText(profileSettings.ram == 0 ? application.getTranslation("runtime.scenes.settings.ramAuto") : MessageFormat.format(application.getTranslation("runtime.scenes.settings.ram"), profileSettings.ram));
    }
}
