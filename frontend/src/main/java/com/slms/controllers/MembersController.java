package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

public class MembersController {
    @FXML private StackPane overlay;
    @FXML private VBox dialog;
    @FXML private TextField name,email;
    @FXML private Label error,code,status;

    @FXML private void openCreate(){
        name.clear();email.clear();error.setVisible(false);error.setManaged(false);code.setVisible(false);code.setManaged(false);
        attachToWindowRoot();overlay.setVisible(true);overlay.setManaged(true);dialog.setMaxHeight(Region.USE_PREF_SIZE);
    }
    private void attachToWindowRoot(){
        if(overlay.getScene()==null)return;
        javafx.scene.Parent windowRoot=overlay.getScene().getRoot();
        if(windowRoot==overlay.getParent())return;
        if(overlay.getParent() instanceof Pane old)old.getChildren().remove(overlay);
        if(windowRoot instanceof StackPane root){root.getChildren().add(overlay);StackPane.setAlignment(overlay,javafx.geometry.Pos.CENTER);}
    }
    @FXML private void closeCreate(){overlay.setVisible(false);overlay.setManaged(false);}
    @FXML private void overlayClicked(MouseEvent e){if(e.getTarget()==overlay)closeCreate();}
    @FXML private void dialogClicked(MouseEvent e){e.consume();}
    @FXML private void create(){
        String n=name.getText().trim(),mail=email.getText().trim();if(n.isBlank()||mail.isBlank()){showError("Enter the client name and email.");return;}
        var body=ApiClient.object().put("name",n).put("email",mail);
        Task<JsonNode> task=new Task<>(){@Override protected JsonNode call()throws Exception{return ApiClient.request("POST","/api/admin/members",AuthSession.token(),null,body);}};
        task.setOnSucceeded(e->{String access=task.getValue().path("customCode").asText();code.setText("Account created. Share this access code:  "+access);code.setVisible(true);code.setManaged(true);status.setText("Client account created successfully.");error.setVisible(false);error.setManaged(false);});
        task.setOnFailed(e->showError(ApiClient.message(task.getException())));Thread t=new Thread(task,"slms-create-member");t.setDaemon(true);t.start();
    }
    private void showError(String message){error.setText(message);error.setVisible(true);error.setManaged(true);}
}
