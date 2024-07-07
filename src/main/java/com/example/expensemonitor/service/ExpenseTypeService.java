package com.example.expensemonitor.service;

import com.example.expensemonitor.dao.ExpenseTypeRepository;
import org.springframework.stereotype.Service;

@Service
public class ExpenseTypeService {
    private final ExpenseTypeRepository expenseTypeRepository;

    public ExpenseTypeService(ExpenseTypeRepository expenseTypeRepository) {
        this.expenseTypeRepository = expenseTypeRepository;
    }
}
