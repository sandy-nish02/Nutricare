package com.nutricare.repository;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Module 02 - Diet Plan Management. */
class DietPlanRepositoryTest extends IntegrationTestBase {

    @Test
    void patientsOnlySeeTheSingleActiveDietPlan() throws Exception {
        long patientUser = newPatient("Diet Patient");
        long patientId = patientId(patientUser);
        long nutritionist = newStaff("Diet Nutritionist", "NUTRITIONIST");

        diets.createDietPlan(patientId, nutritionist, "First plan", 1800, "Oats and rice",
                LocalDate.now(), LocalDate.now().plusDays(14));
        diets.createDietPlan(patientId, nutritionist, "Second plan", 1900, "Eggs and vegetables",
                LocalDate.now(), LocalDate.now().plusDays(14));
        assertTrue(diets.dietPlans(patientUser, "PATIENT").isEmpty(), "drafts are hidden from the patient");

        List<Map<String, Object>> ownedPlans = diets.dietPlans(nutritionist, "NUTRITIONIST");
        diets.updateDietStatus(planId(ownedPlans, "First plan"), "ACTIVE", nutritionist, "NUTRITIONIST");
        diets.updateDietStatus(planId(ownedPlans, "Second plan"), "ACTIVE", nutritionist, "NUTRITIONIST");

        List<Map<String, Object>> patientPlans = diets.dietPlans(patientUser, "PATIENT");
        assertEquals(1, patientPlans.size());
        assertEquals("Second plan", patientPlans.get(0).get("title"));
    }

    @Test
    void allergyCheckBlocksCreationAndActivation() throws Exception {
        long patientUser = patients.createPatient("Allergic Patient", email("allergic"), "0770000008", PASSWORD);
        long patientId = patientId(patientUser);
        long nutritionist = newStaff("Safety Nutritionist", "NUTRITIONIST");
        diets.createDietPlan(patientId, nutritionist, "Milk plan", 2000, "Breakfast: oats with milk",
                LocalDate.now(), LocalDate.now().plusDays(7));
        long planId = planId(diets.dietPlans(nutritionist, "NUTRITIONIST"), "Milk plan");

        patients.updateProfile(patientUser, "Allergic Patient", "0770000008", null, null,
                null, null, "Milk, shellfish", null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> diets.updateDietStatus(planId, "ACTIVE", nutritionist, "NUTRITIONIST"),
                "an allergy added after the draft blocks approval");
        assertThrows(IllegalArgumentException.class, () -> diets.createDietPlan(patientId, nutritionist,
                "Shellfish plan", 2000, "Dinner: shellfish curry", LocalDate.now(), LocalDate.now().plusDays(7)));
    }

    @Test
    void nutritionistsCanOnlyChangeTheirOwnPlans() throws Exception {
        long patientId = patientId(newPatient("Owner Patient"));
        long owner = newStaff("Owner Nutritionist", "NUTRITIONIST");
        long other = newStaff("Other Nutritionist", "NUTRITIONIST");
        diets.createDietPlan(patientId, owner, "Owned plan", 1800, "Rice", LocalDate.now(), LocalDate.now().plusDays(7));
        long planId = planId(diets.dietPlans(owner, "NUTRITIONIST"), "Owned plan");
        assertThrows(IllegalArgumentException.class, () -> diets.updateDietStatus(planId, "ACTIVE", other, "NUTRITIONIST"));
    }
}
