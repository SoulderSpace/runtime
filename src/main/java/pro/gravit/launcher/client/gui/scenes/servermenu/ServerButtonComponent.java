package pro.gravit.launcher.client.gui.scenes.servermenu;

import javafx.animation.ScaleTransition;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.LogHelper;

import java.net.URL;

public class ServerButtonComponent extends AbstractVisualComponent {
    private static final String SERVER_BUTTON_FXML = "components/serverButton.fxml";
    private static final String SERVER_BUTTON_CUSTOM_FXML = "components/serverButton/%s.fxml";
    public ClientProfile profile;
    protected Scene scene;
    protected ServerButtonComponent(JavaFXApplication application, ClientProfile profile) {
        super(getFXMLPath(application, profile), application);
        this.profile = profile;
        if (scene == null) {
            scene = new Scene(getFxmlRoot());
            scene.setFill(Color.TRANSPARENT);
        }
        this.layout = (Pane) LookupHelper.lookupIfPossible(scene.getRoot(), "#layout").orElse(scene.getRoot());
    }

    private static String getFXMLPath(JavaFXApplication application, ClientProfile profile) {
        String customFxmlName = String.format(SERVER_BUTTON_CUSTOM_FXML, profile.getUUID());
        URL customFxml = application.tryResource(customFxmlName);
        if (customFxml != null) {
            return customFxmlName;
        }
        return SERVER_BUTTON_FXML;
    }

    @Override
    public String getName() {
        return "serverButton";
    }

    @Override
    protected void doInit() throws Exception {
        if (profile.getProperty("ImageServerURL") != null){
            LookupHelper.lookupIfPossible(layout, "#serverButtonLayoutImage").ifPresent((e) -> {
                e.setStyle("-fx-background-image: url(" + profile.getProperty("ImageServerURLButton") + ");");
            });
        }
        LookupHelper.<Labeled>lookup(layout, "#nameServer").setText(profile.getTitle());
        LookupHelper.<Labeled>lookup(layout, "#version").setText(profile.getAssetIndex());
        LookupHelper.<Labeled>lookup(layout, "#genreServer").setText(profile.getInfo());
        LookupHelper.<ImageView>lookupIfPossible(layout, "#serverLogo").ifPresent((a) -> {
            try {
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(a.getFitWidth(), a.getFitHeight());
                clip.setArcWidth(20.0);
                clip.setArcHeight(20.0);
                a.setClip(clip);
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        });
        application.pingService.getPingReport(profile.getDefaultServerProfile().name).thenAccept((report) -> {
            if(report == null) {
                LookupHelper.<Label>lookup(layout, "#online").setText("Ошибка при получении");
            } else {
                LookupHelper.<Label>lookup(layout, "#online").setText(report.playersOnline + " из " + report.maxPlayers);
            }
        });
        layout.setOnMouseEntered((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.2), layout);
            transition.setToX(1.02);
            transition.setToY(1.02);
            transition.play();
        });
        layout.setOnMouseExited((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.2), layout);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
        });
        LookupHelper.lookup(layout, "#moreInfo").setOnMouseClicked((e) -> {
            application.openURL(profile.getProperty("moreInfoURL"));
        });
    }

//    private void changeServer(ClientProfile profile) {
//        application.stateService.setProfile(profile);
//        application.runtimeSettings.lastProfile = profile.getUUID();
//    }
//    public void setOnMouseClicked(EventHandler<? super MouseEvent> eventHandler) {
//        layout.setOnMouseClicked(eventHandler);
//    }

    public Button getButtonComponent(){
        return LookupHelper.lookup(layout, "#play");
    }
    public void addTo(Pane pane) {
        if (!isInit()) {
            try {
                init();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        pane.getChildren().add(layout);
    }

    @Override
    public void reset() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void enable() {

    }

    public void setOnMouseClicked(EventHandler<? super MouseEvent> handle) {
    }
}
