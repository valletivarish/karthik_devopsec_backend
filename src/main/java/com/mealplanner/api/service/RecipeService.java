package com.mealplanner.api.service;

import com.mealplanner.api.dto.RecipeIngredientRequest;
import com.mealplanner.api.dto.RecipeRequest;
import com.mealplanner.api.dto.RecipeResponse;
import com.mealplanner.api.exception.ResourceNotFoundException;
import com.mealplanner.api.model.Ingredient;
import com.mealplanner.api.model.Recipe;
import com.mealplanner.api.model.RecipeIngredient;
import com.mealplanner.api.model.User;
import com.mealplanner.api.model.enums.Difficulty;
import com.mealplanner.api.repository.IngredientRepository;
import com.mealplanner.api.repository.RecipeRepository;
import com.mealplanner.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Recipe CRUD operations with nutritional calculations.
 * Manages recipe ingredients, computes total nutrients per recipe,
 * and supports search/filtering functionality.
 */
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;

    /** Retrieves all recipes with computed nutritional totals */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves all recipes belonging to the authenticated user */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipesByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return recipeRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves recipes belonging to a specific user */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipesByUser(Long userId) {
        return recipeRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves a single recipe by ID with full nutritional details */
    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", id));
        return toResponse(recipe);
    }

    /** Searches recipes by title keyword */
    @Transactional(readOnly = true)
    public List<RecipeResponse> searchRecipes(String keyword) {
        return recipeRepository.searchByTitle(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Filters recipes by difficulty level */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipesByDifficulty(String difficulty) {
        return recipeRepository.findByDifficulty(Difficulty.valueOf(difficulty.toUpperCase())).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new recipe with its ingredient associations.
     * Each ingredient reference is validated against the database.
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Recipe recipe = Recipe.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .prepTime(request.getPrepTime())
                .cookTime(request.getCookTime())
                .servings(request.getServings())
                .difficulty(Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                .imageUrl(request.getImageUrl())
                .user(user)
                .recipeIngredients(new ArrayList<>())
                .build();

        /* Add ingredient associations with quantities if provided */
        if (request.getIngredients() != null) {
            for (RecipeIngredientRequest ingredientReq : request.getIngredients()) {
                Ingredient ingredient = ingredientRepository.findById(ingredientReq.getIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient", ingredientReq.getIngredientId()));

                RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                        .recipe(recipe)
                        .ingredient(ingredient)
                        .quantity(ingredientReq.getQuantity())
                        .unit(ingredientReq.getUnit())
                        .build();
                recipe.getRecipeIngredients().add(recipeIngredient);
            }
        }

        return toResponse(recipeRepository.save(recipe));
    }

    /**
     * Updates an existing recipe and replaces its ingredient associations.
     * Old ingredients are removed via orphanRemoval and new ones are added.
     */
    @Transactional
    public RecipeResponse updateRecipe(Long id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", id));

        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setInstructions(request.getInstructions());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());
        recipe.setServings(request.getServings());
        recipe.setDifficulty(Difficulty.valueOf(request.getDifficulty().toUpperCase()));
        recipe.setImageUrl(request.getImageUrl());

        /* Clear existing ingredients and add updated ones */
        recipe.getRecipeIngredients().clear();
        if (request.getIngredients() != null) {
            for (RecipeIngredientRequest ingredientReq : request.getIngredients()) {
                Ingredient ingredient = ingredientRepository.findById(ingredientReq.getIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient", ingredientReq.getIngredientId()));

                RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                        .recipe(recipe)
                        .ingredient(ingredient)
                        .quantity(ingredientReq.getQuantity())
                        .unit(ingredientReq.getUnit())
                        .build();
                recipe.getRecipeIngredients().add(recipeIngredient);
            }
        }

        return toResponse(recipeRepository.save(recipe));
    }

    /** Deletes a recipe and all its ingredient associations */
    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recipe", id);
        }
        recipeRepository.deleteById(id);
    }

    /** Converts Recipe entity to RecipeResponse DTO with nutritional calculations */
    private RecipeResponse toResponse(Recipe recipe) {
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        List<RecipeResponse.RecipeIngredientResponse> ingredientResponses = new ArrayList<>();

        /* Calculate nutritional totals from all recipe ingredients */
        for (RecipeIngredient ri : recipe.getRecipeIngredients()) {
            Ingredient ing = ri.getIngredient();
            /* Nutritional values are per 100g, scale by quantity used */
            double factor = ri.getQuantity() / 100.0;
            double cal = ing.getCalories() * factor;
            double pro = ing.getProtein() * factor;
            double car = ing.getCarbs() * factor;
            double fa = ing.getFat() * factor;

            totalCalories += cal;
            totalProtein += pro;
            totalCarbs += car;
            totalFat += fa;

            ingredientResponses.add(RecipeResponse.RecipeIngredientResponse.builder()
                    .id(ri.getId())
                    .ingredientId(ing.getId())
                    .ingredientName(ing.getName())
                    .quantity(ri.getQuantity())
                    .unit(ri.getUnit())
                    .calories(Math.round(cal * 100.0) / 100.0)
                    .protein(Math.round(pro * 100.0) / 100.0)
                    .carbs(Math.round(car * 100.0) / 100.0)
                    .fat(Math.round(fa * 100.0) / 100.0)
                    .build());
        }

        return RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .instructions(recipe.getInstructions())
                .prepTime(recipe.getPrepTime())
                .cookTime(recipe.getCookTime())
                .servings(recipe.getServings())
                .difficulty(recipe.getDifficulty().name())
                .imageUrl(recipe.getImageUrl())
                .authorName(recipe.getUser().getFullName())
                .userId(recipe.getUser().getId())
                .totalCalories(Math.round(totalCalories * 100.0) / 100.0)
                .totalProtein(Math.round(totalProtein * 100.0) / 100.0)
                .totalCarbs(Math.round(totalCarbs * 100.0) / 100.0)
                .totalFat(Math.round(totalFat * 100.0) / 100.0)
                .ingredients(ingredientResponses)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }
}
