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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.Month;
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

    private static final int PAGE_SIZE = 7;

    public ExpenseController(ExpenseService expenseService, ExpenseTypeService expenseTypeService, ExpenseTypeRepository expenseTypeRepository, ExpenseRepository expenseRepository) {
        this.expenseService = expenseService;
        this.expenseTypeService = expenseTypeService;
        this.expenseTypeRepository = expenseTypeRepository;
        this.expenseRepository = expenseRepository;
    }

    @ModelAttribute("totalAmount")
    public BigDecimal getTotalAmount() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        // Assuming you have a method to get user by username
        User currentUser = userRepository.getUserByUserName(username);

        if (currentUser != null) {
            Iterable<Expense> expenses = expenseService.findAllByUserId(currentUser.getId());
            return expenseService.getTotalAmount(expenses);
        } else {
            // Handle the case where the user is not found
            return BigDecimal.ZERO;
        }
    }


    @ModelAttribute("expenses")
    public Page<Expense> getExpenses(@PageableDefault(size = PAGE_SIZE) Pageable page) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        // Assuming you have a method to get user by username
        User currentUser = userRepository.getUserByUserName(username);

        if (currentUser != null) {
            return expenseService.findAllByUserId(currentUser.getId(), page);
        } else {
            // Handle the case where the user is not found
            return Page.empty();
        }
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


//    ------------------------------------------------------------------------SHOW USER DASHBOARD ------------------------------------------------------------------------
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.getUserByUserName(username);

        System.out.println("____________________________________________________________________________________________");
        System.out.println(user);


        Page<Expense> recentExpenses = expenseService.findAllByUserId(user.getId(), PageRequest.of(0, 5, Sort.by("date").descending()));


        LocalDate now = LocalDate.now();
        BigDecimal monthlyTotal = expenseService.getTotalAmountForMonthAndYear(user.getId(), now.getMonthValue(), now.getYear());


        BigDecimal yearlyTotal = expenseService.getTotalAmountForYear(user.getId(), now.getYear());


        model.addAttribute("user", user);
        model.addAttribute("recentExpenses", recentExpenses.getContent());
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("yearlyTotal", yearlyTotal);

        List<ExpenseType> userExpenseTypes = expenseTypeRepository.findByUserId(user.getId());
        model.addAttribute("expenseTypes", userExpenseTypes);

        return "user/dashboard";
    }

//    ----------------------------------------------------------------------------SHOW EXPENSE TYPE FORM---------------------------------------------------------------------------
    @GetMapping("/newExpenseType")
    public String showExpenseTypes(Model model, Principal principal){
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);
        model.addAttribute("userId", user.getId());
        model.addAttribute("expenseType", new ExpenseType());

        List<ExpenseType> userExpenseTypes = expenseTypeRepository.findByUserId(user.getId());
        model.addAttribute("expenseTypes", userExpenseTypes);

        model.addAttribute("page", "updatePage");

        return "user/newExpenseType";
    }

//    ----------------------------------------------------------------------------ADD NEW EXPENSE TYPE----------------------------------------------------------------------------------
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

//    ----------------------------------------------------------------------------DELETE EXPENSE TYPE-----------------------------------------------------------------------
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


//    -------------------------------------------------------ADD NEW EXPENSE-----------------------------------------------------------------------------
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

//    --------------------------------------------------------------------------DELETE AN EXPENSE------------------------------------------------------------
    @PostMapping("/delete/{id}")
    public String deleteExpenseById(@PathVariable("id") Long id, Principal principal){

       try{
           String username = principal.getName();
           expenseService.deleteExpenseById(id, username);
       } catch (Exception e){
           e.printStackTrace();
           return "/error";
       }

        return "redirect:/dashboard";
    }

//    ------------------------------------------------------------------------UPDATE AN EXPENSE-------------------------------------------------------------
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
            return "/error";
        }
        return "redirect:/dashboard";
    }


//    -------------------------------------------------------------FILTERING EXPENSE-------------------------------------------------------------------------------------
    @GetMapping("/expenses/filter")
    public String filterExpenses(@RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) String month,
                                 @RequestParam(required = false) String expenseTypeFilter,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "7") int size,
                                 Model model,
                                 Principal principal) {
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);

        Month monthEnum = null;
        if (month != null && !month.isEmpty()) {
            monthEnum = Month.valueOf(month.toUpperCase());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Expense> filteredExpenses = expenseService.getFilteredExpenses(user.getId(), year, monthEnum, expenseTypeFilter, pageable);

        // Add filtered expenses and filter parameters
        model.addAttribute("expenses", filteredExpenses);
        model.addAttribute("currentYear", year);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentExpenseType", expenseTypeFilter);

        // Add pagination information
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", filteredExpenses.getTotalPages());
        model.addAttribute("totalItems", filteredExpenses.getTotalElements());

        // Add user data
        model.addAttribute("user", user);

        // Add expense types
        List<ExpenseType> userExpenseTypes = expenseTypeRepository.findByUserId(user.getId());
        model.addAttribute("expenseTypes", userExpenseTypes);

        // Calculate and add totals
        LocalDate now = LocalDate.now();
        BigDecimal monthlyTotal = expenseService.getTotalAmountForMonthAndYear(user.getId(), now.getMonthValue(), now.getYear());
        BigDecimal yearlyTotal = expenseService.getTotalAmountForYear(user.getId(), now.getYear());
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("yearlyTotal", yearlyTotal);

        BigDecimal totalAmount = filteredExpenses.getContent().stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalAmount", totalAmount);

        return "user/filtered-dashboard";
    }




}
