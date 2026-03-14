package com.mealplanner.api.controller;

import com.mealplanner.api.dto.RecipeRequest;
import com.mealplanner.api.dto.RecipeResponse;
import com.mealplanner.api.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Recipe CRUD operations and search/filter endpoints.
 * All endpoints require JWT authentication.
 * Supports search by keyword, filter by difficulty, and user-specific listing.
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /** GET /api/recipes - Retrieve all recipes for the authenticated user */
    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes(Authentication authentication) {
        return ResponseEntity.ok(recipeService.getRecipesByUsername(authentication.getName()));
    }

    /** GET /api/recipes/{id} - Retrieve a specific recipe by ID */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    /** GET /api/recipes/search?keyword=... - Search recipes by title */
    @GetMapping("/search")
    public ResponseEntity<List<RecipeResponse>> searchRecipes(@RequestParam String keyword) {
        return ResponseEntity.ok(recipeService.searchRecipes(keyword));
    }

    /** GET /api/recipes/difficulty/{difficulty} - Filter recipes by difficulty level */
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<RecipeResponse>> getByDifficulty(@PathVariable String difficulty) {
        return ResponseEntity.ok(recipeService.getRecipesByDifficulty(difficulty));
    }

    /** GET /api/recipes/user/{userId} - Get all recipes by a specific user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getRecipesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(recipeService.getRecipesByUser(userId));
    }

    /**
     * POST /api/recipes - Create a new recipe.
     * Associates the recipe with the currently authenticated user.
     */
    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(
            @Valid @RequestBody RecipeRequest request,
            Authentication authentication) {
        RecipeResponse response = recipeService.createRecipe(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/recipes/{id} - Update an existing recipe */
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
    }

    /** DELETE /api/recipes/{id} - Delete a recipe by ID */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }
}
