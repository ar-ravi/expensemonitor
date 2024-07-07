package com.example.expensemonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class ExpenseType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Please specify the type of expense")
    private String expenseCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true)
    private String uniqueUserExpenseType;

    @PrePersist
    @PreUpdate
    private void generateUniqueUserExpenseType() {
        this.uniqueUserExpenseType = this.user.getId() + "_" + this.expenseCategory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotEmpty(message = "Please specify the type of expense") String getExpenseCategory() {
        return expenseCategory;
    }

    public void setExpenseCategory(@NotEmpty(message = "Please specify the type of expense") String expenseCategory) {
        this.expenseCategory = expenseCategory;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUniqueUserExpenseType() {
        return uniqueUserExpenseType;
    }

    public void setUniqueUserExpenseType(String uniqueUserExpenseType) {
        this.uniqueUserExpenseType = uniqueUserExpenseType;
    }

    // Getters and setters
}