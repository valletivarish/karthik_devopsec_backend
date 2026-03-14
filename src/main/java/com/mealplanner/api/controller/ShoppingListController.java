package com.mealplanner.api.controller;

import com.mealplanner.api.dto.ShoppingListRequest;
import com.mealplanner.api.dto.ShoppingListResponse;
import com.mealplanner.api.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for ShoppingList CRUD operations.
 * Supports manual creation and auto-generation from meal plans.
 * Includes item check/uncheck for shopping progress tracking.
 */
@RestController
@RequestMapping("/api/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    /** GET /api/shopping-lists - Retrieve all shopping lists for the authenticated user */
    @GetMapping
    public ResponseEntity<List<ShoppingListResponse>> getAllShoppingLists(Authentication authentication) {
        return ResponseEntity.ok(shoppingListService.getShoppingListsByUsername(authentication.getName()));
    }

    /** GET /api/shopping-lists/{id} - Retrieve a specific shopping list */
    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> getShoppingListById(@PathVariable Long id) {
        return ResponseEntity.ok(shoppingListService.getShoppingListById(id));
    }

    /**
     * POST /api/shopping-lists - Create a shopping list.
     * If mealPlanId is provided, auto-generates items from the meal plan.
     */
    @PostMapping
    public ResponseEntity<ShoppingListResponse> createShoppingList(
            @Valid @RequestBody ShoppingListRequest request,
            Authentication authentication) {
        ShoppingListResponse response = shoppingListService.createShoppingList(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/shopping-lists/{id} - Update a shopping list */
    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> updateShoppingList(
            @PathVariable Long id,
            @Valid @RequestBody ShoppingListRequest request) {
        return ResponseEntity.ok(shoppingListService.updateShoppingList(id, request));
    }

    /** PATCH /api/shopping-lists/items/{itemId}/toggle - Toggle item checked status */
    @PatchMapping("/items/{itemId}/toggle")
    public ResponseEntity<Void> toggleItemChecked(@PathVariable Long itemId) {
        shoppingListService.toggleItemChecked(itemId);
        return ResponseEntity.ok().build();
    }

    /** DELETE /api/shopping-lists/{id} - Delete a shopping list */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShoppingList(@PathVariable Long id) {
        shoppingListService.deleteShoppingList(id);
        return ResponseEntity.noContent().build();
    }
}
