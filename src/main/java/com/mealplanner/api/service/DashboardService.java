package com.mealplanner.api.service;

import com.mealplanner.api.dto.DashboardResponse;
import com.mealplanner.api.model.*;
import com.mealplanner.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for generating dashboard summary statistics and nutritional analytics.
 * Aggregates data from recipes, meal plans, and ingredients to provide
 * overview cards and chart data for the frontend dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RecipeRepository recipeRepository;
    private final MealPlanRepository mealPlanRepository;
    private final IngredientRepository ingredientRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final UserRepository userRepository;

    /**
     * Generates comprehensive dashboard data for a user including
     * entity counts, calorie breakdown by meal type, macro distribution,
     * and daily nutritional summaries from the most recent meal plan.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long totalRecipes = recipeRepository.countByUserId(user.getId());
        long totalMealPlans = mealPlanRepository.countByUserId(user.getId());
        long totalIngredients = ingredientRepository.count();
        long totalShoppingLists = shoppingListRepository.findByUserId(user.getId()).size();

        /* Pick the current active plan (today in range), else fall back to latest */
        List<MealPlan> userPlans = mealPlanRepository.findByUserId(user.getId());
        Map<String, Double> caloriesByMealType = new LinkedHashMap<>();
        Map<String, Double> macroDistribution = new LinkedHashMap<>();
        List<DashboardResponse.DailyNutrition> dailyNutrition = new ArrayList<>();

        if (!userPlans.isEmpty()) {
            java.time.LocalDate today = java.time.LocalDate.now();
            MealPlan latestPlan = userPlans.stream()
                    .filter(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()))
                    .findFirst()
                    .orElse(userPlans.get(userPlans.size() - 1));
            double totalCalories = 0;
            double totalProtein = 0;
            double totalCarbs = 0;
            double totalFat = 0;

            /* Calculate calories by meal type for pie chart */
            Map<String, Double> mealTypeCals = new LinkedHashMap<>();
            Map<String, double[]> dailyTotals = new LinkedHashMap<>();

            for (MealPlanEntry entry : latestPlan.getEntries()) {
                double recipeCal = 0;
                double recipePro = 0;
                double recipeCar = 0;
                double recipeFa = 0;

                for (RecipeIngredient ri : entry.getRecipe().getRecipeIngredients()) {
                    double factor = ri.getQuantity() / 100.0;
                    recipeCal += ri.getIngredient().getCalories() * factor;
                    recipePro += ri.getIngredient().getProtein() * factor;
                    recipeCar += ri.getIngredient().getCarbs() * factor;
                    recipeFa += ri.getIngredient().getFat() * factor;
                }

                totalCalories += recipeCal;
                totalProtein += recipePro;
                totalCarbs += recipeCar;
                totalFat += recipeFa;

                /* Aggregate by meal type */
                String mealType = entry.getMealType().name();
                mealTypeCals.merge(mealType, recipeCal, Double::sum);

                /* Aggregate by day of week */
                String day = entry.getDayOfWeek().name();
                dailyTotals.computeIfAbsent(day, k -> new double[4]);
                dailyTotals.get(day)[0] += recipeCal;
                dailyTotals.get(day)[1] += recipePro;
                dailyTotals.get(day)[2] += recipeCar;
                dailyTotals.get(day)[3] += recipeFa;
            }

            caloriesByMealType = mealTypeCals;

            /* Calculate macro distribution as percentages */
            double totalMacros = totalProtein + totalCarbs + totalFat;
            if (totalMacros > 0) {
                macroDistribution.put("Protein", Math.round(totalProtein / totalMacros * 100.0 * 10.0) / 10.0);
                macroDistribution.put("Carbs", Math.round(totalCarbs / totalMacros * 100.0 * 10.0) / 10.0);
                macroDistribution.put("Fat", Math.round(totalFat / totalMacros * 100.0 * 10.0) / 10.0);
            }

            /* Build daily nutrition list for bar chart */
            for (Map.Entry<String, double[]> dayEntry : dailyTotals.entrySet()) {
                double[] vals = dayEntry.getValue();
                dailyNutrition.add(DashboardResponse.DailyNutrition.builder()
                        .day(dayEntry.getKey())
                        .calories(Math.round(vals[0] * 100.0) / 100.0)
                        .protein(Math.round(vals[1] * 100.0) / 100.0)
                        .carbs(Math.round(vals[2] * 100.0) / 100.0)
                        .fat(Math.round(vals[3] * 100.0) / 100.0)
                        .build());
            }
        }

        return DashboardResponse.builder()
                .totalRecipes(totalRecipes)
                .totalMealPlans(totalMealPlans)
                .totalIngredients(totalIngredients)
                .totalShoppingLists(totalShoppingLists)
                .caloriesByMealType(caloriesByMealType)
                .macroDistribution(macroDistribution)
                .dailyNutrition(dailyNutrition)
                .build();
    }
}
