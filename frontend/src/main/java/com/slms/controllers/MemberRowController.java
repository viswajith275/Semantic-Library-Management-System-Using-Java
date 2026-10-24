package com.slms.controllers;

import com.slms.models.Member;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** One row in the administrator member directory. */
public class MemberRowController {

  @FXML private Label avatar, name, email, code, loans, status;
  @FXML private Button toggleButton;

  private Member member;
  private Consumer<Member> editAction, toggleAction, regenerateAction;

  public void configure(
    Member member,
    Consumer<Member> editAction,
    Consumer<Member> toggleAction,
    Consumer<Member> regenerateAction
  ) {
    this.member = member;
    this.editAction = editAction;
    this.toggleAction = toggleAction;
    this.regenerateAction = regenerateAction;

    avatar.setText(member.getInitials());
    name.setText(member.getName());
    email.setText(member.getEmail().isBlank() ? "No email on file" : member.getEmail());
    code.setText(member.getCustomCode());
    loans.setText(member.getActiveBorrowedCount() + (member.getActiveBorrowedCount() == 1 ? " book" : " books"));

    status.setText(member.isActive() ? "Active" : "Blocked");
    status.getStyleClass().removeAll("member-status-active", "member-status-blocked");
    status.getStyleClass().add(member.isActive() ? "member-status-active" : "member-status-blocked");

    toggleButton.setText(member.isActive() ? "Block" : "Unblock");
    toggleButton.getStyleClass().removeAll("member-action-danger", "member-action-positive");
    toggleButton.getStyleClass().add(member.isActive() ? "member-action-danger" : "member-action-positive");
  }

  @FXML
  private void edit() {
    if (editAction != null) editAction.accept(member);
  }

  @FXML
  private void toggle() {
    if (toggleAction != null) toggleAction.accept(member);
  }

  @FXML
  private void regenerate() {
    if (regenerateAction != null) regenerateAction.accept(member);
  }
}
