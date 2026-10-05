package com.nutricare.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DietSafety {
    private DietSafety() { }

    /** Allergies may be separated by commas, semicolons, or new lines. */
    public static List<String> conflictingAllergies(String allergies, String mealPlan) {
        List<String> conflicts = new ArrayList<>();
        if (allergies == null || allergies.isBlank() || mealPlan == null) return conflicts;
        String normalizedMeals = mealPlan.toLowerCase(Locale.ROOT);
        for (String allergy : allergies.split("[,;\\r\\n]+")) {
            String item = allergy.trim().toLowerCase(Locale.ROOT);
            if (item.length() >= 3 && normalizedMeals.contains(item)) conflicts.add(allergy.trim());
        }
        return conflicts;
    }
}
