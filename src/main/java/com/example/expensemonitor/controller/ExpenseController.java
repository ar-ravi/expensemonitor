package com.example.expensemonitor.controller;

import com.example.expensemonitor.dao.ExpenseRepository;
import com.example.expensemonitor.dao.ExpenseTypeRepository;
import com.example.expensemonitor.dao.UserRepository;
import com.example.expensemonitor.model.Expense;
import com.example.expensemonitor.model.ExpenseType;
import com.example.expensemonitor.model.User;
import com.example.expensemonitor.service.CustomUserDetailsService;
import com.example.expensemonitor.service.ExpenseService;
import com.example.expensemonitor.service.ExpenseTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class ExpenseController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private final ExpenseService expenseService;

    @Autowired
    private final ExpenseTypeService expenseTypeService;

    @Autowired
    private final ExpenseTypeRepository expenseTypeRepository;

    @Autowired
    private final ExpenseRepository expenseRepository;

    private static final int PAGE_SIZE = 8;

    public ExpenseController(ExpenseService expenseService, ExpenseTypeService expenseTypeService, ExpenseTypeRepository expenseTypeRepository, ExpenseRepository expenseRepository) {
        this.expenseService = expenseService;
        this.expenseTypeService = expenseTypeService;
        this.expenseTypeRepository = expenseTypeRepository;
        this.expenseRepository = expenseRepository;
    }

    @ModelAttribute("totalAmount")
    public BigDecimal getTotalAmount(@RequestParam(required = false) Long userId) {
        Iterable<Expense> expenses = userId != null ?
                expenseService.findAllByUserId(userId) :
                expenseService.findAll();
        return expenseService.getTotalAmount(expenses);
    }


    @ModelAttribute("expenses")
    public Page<Expense> getExpenses(@PageableDefault(size = PAGE_SIZE) Pageable page,
                                     @RequestParam(required = false) Long userId) {
        return userId != null ?
                expenseService.findAllByUserId(userId, page) :
                expenseService.findAll(page);
    }

    @GetMapping("/expenses")
    public String showExpenses(@RequestParam(required = false) Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "/expenses";
    }

    @ModelAttribute
    public Expense getExpense(){
        return new Expense();
    }

    @ModelAttribute
    public ExpenseType getExpenseType(){
        return new ExpenseType();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Fetch user data
        User user = userRepository.getUserByUserName(username);

        System.out.println(user);

        // Fetch recent expenses (e.g., last 5)
        Page<Expense> recentExpenses = expenseService.findAllByUserId(user.getId(), PageRequest.of(0, 5, Sort.by("date").descending()));

        // Calculate total expenses for the current month
        LocalDate now = LocalDate.now();
        BigDecimal monthlyTotal = expenseService.getTotalAmountForMonthAndYear(user.getId(), now.getMonthValue(), now.getYear());

        // Calculate total expenses for the current year
        BigDecimal yearlyTotal = expenseService.getTotalAmountForYear(user.getId(), now.getYear());

        // Add data to the model
        model.addAttribute("user", user);
        model.addAttribute("recentExpenses", recentExpenses.getContent());
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("yearlyTotal", yearlyTotal);

        List<ExpenseType> userExpenseTypes = expenseTypeRepository.findByUserId(user.getId());
        model.addAttribute("expenseTypes", userExpenseTypes);

        return "user/dashboard";
    }

    @GetMapping("/newExpenseType")
    public String showExpenseTypes(Model model, Principal principal){
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);
        model.addAttribute("userId", user.getId());
        model.addAttribute("expenseType", new ExpenseType());

        List<ExpenseType> userExpenseTypes = expenseTypeRepository.findByUserId(user.getId());
        model.addAttribute("expenseTypes", userExpenseTypes);

        return "user/newExpenseType";
    }

    @PostMapping("/newExpenseType")
    public String addExpenseTypes(@ModelAttribute @Valid ExpenseType expenseType, BindingResult bindingResult, @RequestParam("userId") Integer userId, Model model){
        if(bindingResult.hasErrors()){
            model.addAttribute("expenseType", expenseType);
            return "/newExpenseType";
        }
        Optional<User> optionalUser = userRepository.findById(userId);
        if(!optionalUser.isPresent()){
            return "redirect:/error";
        }
        User user = optionalUser.get();
        expenseType.setUser(user);
        expenseTypeRepository.save(expenseType);

        return "redirect:/dashboard";
    }

    @PostMapping("/newExpenseType/delete/{id}")
    public String deleteById(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes){
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);
        Optional<ExpenseType>optionalExpenseType = expenseTypeRepository.findById(id);

        if(optionalExpenseType.isPresent()){
            ExpenseType expenseType = optionalExpenseType.get();
            if(expenseType.getUser().getId().equals(user.getId())){
                boolean hasExpenses = expenseRepository.existsByExpenseType(expenseType);
                if(hasExpenses){
                    redirectAttributes.addFlashAttribute("error", "Cannot delete expense type.There are existing expenses of this type.");
                } else{
                    expenseTypeRepository.delete(expenseType);
                    redirectAttributes.addFlashAttribute("success", "Expense type deleted.");
                }
            } else{
                redirectAttributes.addFlashAttribute("error", "You don't have the permission to delete this expense type.");
            }
        } else{
            redirectAttributes.addFlashAttribute("error", "Expense type not found.");
        }
        return "redirect:/newExpenseType";
    }

    @PostMapping("/addExpense")
    public String addExpense(@ModelAttribute @Valid Expense expense,
                             BindingResult bindingResult,
                             Principal principal,
                             Model model){
        if (bindingResult.hasErrors()) {
            model.addAttribute("expense", expense);
            return "redirect:/dashboard";
        }
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);

        System.out.println(expense);

        expense.setUser(user);
        ExpenseType expenseType = expenseTypeRepository.findByExpenseCategoryAndUser(expense.getExpenseType().getExpenseCategory(), user)
                .orElseThrow(() -> new RuntimeException("Invalid Expense Type"));

        expense.setExpenseType(expenseType);
        expenseRepository.save(expense);

        return "redirect:/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String deleteExpenseById(@PathVariable("id") Long id, Principal principal){

        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);
        System.out.println(user);

        Optional<Expense> optionalExpense = expenseRepository.findById(id);
        if(optionalExpense.isPresent()){
            Expense expense = optionalExpense.get();
            System.out.println(expense);
            List<Expense>userExpense = user.getExpenses();
            userExpense.remove(expense);

            expenseRepository.delete(expense);
        }



        return "redirect:/dashboard";
    }

    @GetMapping("/update/{id}")
    public String showUpdateExpenseForm(@PathVariable("id") Long expenseId, Model model, Principal principal){
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);

        Optional<Expense> optionalExpense = expenseRepository.findById(expenseId);
        if(optionalExpense.isPresent()){
            Expense expense = optionalExpense.get();
            System.out.println(expense);
            model.addAttribute("expense", expense);

            Optional<ExpenseType>optionalExpenseType = expenseTypeRepository.findById(expense.getExpenseType().getId());
            if(optionalExpenseType.isPresent()){
                ExpenseType expenseType = optionalExpenseType.get();
                model.addAttribute("currentExpenseType", expenseType);
            }
            List<ExpenseType> userExpenseTypes = expenseTypeRepository.findByUserId(user.getId());
            model.addAttribute("expenseTypes", userExpenseTypes);
            return "user/updateExpense";
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/update")
    public String updateExpense(@ModelAttribute @Valid Expense expense, Principal principal, Model model){
        try {
            String username = principal.getName();
            expenseService.updateExpense(expense, username);
        } catch (Exception e){
            e.printStackTrace();
        }
        return "redirect:/dashboard";
    }



}
