package com.mealplanner.api.config;

import com.mealplanner.api.model.*;
import com.mealplanner.api.model.enums.Difficulty;
import com.mealplanner.api.model.enums.MealType;
import com.mealplanner.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the database with demo data on first startup.
 * Only runs if no users exist, ensuring idempotent initialization.
 * Creates a demo user, ingredients, recipes, a meal plan, and a shopping list.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already exists — skipping seed.");
            return;
        }

        log.info("Seeding demo data...");

        User demo = seedUser();
        User alice = seedAlice();
        User bob = seedBob();
        List<Ingredient> ingredients = seedIngredients();
        List<Recipe> recipes = seedRecipes(demo, ingredients);
        seedMealPlan(demo, recipes);
        seedShoppingList(demo, ingredients);
        seedAliceData(alice, ingredients, recipes);
        seedBobData(bob, ingredients, recipes);

        log.info("Demo data seeded successfully. Logins: demo/demo1234 | alice/alice1234 | bob/bob1234");
    }

    // ─── Users ───────────────────────────────────────────────────────────────

    private User seedUser() {
        return userRepository.save(User.builder()
                .username("demo")
                .email("demo@mealplanner.com")
                .password(passwordEncoder.encode("demo1234"))
                .fullName("Demo User")
                .build());
    }

    private User seedAlice() {
        return userRepository.save(User.builder()
                .username("alice")
                .email("alice@mealplanner.com")
                .password(passwordEncoder.encode("alice1234"))
                .fullName("Alice Johnson")
                .build());
    }

    private User seedBob() {
        return userRepository.save(User.builder()
                .username("bob")
                .email("bob@mealplanner.com")
                .password(passwordEncoder.encode("bob1234"))
                .fullName("Bob Smith")
                .build());
    }

    // ─── Ingredients ─────────────────────────────────────────────────────────

    private List<Ingredient> seedIngredients() {
        List<Ingredient> items = List.of(
            ingredient("Chicken Breast",    165.0, 31.0,  0.0,  3.6, 0.0,   9.0,  0.0,  15.0, 0.9, "per 100g"),
            ingredient("Brown Rice",        216.0,  5.0, 45.0,  1.8, 3.5,   0.0,  0.0,  23.0, 1.5, "per 100g"),
            ingredient("Broccoli",           34.0,  2.8,  7.0,  0.4, 2.6,  31.0, 89.2,  47.0, 0.7, "per 100g"),
            ingredient("Eggs",             155.0, 13.0,  1.1, 11.0, 0.0, 149.0,  0.0,  56.0, 1.8, "per 100g"),
            ingredient("Whole Milk",         61.0,  3.2,  4.8,  3.3, 0.0,  46.0,  0.0, 113.0, 0.0, "per 100g"),
            ingredient("Cheddar Cheese",    403.0, 25.0,  1.3, 33.0, 0.0, 265.0,  0.0, 721.0, 0.7, "per 100g"),
            ingredient("Spaghetti",         371.0, 13.0, 74.0,  1.8, 3.2,   0.0,  0.0,  21.0, 3.3, "per 100g"),
            ingredient("Olive Oil",         884.0,  0.0,  0.0,100.0, 0.0,   0.0,  0.0,   1.0, 0.6, "per 100g"),
            ingredient("Garlic",            149.0,  6.4, 33.0,  0.5, 2.1,   0.0, 31.2,  18.0, 1.7, "per 100g"),
            ingredient("Onion",              40.0,  1.1,  9.3,  0.1, 1.7,   0.0,  7.4,  23.0, 0.2, "per 100g"),
            ingredient("Tomato",             18.0,  0.9,  3.9,  0.2, 1.2,  42.0, 13.7,  10.0, 0.3, "per 100g"),
            ingredient("Spinach",            23.0,  2.9,  3.6,  0.4, 2.2, 469.0, 28.1,  99.0, 2.7, "per 100g"),
            ingredient("Greek Yogurt",       59.0, 10.0,  3.6,  0.4, 0.0,   0.0,  0.0, 110.0, 0.1, "per 100g"),
            ingredient("Oats",             389.0, 17.0, 66.0,  7.0, 10.6,  0.0,  0.0,  54.0, 4.7, "per 100g"),
            ingredient("Banana",             89.0,  1.1, 23.0,  0.3, 2.6,   3.0,  8.7,   5.0, 0.3, "per 100g"),
            ingredient("Salmon",            208.0, 20.0,  0.0, 13.0, 0.0,  50.0,  3.9,  12.0, 0.8, "per 100g"),
            ingredient("Sweet Potato",       86.0,  1.6, 20.0,  0.1, 3.0, 961.0, 19.6,  30.0, 0.6, "per 100g"),
            ingredient("Black Beans",       132.0,  8.9, 24.0,  0.5, 8.7,   0.0,  0.0,  46.0, 2.1, "per 100g"),
            ingredient("Avocado",           160.0,  2.0,  9.0, 15.0, 7.0,   7.0, 10.0,  12.0, 0.6, "per 100g"),
            ingredient("Lemon",              29.0,  1.1,  9.3,  0.3, 2.8,   2.0, 53.0,  26.0, 0.6, "per 100g")
        );
        return ingredientRepository.saveAll(items);
    }

    private Ingredient ingredient(String name, double cal, double protein, double carbs,
                                   double fat, double fiber, double vitA, double vitC,
                                   double calcium, double iron, String unit) {
        return Ingredient.builder()
                .name(name).calories(cal).protein(protein).carbs(carbs).fat(fat)
                .fiber(fiber).vitaminA(vitA).vitaminC(vitC).calcium(calcium)
                .iron(iron).unit(unit).build();
    }

    // ─── Recipes ─────────────────────────────────────────────────────────────

    private List<Recipe> seedRecipes(User user, List<Ingredient> ing) {
        // Index references into the ingredients list (0-based)
        // 0=Chicken, 1=Rice, 2=Broccoli, 3=Eggs, 4=Milk, 5=Cheese,
        // 6=Spaghetti, 7=Olive Oil, 8=Garlic, 9=Onion, 10=Tomato,
        // 11=Spinach, 12=Greek Yogurt, 13=Oats, 14=Banana,
        // 15=Salmon, 16=Sweet Potato, 17=Black Beans, 18=Avocado, 19=Lemon

        Recipe r1 = recipe(user, "Grilled Chicken with Brown Rice",
                "A high-protein balanced meal perfect for lunch or dinner.",
                "1. Season chicken breast with salt, pepper and garlic powder.\n2. Grill on medium heat for 6-7 minutes per side until cooked through.\n3. Cook brown rice according to package instructions.\n4. Steam broccoli for 4-5 minutes until tender-crisp.\n5. Serve chicken over rice with broccoli on the side.",
                10, 25, 2, Difficulty.EASY);

        Recipe r2 = recipe(user, "Spaghetti Aglio e Olio",
                "Classic Italian pasta with garlic and olive oil — simple and delicious.",
                "1. Cook spaghetti in salted boiling water until al dente.\n2. Slice garlic thinly and sauté in olive oil until golden.\n3. Add chilli flakes and a ladle of pasta water to the oil.\n4. Toss drained pasta in the garlic oil.\n5. Garnish with parsley and serve immediately.",
                5, 15, 2, Difficulty.EASY);

        Recipe r3 = recipe(user, "Salmon with Sweet Potato",
                "Omega-3 rich salmon with roasted sweet potato — a nutritional powerhouse.",
                "1. Preheat oven to 200°C.\n2. Cube sweet potato, toss in olive oil, roast 20 minutes.\n3. Season salmon with lemon juice, salt and pepper.\n4. Pan-fry salmon skin-side down for 4 minutes, flip and cook 3 more minutes.\n5. Serve salmon over sweet potato with lemon wedges.",
                10, 25, 2, Difficulty.MEDIUM);

        Recipe r4 = recipe(user, "Spinach & Egg Scramble",
                "Quick high-protein breakfast ready in under 10 minutes.",
                "1. Heat olive oil in a pan over medium heat.\n2. Sauté onion until translucent, about 3 minutes.\n3. Add spinach and cook until wilted.\n4. Crack in eggs and scramble gently until just set.\n5. Season with salt and pepper. Serve immediately.",
                5, 8, 2, Difficulty.EASY);

        Recipe r5 = recipe(user, "Overnight Oats with Banana",
                "Prep the night before for a nutritious ready-to-eat breakfast.",
                "1. Combine oats, milk and Greek yogurt in a jar.\n2. Stir well and refrigerate overnight.\n3. In the morning, slice banana and place on top.\n4. Add honey or nuts if desired and enjoy cold.",
                5, 0, 1, Difficulty.EASY);

        Recipe r6 = recipe(user, "Black Bean & Avocado Bowl",
                "A fibre-packed vegan bowl with creamy avocado.",
                "1. Cook brown rice and allow to cool slightly.\n2. Drain and rinse black beans, warm in a pan.\n3. Dice tomato, slice avocado.\n4. Assemble bowl: rice base, beans, tomato, avocado.\n5. Drizzle with lemon juice, season and serve.",
                15, 5, 2, Difficulty.EASY);

        Recipe r7 = recipe(user, "Chicken & Tomato Stir Fry",
                "Fast weeknight dinner with bold flavours.",
                "1. Slice chicken breast into strips.\n2. Heat olive oil in a wok over high heat.\n3. Stir fry chicken until golden, about 5 minutes.\n4. Add garlic and onion, cook 2 minutes.\n5. Add tomatoes, season and toss. Serve with rice.",
                10, 15, 3, Difficulty.MEDIUM);

        Recipe r8 = recipe(user, "Cheesy Scrambled Eggs",
                "Creamy, rich scrambled eggs with melted cheddar.",
                "1. Whisk eggs with a splash of milk.\n2. Heat butter in non-stick pan over low heat.\n3. Pour in eggs and stir slowly and continuously.\n4. When eggs are just set, fold in cheddar cheese.\n5. Remove from heat immediately and serve on toast.",
                3, 8, 2, Difficulty.EASY);

        List<Recipe> recipes = recipeRepository.saveAll(List.of(r1, r2, r3, r4, r5, r6, r7, r8));

        // Attach ingredients
        recipeIngredientRepository.saveAll(List.of(
            ri(recipes.get(0), ing.get(0),  200.0, "grams"),
            ri(recipes.get(0), ing.get(1),  150.0, "grams"),
            ri(recipes.get(0), ing.get(2),  100.0, "grams"),
            ri(recipes.get(0), ing.get(8),    5.0, "grams"),

            ri(recipes.get(1), ing.get(6),  180.0, "grams"),
            ri(recipes.get(1), ing.get(7),   30.0, "ml"),
            ri(recipes.get(1), ing.get(8),   10.0, "grams"),

            ri(recipes.get(2), ing.get(15), 180.0, "grams"),
            ri(recipes.get(2), ing.get(16), 200.0, "grams"),
            ri(recipes.get(2), ing.get(7),   15.0, "ml"),
            ri(recipes.get(2), ing.get(19),  20.0, "grams"),

            ri(recipes.get(3), ing.get(3),  150.0, "grams"),
            ri(recipes.get(3), ing.get(11), 100.0, "grams"),
            ri(recipes.get(3), ing.get(9),   50.0, "grams"),
            ri(recipes.get(3), ing.get(7),   10.0, "ml"),

            ri(recipes.get(4), ing.get(13),  80.0, "grams"),
            ri(recipes.get(4), ing.get(4),  150.0, "ml"),
            ri(recipes.get(4), ing.get(12), 100.0, "grams"),
            ri(recipes.get(4), ing.get(14), 100.0, "grams"),

            ri(recipes.get(5), ing.get(1),  150.0, "grams"),
            ri(recipes.get(5), ing.get(17), 120.0, "grams"),
            ri(recipes.get(5), ing.get(18), 100.0, "grams"),
            ri(recipes.get(5), ing.get(10),  80.0, "grams"),
            ri(recipes.get(5), ing.get(19),  15.0, "grams"),

            ri(recipes.get(6), ing.get(0),  200.0, "grams"),
            ri(recipes.get(6), ing.get(10), 150.0, "grams"),
            ri(recipes.get(6), ing.get(8),   10.0, "grams"),
            ri(recipes.get(6), ing.get(9),   60.0, "grams"),
            ri(recipes.get(6), ing.get(7),   15.0, "ml"),

            ri(recipes.get(7), ing.get(3),  150.0, "grams"),
            ri(recipes.get(7), ing.get(5),   40.0, "grams"),
            ri(recipes.get(7), ing.get(4),   30.0, "ml")
        ));

        return recipes;
    }

    private Recipe recipe(User user, String title, String description, String instructions,
                           int prepTime, int cookTime, int servings, Difficulty difficulty) {
        return Recipe.builder()
                .user(user).title(title).description(description).instructions(instructions)
                .prepTime(prepTime).cookTime(cookTime).servings(servings).difficulty(difficulty)
                .build();
    }

    private RecipeIngredient ri(Recipe recipe, Ingredient ingredient, double qty, String unit) {
        return RecipeIngredient.builder()
                .recipe(recipe).ingredient(ingredient).quantity(qty).unit(unit).build();
    }

    // ─── Meal Plan ───────────────────────────────────────────────────────────

    private void seedMealPlan(User user, List<Recipe> recipes) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);

        MealPlan plan = MealPlan.builder()
                .user(user)
                .name("Demo Week Plan")
                .startDate(monday)
                .endDate(monday.plusDays(6))
                .build();
        plan = mealPlanRepository.save(plan);

        List<MealPlanEntry> entries = List.of(
            entry(plan, recipes.get(4), DayOfWeek.MONDAY,    MealType.BREAKFAST),
            entry(plan, recipes.get(0), DayOfWeek.MONDAY,    MealType.LUNCH),
            entry(plan, recipes.get(2), DayOfWeek.MONDAY,    MealType.DINNER),

            entry(plan, recipes.get(4), DayOfWeek.TUESDAY,   MealType.BREAKFAST),
            entry(plan, recipes.get(6), DayOfWeek.TUESDAY,   MealType.LUNCH),
            entry(plan, recipes.get(1), DayOfWeek.TUESDAY,   MealType.DINNER),

            entry(plan, recipes.get(7), DayOfWeek.WEDNESDAY, MealType.BREAKFAST),
            entry(plan, recipes.get(5), DayOfWeek.WEDNESDAY, MealType.LUNCH),
            entry(plan, recipes.get(3), DayOfWeek.WEDNESDAY, MealType.DINNER),

            entry(plan, recipes.get(4), DayOfWeek.THURSDAY,  MealType.BREAKFAST),
            entry(plan, recipes.get(0), DayOfWeek.THURSDAY,  MealType.LUNCH),
            entry(plan, recipes.get(2), DayOfWeek.THURSDAY,  MealType.DINNER),

            entry(plan, recipes.get(3), DayOfWeek.FRIDAY,    MealType.BREAKFAST),
            entry(plan, recipes.get(6), DayOfWeek.FRIDAY,    MealType.LUNCH),
            entry(plan, recipes.get(5), DayOfWeek.FRIDAY,    MealType.DINNER),

            entry(plan, recipes.get(7), DayOfWeek.SATURDAY,  MealType.BREAKFAST),
            entry(plan, recipes.get(1), DayOfWeek.SATURDAY,  MealType.LUNCH),
            entry(plan, recipes.get(2), DayOfWeek.SATURDAY,  MealType.DINNER),

            entry(plan, recipes.get(4), DayOfWeek.SUNDAY,    MealType.BREAKFAST),
            entry(plan, recipes.get(0), DayOfWeek.SUNDAY,    MealType.LUNCH),
            entry(plan, recipes.get(6), DayOfWeek.SUNDAY,    MealType.DINNER)
        );
        mealPlanEntryRepository.saveAll(entries);
    }

    private MealPlanEntry entry(MealPlan plan, Recipe recipe, DayOfWeek day, MealType mealType) {
        return MealPlanEntry.builder()
                .mealPlan(plan).recipe(recipe).dayOfWeek(day).mealType(mealType).build();
    }

    // ─── Shopping List ───────────────────────────────────────────────────────

    private void seedShoppingList(User user, List<Ingredient> ing) {
        ShoppingList list = ShoppingList.builder()
                .user(user)
                .name("Demo Week Shopping List")
                .build();
        list = shoppingListRepository.save(list);

        List<ShoppingListItem> items = List.of(
            item(list, ing.get(0),  400.0, "grams",  false),
            item(list, ing.get(1),  600.0, "grams",  false),
            item(list, ing.get(2),  300.0, "grams",  false),
            item(list, ing.get(3),  450.0, "grams",  true),
            item(list, ing.get(4),  500.0, "ml",     true),
            item(list, ing.get(5),   80.0, "grams",  false),
            item(list, ing.get(6),  360.0, "grams",  false),
            item(list, ing.get(7),   90.0, "ml",     true),
            item(list, ing.get(8),   30.0, "grams",  false),
            item(list, ing.get(9),  120.0, "grams",  false),
            item(list, ing.get(10), 230.0, "grams",  false),
            item(list, ing.get(11), 200.0, "grams",  false),
            item(list, ing.get(12), 200.0, "grams",  true),
            item(list, ing.get(13), 160.0, "grams",  false),
            item(list, ing.get(14), 200.0, "grams",  false),
            item(list, ing.get(15), 360.0, "grams",  false),
            item(list, ing.get(16), 400.0, "grams",  false),
            item(list, ing.get(17), 240.0, "grams",  false),
            item(list, ing.get(18), 200.0, "grams",  false),
            item(list, ing.get(19),  60.0, "grams",  true)
        );
        shoppingListItemRepository.saveAll(items);
    }

    private ShoppingListItem item(ShoppingList list, Ingredient ingredient,
                                   double qty, String unit, boolean checked) {
        return ShoppingListItem.builder()
                .shoppingList(list).ingredient(ingredient)
                .quantity(qty).unit(unit).checked(checked).build();
    }

    // ─── Alice: high-protein focus, owns her own recipes ─────────────────────

    private void seedAliceData(User alice, List<Ingredient> ing, List<Recipe> sharedRecipes) {
        // Alice creates two of her own recipes
        Recipe aliceR1 = recipeRepository.save(recipe(alice, "Salmon Power Bowl",
                "Alice's go-to high-protein meal prep bowl.",
                "1. Cook brown rice and let cool.\n2. Pan-sear salmon with lemon juice for 4 minutes each side.\n3. Blanch spinach for 1 minute.\n4. Slice avocado.\n5. Assemble: rice, salmon, spinach, avocado. Season and serve.",
                10, 15, 2, Difficulty.MEDIUM));

        Recipe aliceR2 = recipeRepository.save(recipe(alice, "Greek Yogurt Protein Bowl",
                "Alice's quick breakfast — high protein, low effort.",
                "1. Spoon Greek yogurt into a bowl.\n2. Slice banana and add on top.\n3. Sprinkle oats for crunch.\n4. Drizzle with honey if desired.",
                5, 0, 1, Difficulty.EASY));

        recipeIngredientRepository.saveAll(List.of(
            ri(aliceR1, ing.get(15), 180.0, "grams"),  // Salmon
            ri(aliceR1, ing.get(1),  150.0, "grams"),  // Brown Rice
            ri(aliceR1, ing.get(11), 100.0, "grams"),  // Spinach
            ri(aliceR1, ing.get(18), 100.0, "grams"),  // Avocado
            ri(aliceR1, ing.get(19),  15.0, "grams"),  // Lemon

            ri(aliceR2, ing.get(12), 200.0, "grams"),  // Greek Yogurt
            ri(aliceR2, ing.get(14), 100.0, "grams"),  // Banana
            ri(aliceR2, ing.get(13),  40.0, "grams")   // Oats
        ));

        // Alice's meal plan: Mon–Wed using her recipes + shared recipes
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        MealPlan alicePlan = mealPlanRepository.save(MealPlan.builder()
                .user(alice)
                .name("Alice's Protein Week")
                .startDate(monday)
                .endDate(monday.plusDays(6))
                .build());

        mealPlanEntryRepository.saveAll(List.of(
            entry(alicePlan, aliceR2,              DayOfWeek.MONDAY,    MealType.BREAKFAST),
            entry(alicePlan, aliceR1,              DayOfWeek.MONDAY,    MealType.LUNCH),
            entry(alicePlan, sharedRecipes.get(0), DayOfWeek.MONDAY,    MealType.DINNER),

            entry(alicePlan, aliceR2,              DayOfWeek.TUESDAY,   MealType.BREAKFAST),
            entry(alicePlan, sharedRecipes.get(6), DayOfWeek.TUESDAY,   MealType.LUNCH),
            entry(alicePlan, aliceR1,              DayOfWeek.TUESDAY,   MealType.DINNER),

            entry(alicePlan, aliceR2,              DayOfWeek.WEDNESDAY, MealType.BREAKFAST),
            entry(alicePlan, sharedRecipes.get(3), DayOfWeek.WEDNESDAY, MealType.LUNCH),
            entry(alicePlan, sharedRecipes.get(2), DayOfWeek.WEDNESDAY, MealType.DINNER),

            entry(alicePlan, aliceR2,              DayOfWeek.THURSDAY,  MealType.BREAKFAST),
            entry(alicePlan, aliceR1,              DayOfWeek.THURSDAY,  MealType.LUNCH),
            entry(alicePlan, sharedRecipes.get(0), DayOfWeek.THURSDAY,  MealType.DINNER),

            entry(alicePlan, aliceR2,              DayOfWeek.FRIDAY,    MealType.BREAKFAST),
            entry(alicePlan, sharedRecipes.get(2), DayOfWeek.FRIDAY,    MealType.LUNCH),
            entry(alicePlan, sharedRecipes.get(6), DayOfWeek.FRIDAY,    MealType.DINNER)
        ));

        // Alice's shopping list — protein-focused items
        ShoppingList aliceList = shoppingListRepository.save(ShoppingList.builder()
                .user(alice)
                .name("Alice's Protein Shopping List")
                .mealPlan(alicePlan)
                .build());

        shoppingListItemRepository.saveAll(List.of(
            item(aliceList, ing.get(15), 900.0, "grams", false),  // Salmon
            item(aliceList, ing.get(1),  750.0, "grams", false),  // Brown Rice
            item(aliceList, ing.get(11), 500.0, "grams", false),  // Spinach
            item(aliceList, ing.get(18), 500.0, "grams", false),  // Avocado
            item(aliceList, ing.get(12), 600.0, "grams", true),   // Greek Yogurt
            item(aliceList, ing.get(14), 500.0, "grams", true),   // Banana
            item(aliceList, ing.get(13), 200.0, "grams", false),  // Oats
            item(aliceList, ing.get(0),  400.0, "grams", false),  // Chicken Breast
            item(aliceList, ing.get(19),  60.0, "grams", false),  // Lemon
            item(aliceList, ing.get(8),   20.0, "grams", true)    // Garlic
        ));
    }

    // ─── Bob: vegan focus, owns his own recipes ───────────────────────────────

    private void seedBobData(User bob, List<Ingredient> ing, List<Recipe> sharedRecipes) {
        // Bob creates two of his own vegan recipes
        Recipe bobR1 = recipeRepository.save(recipe(bob, "Bob's Vegan Stir Fry",
                "Bob's quick plant-based weeknight dinner.",
                "1. Cook brown rice.\n2. Heat olive oil in a wok over high heat.\n3. Stir fry broccoli, onion and garlic for 5 minutes.\n4. Add tomatoes and black beans, cook 3 more minutes.\n5. Season with salt, pepper and serve over rice.",
                10, 15, 2, Difficulty.EASY));

        Recipe bobR2 = recipeRepository.save(recipe(bob, "Bob's Overnight Banana Oats",
                "Bob's meal-prep breakfast for the whole week.",
                "1. Mix oats with water or plant milk in a jar.\n2. Stir in a mashed banana.\n3. Refrigerate overnight.\n4. Top with sliced banana in the morning.",
                5, 0, 1, Difficulty.EASY));

        recipeIngredientRepository.saveAll(List.of(
            ri(bobR1, ing.get(1),  150.0, "grams"),  // Brown Rice
            ri(bobR1, ing.get(2),  150.0, "grams"),  // Broccoli
            ri(bobR1, ing.get(9),   80.0, "grams"),  // Onion
            ri(bobR1, ing.get(8),   10.0, "grams"),  // Garlic
            ri(bobR1, ing.get(10), 120.0, "grams"),  // Tomato
            ri(bobR1, ing.get(17), 120.0, "grams"),  // Black Beans
            ri(bobR1, ing.get(7),   15.0, "ml"),     // Olive Oil

            ri(bobR2, ing.get(13),  80.0, "grams"),  // Oats
            ri(bobR2, ing.get(14), 100.0, "grams")   // Banana
        ));

        // Bob's meal plan: Mon–Fri, mostly vegan meals
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        MealPlan bobPlan = mealPlanRepository.save(MealPlan.builder()
                .user(bob)
                .name("Bob's Plant-Based Week")
                .startDate(monday)
                .endDate(monday.plusDays(6))
                .build());

        mealPlanEntryRepository.saveAll(List.of(
            entry(bobPlan, bobR2,               DayOfWeek.MONDAY,    MealType.BREAKFAST),
            entry(bobPlan, sharedRecipes.get(5),DayOfWeek.MONDAY,    MealType.LUNCH),
            entry(bobPlan, bobR1,               DayOfWeek.MONDAY,    MealType.DINNER),

            entry(bobPlan, bobR2,               DayOfWeek.TUESDAY,   MealType.BREAKFAST),
            entry(bobPlan, bobR1,               DayOfWeek.TUESDAY,   MealType.LUNCH),
            entry(bobPlan, sharedRecipes.get(5),DayOfWeek.TUESDAY,   MealType.DINNER),

            entry(bobPlan, bobR2,               DayOfWeek.WEDNESDAY, MealType.BREAKFAST),
            entry(bobPlan, sharedRecipes.get(5),DayOfWeek.WEDNESDAY, MealType.LUNCH),
            entry(bobPlan, bobR1,               DayOfWeek.WEDNESDAY, MealType.DINNER),

            entry(bobPlan, bobR2,               DayOfWeek.THURSDAY,  MealType.BREAKFAST),
            entry(bobPlan, bobR1,               DayOfWeek.THURSDAY,  MealType.LUNCH),
            entry(bobPlan, sharedRecipes.get(1),DayOfWeek.THURSDAY,  MealType.DINNER),

            entry(bobPlan, bobR2,               DayOfWeek.FRIDAY,    MealType.BREAKFAST),
            entry(bobPlan, sharedRecipes.get(5),DayOfWeek.FRIDAY,    MealType.LUNCH),
            entry(bobPlan, bobR1,               DayOfWeek.FRIDAY,    MealType.DINNER)
        ));

        // Bob's shopping list — plant-based items
        ShoppingList bobList = shoppingListRepository.save(ShoppingList.builder()
                .user(bob)
                .name("Bob's Vegan Shopping List")
                .mealPlan(bobPlan)
                .build());

        shoppingListItemRepository.saveAll(List.of(
            item(bobList, ing.get(1),  750.0, "grams", false),  // Brown Rice
            item(bobList, ing.get(2),  750.0, "grams", false),  // Broccoli
            item(bobList, ing.get(9),  400.0, "grams", false),  // Onion
            item(bobList, ing.get(8),   50.0, "grams", true),   // Garlic
            item(bobList, ing.get(10), 600.0, "grams", false),  // Tomato
            item(bobList, ing.get(17), 600.0, "grams", false),  // Black Beans
            item(bobList, ing.get(7),   75.0, "ml",    true),   // Olive Oil
            item(bobList, ing.get(13), 400.0, "grams", false),  // Oats
            item(bobList, ing.get(14), 500.0, "grams", true),   // Banana
            item(bobList, ing.get(18), 300.0, "grams", false),  // Avocado
            item(bobList, ing.get(19),  40.0, "grams", false)   // Lemon
        ));
    }
}
