package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML private VBox clientForm, librarianForm;
    @FXML private TextField clientCode, adminEmail;
    @FXML private PasswordField adminPassword;
    @FXML private Label loginError, adminError;
    private boolean librarianMode;

    @FXML private void signIn() {
        Label error = librarianMode ? adminError : loginError;
        error.setVisible(false); error.setManaged(false);
        if (librarianMode) {
            String email=adminEmail.getText().trim(), password=adminPassword.getText();
            if(email.isBlank()||password.isBlank()){show(error,"Enter your email and password.");return;}
            request("/api/auth/admin/login", ApiClient.object().put("email",email).put("password",password), true, error);
        } else {
            String code=clientCode.getText().trim();
            if(code.isBlank()){show(error,"Enter the access code provided by your librarian.");return;}
            request("/api/auth/member/login", ApiClient.object().put("customCode",code), false, error);
        }
    }
    private void request(String path, JsonNode body, boolean admin, Label error) {
        Task<JsonNode> task=new Task<>() { @Override protected JsonNode call() throws Exception { return ApiClient.request("POST",path,null,null,body); } };
        task.setOnSucceeded(e->{ JsonNode data=task.getValue();
            if(admin) AuthSession.admin(data.path("name").asText(),data.path("email").asText(),data.path("token").asText());
            else AuthSession.member(data.path("name").asText(),data.path("customCode").asText());
            NavigationController.openHome(admin?"Librarian":"User");
        });
        task.setOnFailed(e->show(error,ApiClient.message(task.getException())));
        Thread thread=new Thread(task,"slms-login"); thread.setDaemon(true); thread.start();
    }
    private static void show(Label label,String message){label.setText(message);label.setVisible(true);label.setManaged(true);}
    @FXML private void showLibrarianLogin(){ librarianMode=true; clientForm.setVisible(false);clientForm.setManaged(false);librarianForm.setVisible(true);librarianForm.setManaged(true); }
    @FXML private void showClientLogin(){ librarianMode=false;librarianForm.setVisible(false);librarianForm.setManaged(false);clientForm.setVisible(true);clientForm.setManaged(true); }
}
