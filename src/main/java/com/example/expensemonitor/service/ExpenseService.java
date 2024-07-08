package com.example.expensemonitor.service;

import com.example.expensemonitor.dao.ExpenseRepository;
import com.example.expensemonitor.dao.UserRepository;
import com.example.expensemonitor.model.Expense;
import com.example.expensemonitor.model.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class ExpenseService {
    @Autowired
    private final ExpenseRepository expenseRepository;

    @Autowired
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public Page<Expense> findAll(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("creationDate").descending());

        return expenseRepository.findAll(sortedPageable);
    }

    public Iterable<Expense> findAll() {
        return expenseRepository.findAll();
    }

    public Expense save(Expense entity) {
        return expenseRepository.save(entity);
    }

    public Expense findById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sorry, the content you are looking for does not exist."));
    }

    public Page<Expense> findAllByUserId(Long userId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("creationDate").descending());

        return expenseRepository.findAllByUserId(userId, sortedPageable);
    }


    public Iterable<Expense> findAllByUserId(Long userId) {
        return expenseRepository.findAllByUserId(userId, Pageable.unpaged()).getContent();
    }

    public void deleteById(Long id) {
        Expense expenseToBeDeleted = findById(id);
        expenseRepository.delete(expenseToBeDeleted);
    }


    public BigDecimal getTotalAmount(Iterable<Expense> expenses){
        return StreamSupport.
                stream(expenses.spliterator(), false)
                .toList()
                .stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public Page<Expense> getExpensesByYearMonthAndType(Long userId, int year, Month month, String expenseType, Pageable page) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return expenseRepository.findByUserIdAndDateBetweenAndExpenseType_ExpenseCategoryOrderByCreationDateDesc(
                userId, startDate, endDate, expenseType, page);
    }

    public Page<Expense> getExpensesByYearMonth(Long userId, int year, Month month, Pageable page) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return expenseRepository.findByUserIdAndDateBetweenOrderByCreationDateDesc(userId, startDate, endDate, page);
    }

    public Page<Expense> getExpensesByType(Long userId, String expenseType, Pageable page) {
        return expenseRepository.findByUserIdAndExpenseType_ExpenseCategoryOrderByCreationDateDesc(userId, expenseType, page);
    }


    // Update this method to include userId
    public Expense save(Expense entity, Long userId) {
        entity.setId(userId);
        return expenseRepository.save(entity);
    }
    public BigDecimal getTotalAmountForMonthAndYear(Long userId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        Iterable<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return getTotalAmount(expenses);
    }

    public BigDecimal getTotalAmountForYear(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        Iterable<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return getTotalAmount(expenses);
    }

    @Transactional
    public void updateExpense(Expense newExpense, String username) throws Exception {
        Optional<Expense> oldOptionalExpense = expenseRepository.findById(newExpense.getId());
        if (!oldOptionalExpense.isPresent()) {
            throw new Exception("Expense not found");
        }

        Expense oldExpense = oldOptionalExpense.get();
        User user = userRepository.getUserByUserName(username);

        if (!user.getExpenses().contains(oldExpense)) {
            throw new Exception("Expense does not belong to user");
        }

        // Update fields of the existing expense
        oldExpense.setExpenseType(newExpense.getExpenseType());
        oldExpense.setAmount(newExpense.getAmount());
        oldExpense.setDate(newExpense.getDate());
        oldExpense.setCreationDate(newExpense.getCreationDate());
        oldExpense.setName(newExpense.getName());

        expenseRepository.save(oldExpense);
    }

    @Transactional
    public void deleteExpenseById(Long id, String username) throws Exception {
        User user = userRepository.getUserByUserName(username);
        Optional<Expense> optionalExpense = expenseRepository.findById(id);
        if (optionalExpense.isPresent()) {
            Expense expense = optionalExpense.get();
            if (!user.getExpenses().contains(expense)) {
                throw new Exception("Expense does not belong to user");
            }
            user.getExpenses().remove(expense);
            expenseRepository.delete(expense);
        } else {
            throw new Exception("Expense not found");
        }
    }

    public Page<Expense> getFilteredExpenses(Long userId, Integer year, Month month, String expenseType, Pageable page) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (year != null && month != null) {
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        if (expenseType != null && expenseType.trim().isEmpty()) {
            expenseType = null;
        }

        return expenseRepository.findFilteredExpenses(userId, startDate, endDate, expenseType, page);
    }


}
