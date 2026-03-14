package com.mealplanner.api.controller;

import com.mealplanner.api.dto.MealPlanRequest;
import com.mealplanner.api.dto.MealPlanResponse;
import com.mealplanner.api.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for MealPlan CRUD operations.
 * Manages weekly meal plans with recipe-to-day/meal assignments.
 * Each meal plan is associated with the authenticated user.
 */
@RestController
@RequestMapping("/api/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /** GET /api/meal-plans - Retrieve all meal plans for the authenticated user */
    @GetMapping
    public ResponseEntity<List<MealPlanResponse>> getAllMealPlans(Authentication authentication) {
        return ResponseEntity.ok(mealPlanService.getMealPlansByUsername(authentication.getName()));
    }

    /** GET /api/meal-plans/{id} - Retrieve a specific meal plan with entries */
    @GetMapping("/{id}")
    public ResponseEntity<MealPlanResponse> getMealPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(mealPlanService.getMealPlanById(id));
    }

    /** GET /api/meal-plans/user/{userId} - Get meal plans for a specific user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MealPlanResponse>> getMealPlansByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(mealPlanService.getMealPlansByUser(userId));
    }

    /**
     * POST /api/meal-plans - Create a new meal plan with entries.
     * Associates with the currently authenticated user.
     */
    @PostMapping
    public ResponseEntity<MealPlanResponse> createMealPlan(
            @Valid @RequestBody MealPlanRequest request,
            Authentication authentication) {
        MealPlanResponse response = mealPlanService.createMealPlan(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/meal-plans/{id} - Update an existing meal plan */
    @PutMapping("/{id}")
    public ResponseEntity<MealPlanResponse> updateMealPlan(
            @PathVariable Long id,
            @Valid @RequestBody MealPlanRequest request) {
        return ResponseEntity.ok(mealPlanService.updateMealPlan(id, request));
    }

    /** DELETE /api/meal-plans/{id} - Delete a meal plan and all its entries */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMealPlan(@PathVariable Long id) {
        mealPlanService.deleteMealPlan(id);
        return ResponseEntity.noContent().build();
    }
}
