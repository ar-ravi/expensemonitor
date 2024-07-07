package com.example.expensemonitor.controller;

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

    private static final int PAGE_SIZE = 8;

    public ExpenseController(ExpenseService expenseService, ExpenseTypeService expenseTypeService, ExpenseTypeRepository expenseTypeRepository) {
        this.expenseService = expenseService;
        this.expenseTypeService = expenseTypeService;
        this.expenseTypeRepository = expenseTypeRepository;
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
    public String deleteById(@PathVariable("id") Long id, Principal principal){
        String username = principal.getName();
        User user = userRepository.getUserByUserName(username);
        Optional<ExpenseType>optionalExpenseType = expenseTypeRepository.findById(id);

        if(optionalExpenseType.isPresent()){
            ExpenseType expenseType = optionalExpenseType.get();
            if(expenseType.getUser().getId().equals(user.getId())){
                expenseTypeRepository.delete(expenseType);
            } else{

            }
        } else{

        }
        return "redirect:/newExpenseType";
    }
}
