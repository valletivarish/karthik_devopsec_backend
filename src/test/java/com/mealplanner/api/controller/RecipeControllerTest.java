package com.mealplanner.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealplanner.api.dto.RecipeRequest;
import com.mealplanner.api.dto.RecipeResponse;
import com.mealplanner.api.exception.ResourceNotFoundException;
import com.mealplanner.api.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RecipeController endpoints.
 * Uses SpringBootTest with MockBean to replace real service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecipeService recipeService;

    private RecipeResponse recipeResponse;
    private RecipeRequest recipeRequest;

    @BeforeEach
    void setUp() {
        recipeResponse = RecipeResponse.builder()
                .id(1L)
                .title("Chicken Salad")
                .description("Healthy chicken salad")
                .instructions("Mix everything together")
                .prepTime(10)
                .cookTime(15)
                .servings(2)
                .difficulty("EASY")
                .authorName("testuser")
                .userId(1L)
                .totalCalories(350.0)
                .totalProtein(40.0)
                .totalCarbs(10.0)
                .totalFat(12.0)
                .ingredients(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        recipeRequest = new RecipeRequest(
                "Chicken Salad", "Healthy chicken salad",
                "Mix everything together", 10, 15, 2, "EASY", null, null
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllRecipes_returnsListOfRecipes() throws Exception {
        when(recipeService.getRecipesByUsername("testuser")).thenReturn(Arrays.asList(recipeResponse));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Chicken Salad"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRecipeById_whenExists_returnsRecipe() throws Exception {
        when(recipeService.getRecipeById(1L)).thenReturn(recipeResponse);

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Chicken Salad"))
                .andExpect(jsonPath("$.totalCalories").value(350.0));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRecipeById_whenNotFound_returnsNotFound() throws Exception {
        when(recipeService.getRecipeById(99L))
                .thenThrow(new ResourceNotFoundException("Recipe", 99L));

        mockMvc.perform(get("/api/recipes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchRecipes_returnsMatchingRecipes() throws Exception {
        when(recipeService.searchRecipes("chicken")).thenReturn(Arrays.asList(recipeResponse));

        mockMvc.perform(get("/api/recipes/search").param("keyword", "chicken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chicken Salad"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getByDifficulty_returnsMatchingRecipes() throws Exception {
        when(recipeService.getRecipesByDifficulty("EASY")).thenReturn(Arrays.asList(recipeResponse));

        mockMvc.perform(get("/api/recipes/difficulty/EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("EASY"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createRecipe_withValidRequest_returnsCreated() throws Exception {
        when(recipeService.createRecipe(any(RecipeRequest.class), eq("testuser")))
                .thenReturn(recipeResponse);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recipeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Chicken Salad"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createRecipe_withBlankTitle_returnsBadRequest() throws Exception {
        RecipeRequest invalid = new RecipeRequest("", "desc", "instructions", 10, 15, 2, "EASY", null, null);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateRecipe_withValidRequest_returnsOk() throws Exception {
        when(recipeService.updateRecipe(eq(1L), any(RecipeRequest.class))).thenReturn(recipeResponse);

        mockMvc.perform(put("/api/recipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recipeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Chicken Salad"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateRecipe_whenNotFound_returnsNotFound() throws Exception {
        when(recipeService.updateRecipe(eq(99L), any(RecipeRequest.class)))
                .thenThrow(new ResourceNotFoundException("Recipe", 99L));

        mockMvc.perform(put("/api/recipes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recipeRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteRecipe_whenExists_returnsNoContent() throws Exception {
        doNothing().when(recipeService).deleteRecipe(1L);

        mockMvc.perform(delete("/api/recipes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteRecipe_whenNotFound_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Recipe", 99L))
                .when(recipeService).deleteRecipe(99L);

        mockMvc.perform(delete("/api/recipes/99"))
                .andExpect(status().isNotFound());
    }
}
