package org.example.smartgarage.controllers.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.example.smartgarage.dtos.request.UserUpdateDto;
import org.example.smartgarage.dtos.response.UserOutDto;
import org.example.smartgarage.exceptions.EntityDuplicateException;
import org.example.smartgarage.exceptions.EntityNotFoundException;
import org.example.smartgarage.exceptions.IllegalFileUploadException;
import org.example.smartgarage.mappers.UserMapper;
import org.example.smartgarage.models.UserEntity;
import org.example.smartgarage.security.CustomUserDetails;
import org.example.smartgarage.services.contracts.UserService;
import org.example.smartgarage.utils.filtering.UserFilterOptions;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/garage")
public class UserMvcController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserMvcController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @ModelAttribute("requestURI")
    public String requestURI(final HttpServletRequest request) {
        return request.getRequestURI();
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/hr")
    public String getHrPage() {
        return "admin";
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/client")
    public String getCustomerPage(@AuthenticationPrincipal CustomUserDetails principal,
                                  Model model) {
        UserEntity client = userService.getById(principal.getId());
        model.addAttribute("client", client);
        return "client";
    }

    @PreAuthorize("hasRole('CLERK')")
    @GetMapping("/clerk")
    public String getClerkPage() {
        return "clerk";
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping("/mechanic")
    public ModelAndView getMechanicPage(Model model) {
        return new ModelAndView("mechanic");
    }

    @PreAuthorize("hasAnyRole('CLERK','HR')")
    @GetMapping("/customers")
    public String getCustomersPage(@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                                   @RequestParam(value = "pageSize", defaultValue = "5") int pageSize,
                                   @ModelAttribute("userFilterOptions") UserFilterOptions filterOptions,
                                   Model model) {
        filterOptions.removeInvalid();

        Page<UserEntity> users = userService.findAll(pageIndex - 1, pageSize, filterOptions);
        List<UserOutDto> userOutDtos = users.map(userMapper::toDto).toList();

        Map<String, Long> userMap = new HashMap<>();
        users.forEach(user -> userMap.put(user.getEmail(), user.getId()));

        model.addAttribute("users", userOutDtos);
        model.addAttribute("userMap", userMap);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("currentPage", users.getNumber() + 1);

        return "customers";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me")
    public String getProfilePage(@AuthenticationPrincipal CustomUserDetails principal) {
        return "redirect:/garage/users/" + principal.getId();
    }

    @PreAuthorize("hasAnyRole('CLERK', 'HR') or #principal.id == #userId")
    @GetMapping("/users/{userId}")
    public String getSingleUser(@PathVariable long userId,
                                @ModelAttribute("userUpdateDto") UserUpdateDto dto,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {

        UserEntity user = userService.getById(userId);

        model.addAttribute("user", user);

        return "user-single";
    }

    @PostMapping("/users/{userId}/avatar")
    public String changeProfilePicture(@PathVariable long userId,
                                       @ModelAttribute("userUpdateDto") UserUpdateDto dto,
                                       Model model) throws IOException {
        UserEntity user = userService.getById(userId);
        try {
            userService.update(userId, user, dto.profilePic());
        } catch (EntityDuplicateException | IllegalFileUploadException e) {
            model.addAttribute("statusCode", HttpStatus.BAD_REQUEST.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "error-page";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "error-page";
        }

        return "redirect:/garage/users/" + userId;
    }
}
