package com.mealplanner.api.service;

import com.mealplanner.api.dto.ShoppingListRequest;
import com.mealplanner.api.dto.ShoppingListResponse;
import com.mealplanner.api.exception.ResourceNotFoundException;
import com.mealplanner.api.model.*;
import com.mealplanner.api.repository.MealPlanRepository;
import com.mealplanner.api.repository.ShoppingListRepository;
import com.mealplanner.api.repository.ShoppingListItemRepository;
import com.mealplanner.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for ShoppingList CRUD operations and auto-generation.
 * Can generate shopping lists from meal plans by aggregating all
 * ingredient quantities across all recipes in the plan.
 */
@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;

    /** Retrieves all shopping lists for a specific user */
    @Transactional(readOnly = true)
    public List<ShoppingListResponse> getShoppingListsByUser(Long userId) {
        return shoppingListRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves all shopping lists */
    @Transactional(readOnly = true)
    public List<ShoppingListResponse> getAllShoppingLists() {
        return shoppingListRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves all shopping lists belonging to the authenticated user */
    @Transactional(readOnly = true)
    public List<ShoppingListResponse> getShoppingListsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return shoppingListRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retrieves a single shopping list by ID */
    @Transactional(readOnly = true)
    public ShoppingListResponse getShoppingListById(Long id) {
        ShoppingList list = shoppingListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShoppingList", id));
        return toResponse(list);
    }

    /**
     * Creates a shopping list. If mealPlanId is provided, auto-generates
     * items by aggregating all ingredient quantities from the meal plan's recipes.
     * Duplicate ingredients are merged with combined quantities.
     */
    @Transactional
    public ShoppingListResponse createShoppingList(ShoppingListRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        ShoppingList shoppingList = ShoppingList.builder()
                .name(request.getName())
                .user(user)
                .items(new ArrayList<>())
                .build();

        /* Auto-generate items from meal plan if ID is provided */
        if (request.getMealPlanId() != null) {
            MealPlan mealPlan = mealPlanRepository.findById(request.getMealPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("MealPlan", request.getMealPlanId()));
            shoppingList.setMealPlan(mealPlan);

            /* Aggregate ingredients across all recipes in the meal plan */
            Map<Long, ShoppingListItem> aggregatedItems = new HashMap<>();
            for (MealPlanEntry entry : mealPlan.getEntries()) {
                for (RecipeIngredient ri : entry.getRecipe().getRecipeIngredients()) {
                    Long ingredientId = ri.getIngredient().getId();
                    if (aggregatedItems.containsKey(ingredientId)) {
                        /* Merge quantities for duplicate ingredients */
                        ShoppingListItem existing = aggregatedItems.get(ingredientId);
                        existing.setQuantity(existing.getQuantity() + ri.getQuantity());
                    } else {
                        ShoppingListItem item = ShoppingListItem.builder()
                                .shoppingList(shoppingList)
                                .ingredient(ri.getIngredient())
                                .quantity(ri.getQuantity())
                                .unit(ri.getUnit())
                                .checked(false)
                                .build();
                        aggregatedItems.put(ingredientId, item);
                    }
                }
            }
            shoppingList.setItems(new ArrayList<>(aggregatedItems.values()));
        }

        return toResponse(shoppingListRepository.save(shoppingList));
    }

    /** Updates a shopping list's name */
    @Transactional
    public ShoppingListResponse updateShoppingList(Long id, ShoppingListRequest request) {
        ShoppingList list = shoppingListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShoppingList", id));
        list.setName(request.getName());
        return toResponse(shoppingListRepository.save(list));
    }

    /** Toggles the checked status of a shopping list item */
    @Transactional
    public void toggleItemChecked(Long itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ShoppingListItem not found with id: " + itemId));
        item.setChecked(!item.getChecked());
        shoppingListItemRepository.save(item);
    }

    /** Deletes a shopping list and all its items */
    @Transactional
    public void deleteShoppingList(Long id) {
        if (!shoppingListRepository.existsById(id)) {
            throw new ResourceNotFoundException("ShoppingList", id);
        }
        shoppingListRepository.deleteById(id);
    }

    /** Converts entity to response DTO */
    private ShoppingListResponse toResponse(ShoppingList list) {
        List<ShoppingListResponse.ShoppingListItemResponse> itemResponses = list.getItems().stream()
                .map(item -> ShoppingListResponse.ShoppingListItemResponse.builder()
                        .id(item.getId())
                        .ingredientId(item.getIngredient().getId())
                        .ingredientName(item.getIngredient().getName())
                        .quantity(item.getQuantity())
                        .unit(item.getUnit())
                        .checked(item.getChecked())
                        .build())
                .collect(Collectors.toList());

        return ShoppingListResponse.builder()
                .id(list.getId())
                .name(list.getName())
                .mealPlanId(list.getMealPlan() != null ? list.getMealPlan().getId() : null)
                .mealPlanName(list.getMealPlan() != null ? list.getMealPlan().getName() : null)
                .userId(list.getUser().getId())
                .items(itemResponses)
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }
}
