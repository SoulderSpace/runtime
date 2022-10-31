package pro.gravit.launcher.client.gui.scenes.options;

import animatefx.animation.SlideInUp;
import com.google.gson.reflect.TypeToken;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerButtonComponent;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static pro.gravit.launcher.Launcher.profile;

public class OptionsScene extends AbstractScene {
    private Pane componentList;
    private OptionalView optionalView;

    private ImageView avatar;

    private Image originalAvatarImage;
    public OptionsScene(JavaFXApplication application) {
        super("scenes/options/options.fxml", application);
    }

    @Override
    protected void doInit() {
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#header", "#exit").ifPresent((b) -> b.setOnAction((e) -> currentStage.close()));
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#header", "#minimize").ifPresent((b) -> b.setOnAction((e) -> currentStage.hide()));

        LookupHelper.lookupIfPossible(layout, "#web").ifPresent((e) -> e.setOnMouseClicked(event ->
                application.openURL("https://soulder.space/")));
        LookupHelper.lookupIfPossible(layout, "#discord").ifPresent((e) -> e.setOnMouseClicked(event ->
                application.openURL("https://discord.gg/P64RFКЧЧYNzf3")));
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
        LookupHelper.<Button>lookup(layout, "#settings").setOnAction((e) -> {
            try {
                switchScene(application.gui.settingsScene);
                application.gui.settingsScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        componentList = (Pane) LookupHelper.<ScrollPane>lookup(layout, "#optionslist").getContent();
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname").ifPresent((e) -> e.setText(application.stateService.getUsername()));
    }

    @Override
    public void reset() {
        avatar.setImage(originalAvatarImage);
        new SlideInUp(LookupHelper.lookup(layout, "#options-detail")).setSpeed(1.5).play();
        profile = application.stateService.getProfile();
        LookupHelper.lookup(layout, "#moreInfo").setOnMouseClicked((e) -> {
            application.openURL(profile.getProperty("moreInfoURL"));
        });
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> e.setText(profile.getTitle()));
//        Pane serverButtonContainer = LookupHelper.lookup(layout, "#serverButton");
//        serverButtonContainer.getChildren().clear();

//        ServerButtonComponent serverButton = ServerMenuScene.getServerButton(application, profile);
//        serverButton.addTo(serverButtonContainer);
//        serverButton.enableSaveButton(null, (e) -> {
//            try {
//                application.stateService.setOptionalView(profile, optionalView);
//                switchScene(application.gui.serverInfoScene);
//            } catch (Exception exception) {
//                errorHandle(exception);
//            }
//        });
//        serverButton.enableResetButton(null, (e) -> {
//            componentList.getChildren().clear();
//            application.stateService.setOptionalView(profile, new OptionalView(profile));
//            addProfileOptionals(application.stateService.getOptionalView());
//        });
        componentList.getChildren().clear();
        LookupHelper.<Button>lookupIfPossible(layout, "#back").ifPresent(x -> x.setOnAction((e) -> {
            try {
                switchScene(application.gui.serverInfoScene);
                application.gui.serverInfoScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        ServerMenuScene.putAvatarToImageView(application, application.stateService.getUsername(), avatar);
    }

    @Override
    public String getName() {
        return "options";
    }

    private final Map<OptionalFile, Consumer<Boolean>> watchers = new HashMap<>();

    private void callWatcher(OptionalFile file, Boolean value) {
        for (Map.Entry<OptionalFile, Consumer<Boolean>> v : watchers.entrySet()) {
            if (v.getKey() == file) {
                v.getValue().accept(value);
                break;
            }
        }
    }

    public void addProfileOptionals(OptionalView view) {
        this.optionalView = new OptionalView(view);
        watchers.clear();
        for (OptionalFile optionalFile : optionalView.all) {
            if (!optionalFile.visible)
                continue;

            Consumer<Boolean> setCheckBox = add(optionalFile.name, optionalFile.info, optionalView.enabled.contains(optionalFile),
                    optionalFile.subTreeLevel, (isSelected) -> {
                        if (isSelected)
                            optionalView.enable(optionalFile, true, this::callWatcher);
                        else
                            optionalView.disable(optionalFile, this::callWatcher);
                    });
            watchers.put(optionalFile, setCheckBox);
        }
    }

    public Consumer<Boolean> add(String name, String description, boolean value, int padding, Consumer<Boolean> onChanged) {
        VBox vBox = new VBox();
        CheckBox checkBox = new CheckBox();
        Label label = new Label();
        vBox.getChildren().add(checkBox);
        vBox.getChildren().add(label);
        VBox.setMargin(vBox, new Insets(0, 0, 0, 30 * --padding));
        vBox.getStyleClass().add("optional-container");
        checkBox.setSelected(value);
        checkBox.setText(name);
        checkBox.setOnAction((e) -> onChanged.accept(checkBox.isSelected()));
        checkBox.getStyleClass().add("optional-checkbox");
        label.setText(description);
        label.setWrapText(true);
        label.getStyleClass().add("optional-label");
        componentList.getChildren().add(vBox);
        return checkBox::setSelected;
    }

    public void saveAll() throws IOException {
        List<ClientProfile> profiles = application.stateService.getProfiles();
        Map<ClientProfile, OptionalView> optionalViewMap = application.stateService.getOptionalViewMap();
        if (profiles == null)
            return;
        Path optionsFile = DirBridge.dir.resolve("options.json");
        List<OptionalListEntry> list = new ArrayList<>(5);
        for (ClientProfile clientProfile : profiles) {
            OptionalListEntry entry = new OptionalListEntry();
            entry.name = clientProfile.getTitle();
            entry.profileUUID = clientProfile.getUUID();
            OptionalView view = optionalViewMap.get(clientProfile);
            view.all.forEach((optionalFile -> {
                if (optionalFile.visible) {
                    boolean isEnabled = view.enabled.contains(optionalFile);
                    OptionalView.OptionalFileInstallInfo installInfo = view.installInfo.get(optionalFile);
                    entry.enabled.add(new OptionalListEntryPair(optionalFile, isEnabled, installInfo));
                }
            }));
            list.add(entry);
        }
        try (Writer writer = IOHelper.newWriter(optionsFile)) {
            Launcher.gsonManager.gson.toJson(list, writer);
        }
    }

    public void loadAll() throws IOException {
        List<ClientProfile> profiles = application.stateService.getProfiles();
        Map<ClientProfile, OptionalView> optionalViewMap = application.stateService.getOptionalViewMap();
        if (profiles == null)
            return;
        Path optionsFile = DirBridge.dir.resolve("options.json");
        if (!Files.exists(optionsFile))
            return;

        Type collectionType = new TypeToken<List<OptionalListEntry>>() {
        }.getType();

        try (Reader reader = IOHelper.newReader(optionsFile)) {
            List<OptionalListEntry> list = Launcher.gsonManager.gson.fromJson(reader, collectionType);
            for (OptionalListEntry entry : list) {
                ClientProfile selectedProfile = null;
                for (ClientProfile clientProfile : profiles) {
                    if (entry.profileUUID != null ? entry.profileUUID.equals(clientProfile.getUUID()) : clientProfile.getTitle().equals(entry.name))
                        selectedProfile = clientProfile;
                }
                if (selectedProfile == null) {
                    LogHelper.warning("Optional: profile %s(%s) not found", entry.name, entry.profileUUID);
                    continue;
                }
                OptionalView view = optionalViewMap.get(selectedProfile);
                for (OptionalListEntryPair entryPair : entry.enabled) {
                    try {
                        OptionalFile file = selectedProfile.getOptionalFile(entryPair.name);
                        if (file.visible) {
                            if (entryPair.mark)
                                view.enable(file, entryPair.installInfo != null && entryPair.installInfo.isManual, null);
                            else
                                view.disable(file, null);
                        }
                    } catch (Exception exc) {
                        LogHelper.warning("Optional: in profile %s markOptional mod %s failed", selectedProfile.getTitle(), entryPair.name);
                    }
                }
            }
        }
    }

    public static class OptionalListEntryPair {
        public String name;
        public boolean mark;
        public OptionalView.OptionalFileInstallInfo installInfo;

        public OptionalListEntryPair(OptionalFile optionalFile, boolean enabled, OptionalView.OptionalFileInstallInfo installInfo) {
            name = optionalFile.name;
            mark = enabled;
            this.installInfo = installInfo;
        }
    }

    public static class OptionalListEntry {
        public List<OptionalListEntryPair> enabled = new LinkedList<>();
        public String name;
        public UUID profileUUID;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionalListEntry that = (OptionalListEntry) o;
            return Objects.equals(profileUUID, that.profileUUID) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, profileUUID);
        }
    }
}
