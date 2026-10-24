package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.models.MemberLoan;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class UserBorrowsController {
    @FXML private Label activeCount, dueSoonCount, overdueCount;
    @FXML private Label currentLoanCount, statusLabel;
    @FXML private VBox currentLoansContainer, emptyCurrentState;

    private final List<MemberLoan> currentLoans = new ArrayList<>();

    @FXML
    private void initialize() {
        loadLoans();
    }

    @FXML
    private void refresh() {
        loadLoans();
    }

    private void loadLoans() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        
        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return ApiClient.request("GET", "/api/member/loans", AuthSession.token(), AuthSession.code(), null);
            }
        };

        task.setOnSucceeded(e -> {
            currentLoans.clear();
            JsonNode data = task.getValue();
            if (data != null && data.isArray()) {
                data.forEach(node -> currentLoans.add(MemberLoan.from(node)));
            }
            render();
        });

        task.setOnFailed(e -> {
            showStatus("Failed to load loans: " + ApiClient.message(task.getException()), true);
            render();
        });

        Thread thread = new Thread(task, "slms-member-loans");
        thread.setDaemon(true);
        thread.start();
    }

    private void render() {
        currentLoansContainer.getChildren().clear();
        
        long dueSoon = currentLoans.stream().filter(l -> !l.isOverdue() && l.getRemainingDays() <= 3 && l.getRemainingDays() >= 0).count();
        long overdue = currentLoans.stream().filter(MemberLoan::isOverdue).count();
        long active = currentLoans.size();
        
        activeCount.setText(String.valueOf(active));
        dueSoonCount.setText(String.valueOf(dueSoon));
        overdueCount.setText(String.valueOf(overdue));
        currentLoanCount.setText(active == 1 ? "1 loan" : active + " loans");
        
        if (currentLoans.isEmpty()) {
            emptyCurrentState.setVisible(true);
            emptyCurrentState.setManaged(true);
            currentLoansContainer.setVisible(false);
            currentLoansContainer.setManaged(false);
            return;
        }
        
        emptyCurrentState.setVisible(false);
        emptyCurrentState.setManaged(false);
        currentLoansContainer.setVisible(true);
        currentLoansContainer.setManaged(true);
        
        // Sort: overdue first, then due soon, then by due date
        List<MemberLoan> sorted = currentLoans.stream()
            .sorted((a, b) -> {
                if (a.isOverdue() != b.isOverdue()) return Boolean.compare(b.isOverdue(), a.isOverdue());
                if (a.getDueDate() == null) return 1;
                if (b.getDueDate() == null) return -1;
                return a.getDueDate().compareTo(b.getDueDate());
            })
            .toList();
        
        for (MemberLoan loan : sorted) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/MemberLoanCard.fxml"));
                currentLoansContainer.getChildren().add(loader.load());
                MemberLoanCardController controller = loader.getController();
                controller.configure(loan);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load loan card", ex);
            }
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-message-error", "status-message-success");
        statusLabel.getStyleClass().add(isError ? "status-message-error" : "status-message-success");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
