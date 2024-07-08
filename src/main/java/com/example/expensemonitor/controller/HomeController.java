package com.example.expensemonitor.controller;


import com.example.expensemonitor.dao.UserRepository;
import com.example.expensemonitor.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;


    @GetMapping("/signup")
    public String signup(Model model, HttpSession session){
        model.addAttribute("title", "Register");
        model.addAttribute("user", new User());
        return "signup" ;
    }

    @GetMapping("/login")
    public String customLogin(Model model){
        model.addAttribute("title", "Login Page");
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/do-register")
    public String registerUser(@ModelAttribute User user){
        System.out.println("****************************************************************************************");
        System.out.println(user);
        user.setRole("ROLE_USER");
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        System.out.println(user);
        userRepository.save(user);
        System.out.println(user);
        return "redirect:/signup";
    }

    @PostMapping("/login")
    public String verifyLogin(@ModelAttribute("user") User user, Model model){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return "redirect:/user/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/home")
    public String findHome(){
        return "/user/dashboard";
    }

}
