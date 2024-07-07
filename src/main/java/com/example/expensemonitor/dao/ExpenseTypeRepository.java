package com.example.expensemonitor.dao;

import com.example.expensemonitor.model.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Long> {

    @Query("SELECT COUNT(et) > 0 FROM ExpenseType et WHERE et.user.id = :userId AND LOWER(et.expenseCategory) = LOWER(:expenseCategory)")
    boolean existsByUserIdAndExpenseCategoryIgnoreCase(
            @Param("userId") int userId,
            @Param("expenseCategory") String expenseCategory);

    List<ExpenseType> findByUserId(int userId);
}