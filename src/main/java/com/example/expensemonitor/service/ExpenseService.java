package com.example.expensemonitor.service;

import com.example.expensemonitor.dao.ExpenseRepository;
import com.example.expensemonitor.model.Expense;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.stream.StreamSupport;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
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

    /**
     *
     * @param id
     * @returns the saved entity if found, otherwise throws EntityNotFoundException
     */
    public Expense findById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sorry, the content you are looking for does not exist."));
    }


    /**
     * Retrieves a Page of Expense objects sorted by their creation date in descending order.
     *
     * The provided Pageable object determines the page number, page size, and any additional sorting or filtering options.
     * It is to ensure that each new expense added will stack on top.
     *
     * @param pageable The Pageable object specifying the page number, page size, and sorting preferences.
     * @return A Page containing a list of Expense objects sorted by their creation date in descending order.
     */
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

    /**
     * Estimates the total amount of expenses from the given Iterable of Expense objects.
     *
     * @param expenses An Iterable of Expense objects containing expense data.
     * @return The total amount of expenses as a BigDecimal value.
     */
    public BigDecimal getTotalAmount(Iterable<Expense> expenses){
        return StreamSupport.
                stream(expenses.spliterator(), false)
                .toList()
                .stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retrieves a Page of Expense objects filtered by year, month, and expense type.
     *
     * This method queries the expense repository to retrieve expenses within the specified year and month,
     * belonging to the given expense type. The results are ordered by creation date in descending order.
     *
     * @param year The year for filtering expenses.
     * @param month The month for filtering expenses.
     * @param expenseType The expense type for filtering expenses.
     * @param page The Pageable object specifying the desired page and page size.
     * @return A Page containing filtered Expense objects.
     */
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

    // You might want to update this method to include userId
    public void deleteById(Long id, Long userId) {
        Expense expenseToBeDeleted = findById(id);
        if (expenseToBeDeleted.getId().equals(userId)) {
            expenseRepository.delete(expenseToBeDeleted);
        } else {
            throw new IllegalArgumentException("User does not have permission to delete this expense");
        }
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


}
