package org.example.smartgarage.controllers.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.smartgarage.dtos.request.VehicleInDTO;
import org.example.smartgarage.dtos.response.VehicleBrandOutDTO;
import org.example.smartgarage.dtos.response.VehicleModelOutDTO;
import org.example.smartgarage.dtos.response.VehicleOutDTO;
import org.example.smartgarage.exceptions.EntityDuplicateException;
import org.example.smartgarage.exceptions.EntityNotFoundException;
import org.example.smartgarage.mappers.VehicleBrandMapper;
import org.example.smartgarage.mappers.VehicleMapper;
import org.example.smartgarage.mappers.VehicleModelMapper;
import org.example.smartgarage.models.*;
import org.example.smartgarage.security.CustomUserDetails;
import org.example.smartgarage.services.contracts.*;
import org.example.smartgarage.utils.filtering.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/garage")
public class VehicleMvcController {

    private final VehicleService vehicleService;
    private final VehicleBrandService vehicleBrandService;
    private final VehicleModelService vehicleModelService;
    private final VehicleYearService vehicleYearService;
    private final UserService userService;
    private final VisitService visitService;
    private final VehicleMapper vehicleMapper;
    private final VehicleBrandMapper vehicleBrandMapper;
    private final VehicleModelMapper vehicleModelMapper;

    public VehicleMvcController(VehicleService vehicleService, VehicleBrandService vehicleBrandService, VehicleModelService vehicleModelService, VehicleYearService vehicleYearService, UserService userService, VisitService visitService, VehicleMapper vehicleMapper, VehicleBrandMapper vehicleBrandMapper, VehicleModelMapper vehicleModelMapper) {
        this.vehicleService = vehicleService;
        this.vehicleBrandService = vehicleBrandService;
        this.vehicleModelService = vehicleModelService;
        this.vehicleYearService = vehicleYearService;
        this.userService = userService;
        this.visitService = visitService;
        this.vehicleMapper = vehicleMapper;
        this.vehicleBrandMapper = vehicleBrandMapper;
        this.vehicleModelMapper = vehicleModelMapper;
    }

    @ModelAttribute("requestURI")
    public String requestURI(final HttpServletRequest request) {
        return request.getRequestURI();
    }

    @GetMapping("/brands")
    public String getBrands(@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                            @ModelAttribute("vehicleBrandFilterOptions") VehicleBrandFilterOptions filterOptions,
                            Model model) {

        filterOptions.removeInvalid();

        Page<VehicleBrand> brands = vehicleBrandService.getAll(pageIndex - 1, pageSize, filterOptions);
        //Page<VehicleBrandOutDTO> brandOutDTOS = vehicleBrandMapper.vehicleBrandsToVehicleBrandDTOs(brands);

        model.addAttribute("brands", brands);
        model.addAttribute("currentPage", brands.getNumber() + 1);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", brands.getTotalPages());
        return "brands";
    }

    @GetMapping("/brands/{brandId}/models")
    public String getModels(@PathVariable long brandId,
                            @RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                            @ModelAttribute("vehicleModelFilterOptions") VehicleModelFilterOptions filterOptions,
                            Model model) {

        filterOptions.removeInvalid();
        VehicleBrand brand = vehicleBrandService.getById(brandId);
        Page<VehicleModel> models = vehicleModelService.getByBrand(brand, pageIndex - 1, pageSize, filterOptions);
        Page<VehicleModelOutDTO> modelOutDTOS = vehicleModelMapper.vehicleModelsToVehicleModelDTOs(models);

        model.addAttribute("brandId", brandId);
        model.addAttribute("currentPage", models.getNumber() + 1);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", models.getTotalPages());
        model.addAttribute("models", modelOutDTOS);
        return "models";
    }

    @GetMapping("/vehicles")
    public String getAllVehiclesPage(@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                                     @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                     @ModelAttribute("vehicleFilterOptions") VehicleFilterOptions filterOptions,
                                     Model model) {

        filterOptions.removeInvalid();

        Page<Vehicle> vehicles = vehicleService.getAll(pageIndex - 1, pageSize, filterOptions);
        List<VehicleOutDTO> vehicleOutDTOS = vehicles.map(vehicleMapper::toDTO).toList();

        Map<String, Long> ownerMap = new HashMap<>();
        vehicles.forEach(vehicle -> ownerMap.put(vehicle.getVin(), vehicle.getOwner().getId()));

        model.addAttribute("vehicles", vehicleOutDTOS);
        model.addAttribute("ownerMap", ownerMap);
        model.addAttribute("totalPages", vehicles.getTotalPages());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("currentPage", vehicles.getNumber() + 1);

        return "vehicles";
    }

    @GetMapping("vehicles/client/{customerId}")
    public String getVehiclesForCustomer(@PathVariable long customerId,
                                         @RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                                         @RequestParam(value = "pageSize", defaultValue = "5") int pageSize,
                                         @ModelAttribute("vehicleFilterOptions") VehicleFilterOptions filterOptions,
                                         @AuthenticationPrincipal CustomUserDetails principal,
                                         Model model){


        filterOptions.removeInvalid();

        if (principal.getId() != customerId) {
            return "403";
        }
        UserEntity client = userService.getById(customerId);
        filterOptions.setOwner(Optional.of(client.getPhoneNumber()));
        Page<Vehicle> vehicles = vehicleService.getAll( pageIndex - 1, pageSize, filterOptions);

        List<VehicleOutDTO> vehicleOutDTOS = vehicles.stream().map(vehicleMapper::toDTO).toList();

        model.addAttribute("vehicles", vehicleOutDTOS);
        model.addAttribute("totalPages", vehicles.getTotalPages());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("currentPage", vehicles.getNumber() + 1);

        return "vehicles-client";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vehicles/{vehicleId}")
    public String showSingleVehiclePage(Model model, @PathVariable Long vehicleId) {

        Vehicle vehicle;
        try {
            vehicle = vehicleService.getById(vehicleId);
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.CONFLICT.getReasonPhrase());
            model.addAttribute("error", "Client Car has been scraped");
            return "error-page";
        }

        if (vehicle.isDeleted()) {
            model.addAttribute("statusCode", HttpStatus.CONFLICT.getReasonPhrase());
            model.addAttribute("error", "Client Car has been scraped");
            return "error-page";
        }
        VehicleOutDTO dto = vehicleMapper.toDTO(vehicle);

        VisitFilterOptions visitFilterOptions =
                new VisitFilterOptions(null, null, null, null,
                        vehicleId, null, null, null,
                        null, null, null, null, null);

        List<Visit> visitsForVehicle = visitService.findAll(visitFilterOptions);

        model.addAttribute("vehicle", dto);
        model.addAttribute("owner", vehicle.getOwner().getId());
        model.addAttribute("vehicleVisits", visitsForVehicle);

        return "vehicle-single";
    }

    @PreAuthorize("hasRole('CLERK')")
    @GetMapping("/vehicles/new")
    public String showNewVehiclePage(Model model) {

        List<VehicleBrand> brands = vehicleBrandService.getAll();
        List<VehicleModel> models = vehicleModelService.getAll();
        List<VehicleYear> years = vehicleYearService.getAll();
        List<UserEntity> clients = userService.findAll();

        model.addAttribute("vehicle", new VehicleInDTO(null, null, null, null, null, null));
        model.addAttribute("brands", brands);
        model.addAttribute("models", models);
        model.addAttribute("years", years);
        model.addAttribute("clients", clients);
        return "vehicle-create";
    }

    @PreAuthorize("hasAnyRole('CLERK', 'MECHANIC')")
    @PostMapping("/vehicles/new")
    public String createVehicle(@Valid @ModelAttribute("vehicle") VehicleInDTO dto,
                                BindingResult bindingResult,
                                Model model,
                                @AuthenticationPrincipal CustomUserDetails principal) {

        UserEntity employee = userService.getById(principal.getId());

        if (bindingResult.hasErrors()) {
            List<VehicleBrand> brands = vehicleBrandService.getAll();
            List<VehicleModel> models = vehicleModelService.getAll();
            List<VehicleYear> years = vehicleYearService.getAll();
            List<UserEntity> clients = userService.findAll();

            model.addAttribute("vehicle", dto);
            model.addAttribute("brands", brands);
            model.addAttribute("models", models);
            model.addAttribute("years", years);
            model.addAttribute("clients", clients);
            return "vehicle-create";
        }

        try {
            Vehicle vehicle = vehicleMapper.toEntity(dto, vehicleBrandService, vehicleModelService, vehicleYearService, userService);
            vehicleService.create(vehicle, employee);
            return "redirect:/garage/vehicles?owner=" + vehicle.getOwner().getPhoneNumber();
        } catch (EntityDuplicateException e) {
            model.addAttribute("statusCode", HttpStatus.CONFLICT.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "error-page";
        }
    }

    @PreAuthorize("hasAnyRole('CLERK', 'MECHANIC')")
    @GetMapping("/vehicles/{vehicleId}/update")
    public String showUpdateVehiclePage(@PathVariable long vehicleId, Model model) {

        Vehicle vehicle = null;
        try {
            vehicle = vehicleService.getById(vehicleId);
        } catch (EntityNotFoundException e) {

            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "error-page";

        }
        VehicleInDTO dto = vehicleMapper.vehicleToVehicleDTO(vehicle);
        List<VehicleBrand> brands = vehicleBrandService.getAll();
        List<VehicleModel> models = vehicleModelService.getAll();
        List<VehicleYear> years = vehicleYearService.getAll();
        List<UserEntity> clients = userService.findAll();

        model.addAttribute("vehicle", dto);
        model.addAttribute("brands", brands);
        model.addAttribute("models", models);
        model.addAttribute("years", years);
        model.addAttribute("clients", clients);
        return "vehicle-create";
    }

    @PreAuthorize("hasAnyRole('CLERK', 'MECHANIC')")
    @PostMapping("/vehicles/{vehicleId}/update")
    public String updateVehicle(@PathVariable long vehicleId,
                                @Valid @ModelAttribute("vehicle") VehicleInDTO dto,
                                BindingResult bindingResult,
                                Model model,
                                @AuthenticationPrincipal CustomUserDetails principal) {

        UserEntity employee = userService.getById(principal.getId());

        if (bindingResult.hasErrors()) {
            List<VehicleBrand> brands = vehicleBrandService.getAll();
            List<VehicleModel> models = vehicleModelService.getAll();
            List<VehicleYear> years = vehicleYearService.getAll();
            List<UserEntity> clients = userService.findAll();

            model.addAttribute("vehicle", dto);
            model.addAttribute("brands", brands);
            model.addAttribute("models", models);
            model.addAttribute("years", years);
            model.addAttribute("clients", clients);
            return "vehicle-create";
        }

        try {
            Vehicle vehicle = vehicleMapper.toEntity(dto, vehicleBrandService, vehicleModelService, vehicleYearService, userService);
            vehicleService.update(vehicleId, vehicle, employee);
            return "redirect:/garage/vehicles?owner=" + vehicle.getOwner().getPhoneNumber();
        } catch (EntityDuplicateException e) {
            model.addAttribute("statusCode", HttpStatus.CONFLICT.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "error-page";
        } catch (EntityNotFoundException e) {
            model.addAttribute("statusCode", HttpStatus.NOT_FOUND.getReasonPhrase());
            model.addAttribute("error", e.getMessage());
            return "error-page";
        }
    }
}
