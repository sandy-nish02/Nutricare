package com.nutricare.service;

/** Pure business rules are kept here so they are easy to test and explain. */
public final class HealthRules {
    private HealthRules() { }

    public static double bmi(double weightKg, double heightCm) {
        if (weightKg <= 0 || heightCm <= 0) {
            throw new IllegalArgumentException("Height and weight must be positive");
        }
        double heightMetres = heightCm / 100.0;
        return Math.round((weightKg / (heightMetres * heightMetres)) * 100.0) / 100.0;
    }

    public static String bmiLabel(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Healthy";
        if (bmi < 30) return "Overweight";
        return "Obesity range";
    }

    /** Each reading is judged on its own, so one out-of-range value is enough to flag the check-up. */
    public static boolean abnormal(double bmi, Integer systolic, Integer diastolic, Double bloodSugar) {
        boolean bmiRisk = bmi < 18.5 || bmi >= 30;
        boolean systolicRisk = systolic != null && (systolic >= 140 || systolic < 90);
        boolean diastolicRisk = diastolic != null && (diastolic >= 90 || diastolic < 60);
        boolean sugarRisk = bloodSugar != null && (bloodSugar < 70 || bloodSugar > 140);
        return bmiRisk || systolicRisk || diastolicRisk || sugarRisk;
    }

    public static boolean significantWeightChange(double oldWeight, double newWeight) {
        return oldWeight > 0 && Math.abs(newWeight - oldWeight) / oldWeight >= 0.05;
    }
}
