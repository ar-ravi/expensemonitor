package com.example.expensemonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty(message = "Please specify the name of expense")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_type_id", nullable = false)
    private ExpenseType expenseType;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer=15, fraction=2)
    @NotNull(message = "Please specify an amount")
    private BigDecimal amount;

    @NotNull(message = "Date cannot be empty!")
    private LocalDate date;

    @CreationTimestamp
    private Timestamp creationDate;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public @NotNull(message = "Date cannot be empty!") LocalDate getDate() {
        return date;
    }

    public void setDate(@NotNull(message = "Date cannot be empty!") LocalDate date) {
        this.date = date;
    }

    public @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 15, fraction = 2) @NotNull(message = "Please specify an amount") BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(@DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 15, fraction = 2) @NotNull(message = "Please specify an amount") BigDecimal amount) {
        this.amount = amount;
    }

    public ExpenseType getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(ExpenseType expenseType) {
        this.expenseType = expenseType;
    }

    public @NotEmpty(message = "Please specify the name of expense") String getName() {
        return name;
    }

    public void setName(@NotEmpty(message = "Please specify the name of expense") String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", expenseType=" + expenseType +
                ", amount=" + amount +
                ", date=" + date +
                ", creationDate=" + creationDate +
                ", user=" + user +
                '}';
    }
}
