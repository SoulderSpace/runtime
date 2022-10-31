package pro.gravit.launcher.client.gui.scenes.serverinfo;

import animatefx.animation.SlideInLeft;
import animatefx.animation.SlideInRight;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.debug.DebugScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerButtonComponent;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.launcher.request.auth.SetProfileRequest;
import pro.gravit.utils.helper.*;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;

public class ServerInfoScene extends AbstractScene {
    private ImageView avatar;
    private Image originalAvatarImage;
    private ServerButtonComponent serverButton;

    public ServerInfoScene(JavaFXApplication application) {
        super("scenes/serverinfo/serverinfo.fxml", application);
    }

    @Override
    protected void doInit() throws Exception {
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#header", "#exit").ifPresent((b) -> b.setOnAction((e) -> currentStage.close()));
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#header", "#minimize").ifPresent((b) -> b.setOnAction((e) -> currentStage.hide()));

        LookupHelper.lookupIfPossible(layout, "#web").ifPresent((e) -> e.setOnMouseClicked(event ->
                application.openURL("https://soulder.space/")));
        LookupHelper.lookupIfPossible(layout, "#discord").ifPresent((e) -> e.setOnMouseClicked(event ->
                application.openURL("https://discord.gg/P64RFYNzf3")));
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
        LookupHelper.<Button>lookup(layout, "#back").setOnAction((e) -> {
            try {
                switchScene(application.gui.serverMenuScene);
                application.gui.serverMenuScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });

        LookupHelper.<Button>lookup(layout, "#play").setOnAction((e) -> {
            try {
                launchClient();
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });
        LookupHelper.<Button>lookup(layout, "#clientSettings").setOnAction((e) -> {
            try {
                if (application.stateService.getProfile() == null)
                    return;
                switchScene(application.gui.optionsScene);
                application.gui.optionsScene.reset();
                application.gui.optionsScene.addProfileOptionals(application.stateService.getOptionalView());
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });
        LookupHelper.<Button>lookup(layout, "#settings").setOnAction((e) -> {
            try {
                switchScene(application.gui.settingsScene);
                application.gui.settingsScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        reset();
    }

    @Override
    public void reset() {
        avatar.setImage(originalAvatarImage);
        ClientProfile profile = application.stateService.getProfile();
        LookupHelper.lookup(layout, "#moreInfo").setOnMouseClicked((e) -> {
            application.openURL(profile.getProperty("moreInfoURL"));
        });
        Path clientfolder = DirBridge.dirUpdates.resolve(profile.getDir());
        Path assetfolder = DirBridge.dirUpdates.resolve(profile.getDir());
        Pane panepanepane = (Pane) LookupHelper.<Button>lookup(layout, "#play").getGraphic();
        if (IOHelper.exists(clientfolder) && IOHelper.exists(assetfolder)){
            LookupHelper.<SVGPath>lookup(panepanepane, "#playIcon").setVisible(true);
            LookupHelper.<SVGPath>lookup(panepanepane, "#downloadIcon").setVisible(false);
        } else {
            LookupHelper.<SVGPath>lookup(panepanepane, "#playIcon").setVisible(false);
            LookupHelper.<SVGPath>lookup(panepanepane, "#downloadIcon").setVisible(true);
        }
        application.pingService.getPingReport(profile.getDefaultServerProfile().name).thenAccept((report) -> {
            if(report == null) {
                LookupHelper.<Label>lookup(layout, "#online").setText("Ошибка при получении");
            } else {
                LookupHelper.<Label>lookup(layout, "#online").setText(report.playersOnline + " из " + report.maxPlayers);
            }
        });
        LookupHelper.<Label>lookup(layout, "#version").setText(String.valueOf(profile.getAssetIndex()));
        if (profile.getProperty("modLoader") != null){
            if (Objects.equals(profile.getProperty("modLoader"), "forge")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderForge").ifPresent((e) -> e.setVisible(true));
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderFabric").ifPresent((e) -> e.setVisible(false));
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderVanilla").ifPresent((e) -> e.setVisible(false));
            } else if (Objects.equals(profile.getProperty("modLoader"), "fabric")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderFabric").ifPresent((e) -> e.setVisible(true));
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderForge").ifPresent((e) -> e.setVisible(false));
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderVanilla").ifPresent((e) -> e.setVisible(false));
            } else if (Objects.equals(profile.getProperty("modLoader"), "vanilla")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderVanilla").ifPresent((e) -> e.setVisible(true));
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderFabric").ifPresent((e) -> e.setVisible(false));
                LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderForge").ifPresent((e) -> e.setVisible(false));
            }
        } else {
            LookupHelper.<Pane>lookupIfPossible(layout, "#modLoaderVanilla").ifPresent((e) -> e.setVisible(true));
        }
        String status = profile.getProperty("status");
        if (status != null){
            if (status.equals("indev")){
                LookupHelper.<Pane>lookupIfPossible(layout, "#indev").ifPresent((e) -> e.setVisible(true));
            } else if (status.equals("obt")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#obt").ifPresent((e) -> e.setVisible(true));
            } else if (status.equals("release")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#release").ifPresent((e) -> e.setVisible(true));
            } else if (status.equals("zbt")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#zbt").ifPresent((e) -> e.setVisible(true));
            } else if (status.equals("closed")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#closed").ifPresent((e) -> e.setVisible(true));
            } else if (status.equals("bt")) {
                LookupHelper.<Pane>lookupIfPossible(layout, "#bt").ifPresent((e) -> e.setVisible(true));
            }
        }
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> e.setText(profile.getTitle()));
        LookupHelper.<Label>lookupIfPossible(layout, "#serverDescription").ifPresent((e) -> e.setText(profile.getInfo()));
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname").ifPresent((e) -> e.setText(application.stateService.getUsername()));
        LookupHelper.<Pane>lookupIfPossible(layout, "#animationPane").ifPresent((e) -> {
            new SlideInLeft(e).setSpeed(1.5).play();
        });
        if (profile.getProperty("ImageServerURL") != null){
            LookupHelper.lookupIfPossible(layout, "#serverImage").ifPresent((e) -> {
                e.setStyle("-fx-background-image: url(" + profile.getProperty("ImageServerURL") + ");");
            });
        }
        LookupHelper.lookupIfPossible(layout, "#panel").ifPresent((e) -> new SlideInRight(e).setSpeed(1.5).play());
//        Pane serverButtonContainer = LookupHelper.lookup(layout, "#serverButton");
//        serverButtonContainer.getChildren().clear();
//        serverButton = ServerMenuScene.getServerButton(application, profile);
//        serverButton.addTo(serverButtonContainer);
//        serverButton.enableSaveButton(application.getTranslation("runtime.scenes.serverinfo.serverButton.game"), (e) -> launchClient());
        ServerMenuScene.putAvatarToImageView(application, application.stateService.getUsername(), avatar);
    }

    @Override
    public String getName() {
        return null;
    }

    private void downloadClients(ClientProfile profile, JavaHelper.JavaVersion javaVersion, HashedDir jvmHDir) {
        Path target = DirBridge.dirUpdates.resolve(profile.getAssetDir());
        LogHelper.info("Start update to %s", target.toString());
        Consumer<HashedDir> next = (assetHDir) -> {
            Path targetClient = DirBridge.dirUpdates.resolve(profile.getDir());
            LogHelper.info("Start update to %s", targetClient.toString());
            application.gui.updateScene.sendUpdateRequest(profile.getDir(), targetClient, profile.getClientUpdateMatcher(), true, application.stateService.getOptionalView(), true, (clientHDir) -> {
                LogHelper.info("Success update");
                try {
                    doLaunchClient(target, assetHDir, targetClient, clientHDir, profile, application.stateService.getOptionalView(), javaVersion, jvmHDir);
                } catch (Throwable e) {
                    LogHelper.error(e);
                    ContextHelper.runInFxThreadStatic(() -> application.gui.updateScene.addLog(String.format("launchClient error %s:%s", e.getClass().getName(), e.getMessage())));
                }
            });
        };
        if(profile.getVersion().compareTo(ClientProfile.Version.MC164) <= 0) {
            application.gui.updateScene.sendUpdateRequest(profile.getAssetDir(), target, profile.getAssetUpdateMatcher(), true, null, false, next);
        } else {
            application.gui.updateScene.sendUpdateAssetRequest(profile.getAssetDir(), target, profile.getAssetUpdateMatcher(), true, profile.getAssetIndex(), next);
        }
    }

    private void doLaunchClient(Path assetDir, HashedDir assetHDir, Path clientDir, HashedDir clientHDir, ClientProfile profile, OptionalView view, JavaHelper.JavaVersion javaVersion, HashedDir jvmHDir) {
        RuntimeSettings.ProfileSettings profileSettings = application.getProfileSettings();
        if(javaVersion == null) {
            javaVersion = application.javaService.getRecommendJavaVersion(profile);
        }
        if(javaVersion == null) {
            javaVersion = JavaHelper.JavaVersion.getCurrentJavaVersion();
        }
        ClientLauncherProcess clientLauncherProcess = new ClientLauncherProcess(clientDir, assetDir, javaVersion,
                clientDir.resolve("resourcepacks"), profile, application.stateService.getPlayerProfile(), view,
                application.stateService.getAccessToken(), clientHDir, assetHDir, jvmHDir);
        clientLauncherProcess.params.ram = profileSettings.ram;
        clientLauncherProcess.params.offlineMode = application.offlineService.isOfflineMode();
        if (clientLauncherProcess.params.ram > 0) {
            clientLauncherProcess.jvmArgs.add("-Xms" + clientLauncherProcess.params.ram + 'M');
            clientLauncherProcess.jvmArgs.add("-Xmx" + clientLauncherProcess.params.ram + 'M');
        }
        clientLauncherProcess.params.fullScreen = profileSettings.fullScreen;
        clientLauncherProcess.params.autoEnter = profileSettings.autoEnter;
        contextHelper.runCallback(() -> {
            Thread writerThread = CommonHelper.newThread("Client Params Writer Thread", true, () -> {
                try {
                    clientLauncherProcess.runWriteParams(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
                    if (!profileSettings.debug) {
                        LogHelper.debug("Params writted successful. Exit...");
                        LauncherEngine.exitLauncher(0);
                    }
                } catch (Throwable e) {
                    LogHelper.error(e);
                    if (getCurrentStage().getVisualComponent() instanceof DebugScene) { //TODO: FIX
                        DebugScene debugScene = (DebugScene) getCurrentStage().getVisualComponent();
                        debugScene.append(String.format("Launcher fatal error(Write Params Thread): %s: %s", e.getClass().getName(), e.getMessage()));
                        if (debugScene.currentProcess != null && debugScene.currentProcess.isAlive()) {
                            debugScene.currentProcess.destroy();
                        }
                    }
                }
            });
            writerThread.start();
            application.gui.debugScene.writeParametersThread = writerThread;
            clientLauncherProcess.start(true);
            contextHelper.runInFxThread(() -> {
                switchScene(application.gui.debugScene);
                application.gui.debugScene.onProcess(clientLauncherProcess.getProcess());
            });
        });
    }

    private String getJavaDirName(Path javaPath) {
        String prefix = DirBridge.dirUpdates.toAbsolutePath().toString();
        if (javaPath == null || !javaPath.startsWith(prefix)) {
            return null;
        }
        Path result = DirBridge.dirUpdates.relativize(javaPath);
        return result.toString();
    }

    private void showJavaAlert(ClientProfile profile) {
        if((JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM32 || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64) && profile.getVersion().compareTo(ClientProfile.Version.MC112) <= 0) {
            application.messageManager.showDialog(application.getTranslation("runtime.scenes.serverinfo.javaalert.lwjgl2.header"),
                    String.format(application.getTranslation("runtime.scenes.serverinfo.javaalert.lwjgl2.description"), profile.getRecommendJavaVersion()), () -> {}, () -> {}, true);
        } else {
            application.messageManager.showDialog(application.getTranslation("runtime.scenes.serverinfo.javaalert.header"),
                    String.format(application.getTranslation("runtime.scenes.serverinfo.javaalert.description"), profile.getRecommendJavaVersion()), () -> {}, () -> {}, true);
        }
    }

    private void launchClient() {
        ClientProfile profile = application.stateService.getProfile();
        if (profile == null)
            return;
        processRequest(application.getTranslation("runtime.overlay.processing.text.setprofile"), new SetProfileRequest(profile), (result) -> contextHelper.runInFxThread(() -> {
            hideOverlay(0, (ev) -> {
                RuntimeSettings.ProfileSettings profileSettings = application.getProfileSettings();
                JavaHelper.JavaVersion javaVersion = null;
                for(JavaHelper.JavaVersion v : application.javaService.javaVersions) {
                    if(v.jvmDir.toAbsolutePath().toString().equals(profileSettings.javaPath)) {
                        javaVersion = v;
                    }
                }
                if(javaVersion == null && profileSettings.javaPath != null && !application.guiModuleConfig.forceDownloadJava) {
                    try {
                        javaVersion = JavaHelper.JavaVersion.getByPath(Paths.get(profileSettings.javaPath));
                    } catch (Throwable e) {
                        if(LogHelper.isDevEnabled()) {
                            LogHelper.error(e);
                        }
                        LogHelper.warning("Incorrect java path %s", profileSettings.javaPath);
                    }
                }
                if(javaVersion == null || application.javaService.isIncompatibleJava(javaVersion, profile)) {
                    javaVersion = application.javaService.getRecommendJavaVersion(profile);
                }
                if(javaVersion == null) {
                    showJavaAlert(profile);
                    return;
                }
                String jvmDirName = getJavaDirName(javaVersion.jvmDir);
                if (jvmDirName != null) {
                    final JavaHelper.JavaVersion finalJavaVersion = javaVersion;
                    try {
                        switchScene(application.gui.updateScene);
                    } catch (Exception e) {
                        errorHandle(e);
                    }
                    application.gui.updateScene.sendUpdateRequest(jvmDirName, javaVersion.jvmDir, null, true, application.stateService.getOptionalView(), false, (jvmHDir) -> {
                        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX || JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX) {
                            Path javaFile = finalJavaVersion.jvmDir.resolve("bin").resolve("java");
                            if(Files.exists(javaFile)) {
                                if(!javaFile.toFile().setExecutable(true)) {
                                    LogHelper.warning("Set permission for %s unsuccessful", javaFile.toString());
                                }
                            }
                        }
                        downloadClients(profile, finalJavaVersion, jvmHDir);
                    });
                } else {
                    try {
                        switchScene(application.gui.updateScene);
                    } catch (Exception e) {
                        errorHandle(e);
                    }
                    downloadClients(profile, javaVersion, null);
                }
            });
        }), null);
    }
}
