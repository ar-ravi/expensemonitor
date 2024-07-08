package com.example.expensemonitor.dao;

import com.example.expensemonitor.model.Expense;
import com.example.expensemonitor.model.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {



    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesByUserAndDateRange(
            @Param("userId") int userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByExpenseType(ExpenseType expenseType);

    Page<Expense> findAllByUserId(Long userId, Pageable pageable);

    Iterable<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    Page<Expense> findByUserIdAndDateBetweenOrderByCreationDateDesc(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Expense> findByUserIdAndExpenseType_ExpenseCategoryOrderByCreationDateDesc(Long userId, String expenseType, Pageable pageable);

    Page<Expense> findByUserIdAndDateBetweenAndExpenseType_ExpenseCategoryOrderByCreationDateDesc(Long userId,
                                                                                                  LocalDate startDate,
                                                                                                  LocalDate endDate,
                                                                                                  String expenseType,
                                                                                                  Pageable pageable);


    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
            "AND (:startDate IS NULL OR e.date >= :startDate) " +
            "AND (:endDate IS NULL OR e.date <= :endDate) " +
            "AND (:expenseType IS NULL OR e.expenseType.expenseCategory = :expenseType) " +
            "ORDER BY e.creationDate DESC")
    Page<Expense> findFilteredExpenses(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("expenseType") String expenseType,
            Pageable pageable
    );

}

