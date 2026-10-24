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

/** Member borrowing list. The current API supplies active loans only. */
public class UserBorrowsController {
    @FXML private Label activeCount, dueSoonCount, overdueCount, loanCount, statusLabel;
    @FXML private VBox currentLoansContainer, emptyCurrentState;

    private final List<MemberLoan> loans = new ArrayList<>();

    @FXML private void initialize() { loadLoans(); }

    @FXML private void refresh() { loadLoans(); }

    private void loadLoans() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        Task<JsonNode> task = new Task<>() {
            @Override protected JsonNode call() throws Exception {
                return ApiClient.request("GET", "/api/member/loans", AuthSession.token(), AuthSession.code(), null);
            }
        };
        task.setOnSucceeded(e -> {
            loans.clear();
            JsonNode data = task.getValue();
            if (data != null && data.isArray()) data.forEach(node -> loans.add(MemberLoan.from(node)));
            render();
        });
        task.setOnFailed(e -> {
            showStatus("Could not load borrowed books: " + ApiClient.message(task.getException()));
            render();
        });
        Thread thread = new Thread(task, "slms-member-loans");
        thread.setDaemon(true);
        thread.start();
    }

    private void render() {
        currentLoansContainer.getChildren().clear();
        int count = loans.size();
        loanCount.setText(count + (count == 1 ? " book" : " books"));
        activeCount.setText(String.valueOf(count));
        dueSoonCount.setText(String.valueOf(loans.stream()
            .filter(loan -> !loan.isOverdue() && loan.getRemainingDays() >= 0 && loan.getRemainingDays() <= 3)
            .count()));
        overdueCount.setText(String.valueOf(loans.stream().filter(MemberLoan::isOverdue).count()));

        boolean empty = loans.isEmpty();
        currentLoansContainer.setVisible(!empty);
        currentLoansContainer.setManaged(!empty);
        emptyCurrentState.setVisible(empty);
        emptyCurrentState.setManaged(empty);
        if (empty) return;

        List<MemberLoan> ordered = loans.stream().sorted((a, b) -> {
            if (a.isOverdue() != b.isOverdue()) return Boolean.compare(b.isOverdue(), a.isOverdue());
            if (a.getDueDate() == null) return b.getDueDate() == null ? 0 : 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        }).toList();

        for (MemberLoan loan : ordered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/MemberLoanCard.fxml"));
                currentLoansContainer.getChildren().add(loader.load());
                loader.<MemberLoanCardController>getController().configure(loan);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load loan card", ex);
            }
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-message-error", "status-message-success");
        statusLabel.getStyleClass().add("status-message-error");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
