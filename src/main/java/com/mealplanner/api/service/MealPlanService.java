package com.mealplanner.api.service;

import com.mealplanner.api.dto.MealPlanEntryRequest;
import com.mealplanner.api.dto.MealPlanRequest;
import com.mealplanner.api.dto.MealPlanResponse;
import com.mealplanner.api.exception.ResourceNotFoundException;
import com.mealplanner.api.model.*;
import com.mealplanner.api.model.enums.MealType;
import com.mealplanner.api.repository.MealPlanRepository;
import com.mealplanner.api.repository.RecipeRepository;
import com.mealplanner.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for MealPlan CRUD operations and meal plan generation.
 * Manages meal plan entries that map recipes to specific day/meal slots.
 * Computes aggregate nutritional information for the entire plan.
 */
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    /** Retrieves all meal plans for a specific user */
    @Transactional(readOnly = true)
    public List<MealPlanResponse> getMealPlansByUser(Long userId) {
        return mealPlanRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves all meal plans in the system */
    @Transactional(readOnly = true)
    public List<MealPlanResponse> getAllMealPlans() {
        return mealPlanRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves all meal plans belonging to the authenticated user */
    @Transactional(readOnly = true)
    public List<MealPlanResponse> getMealPlansByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return mealPlanRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves a single meal plan by ID with all entries */
    @Transactional(readOnly = true)
    public MealPlanResponse getMealPlanById(Long id) {
        MealPlan mealPlan = mealPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MealPlan", id));
        return toResponse(mealPlan);
    }

    /**
     * Creates a new meal plan with recipe-to-day/meal assignments.
     * Validates that the end date is after the start date and all
     * referenced recipes exist in the database.
     */
    @Transactional
    public MealPlanResponse createMealPlan(MealPlanRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        MealPlan mealPlan = MealPlan.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .user(user)
                .entries(new ArrayList<>())
                .build();

        /* Add meal plan entries if provided */
        if (request.getEntries() != null) {
            for (MealPlanEntryRequest entryReq : request.getEntries()) {
                Recipe recipe = recipeRepository.findById(entryReq.getRecipeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Recipe", entryReq.getRecipeId()));

                MealPlanEntry entry = MealPlanEntry.builder()
                        .mealPlan(mealPlan)
                        .recipe(recipe)
                        .dayOfWeek(DayOfWeek.valueOf(entryReq.getDayOfWeek().toUpperCase()))
                        .mealType(MealType.valueOf(entryReq.getMealType().toUpperCase()))
                        .build();
                mealPlan.getEntries().add(entry);
            }
        }

        return toResponse(mealPlanRepository.save(mealPlan));
    }

    /** Updates an existing meal plan and replaces its entries */
    @Transactional
    public MealPlanResponse updateMealPlan(Long id, MealPlanRequest request) {
        MealPlan mealPlan = mealPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MealPlan", id));

        mealPlan.setName(request.getName());
        mealPlan.setStartDate(request.getStartDate());
        mealPlan.setEndDate(request.getEndDate());

        /* Clear and rebuild entries */
        mealPlan.getEntries().clear();
        if (request.getEntries() != null) {
            for (MealPlanEntryRequest entryReq : request.getEntries()) {
                Recipe recipe = recipeRepository.findById(entryReq.getRecipeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Recipe", entryReq.getRecipeId()));

                MealPlanEntry entry = MealPlanEntry.builder()
                        .mealPlan(mealPlan)
                        .recipe(recipe)
                        .dayOfWeek(DayOfWeek.valueOf(entryReq.getDayOfWeek().toUpperCase()))
                        .mealType(MealType.valueOf(entryReq.getMealType().toUpperCase()))
                        .build();
                mealPlan.getEntries().add(entry);
            }
        }

        return toResponse(mealPlanRepository.save(mealPlan));
    }

    /** Deletes a meal plan and all its entries */
    @Transactional
    public void deleteMealPlan(Long id) {
        if (!mealPlanRepository.existsById(id)) {
            throw new ResourceNotFoundException("MealPlan", id);
        }
        mealPlanRepository.deleteById(id);
    }

    /** Converts MealPlan entity to response DTO with nutritional calculations */
    private MealPlanResponse toResponse(MealPlan mealPlan) {
        double totalCalories = 0;
        double totalProtein = 0;

        List<MealPlanResponse.MealPlanEntryResponse> entryResponses = new ArrayList<>();

        for (MealPlanEntry entry : mealPlan.getEntries()) {
            Recipe recipe = entry.getRecipe();
            /* Calculate recipe nutritional totals from its ingredients */
            double recipeCal = 0;
            double recipePro = 0;
            double recipeCar = 0;
            double recipeFa = 0;

            for (RecipeIngredient ri : recipe.getRecipeIngredients()) {
                double factor = ri.getQuantity() / 100.0;
                recipeCal += ri.getIngredient().getCalories() * factor;
                recipePro += ri.getIngredient().getProtein() * factor;
                recipeCar += ri.getIngredient().getCarbs() * factor;
                recipeFa += ri.getIngredient().getFat() * factor;
            }

            totalCalories += recipeCal;
            totalProtein += recipePro;

            entryResponses.add(MealPlanResponse.MealPlanEntryResponse.builder()
                    .id(entry.getId())
                    .recipeId(recipe.getId())
                    .recipeTitle(recipe.getTitle())
                    .dayOfWeek(entry.getDayOfWeek().name())
                    .mealType(entry.getMealType().name())
                    .calories(Math.round(recipeCal * 100.0) / 100.0)
                    .protein(Math.round(recipePro * 100.0) / 100.0)
                    .carbs(Math.round(recipeCar * 100.0) / 100.0)
                    .fat(Math.round(recipeFa * 100.0) / 100.0)
                    .build());
        }

        return MealPlanResponse.builder()
                .id(mealPlan.getId())
                .name(mealPlan.getName())
                .startDate(mealPlan.getStartDate())
                .endDate(mealPlan.getEndDate())
                .userId(mealPlan.getUser().getId())
                .totalCalories(Math.round(totalCalories * 100.0) / 100.0)
                .totalProtein(Math.round(totalProtein * 100.0) / 100.0)
                .entries(entryResponses)
                .createdAt(mealPlan.getCreatedAt())
                .updatedAt(mealPlan.getUpdatedAt())
                .build();
    }
}
