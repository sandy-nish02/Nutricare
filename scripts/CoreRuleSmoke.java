import com.nutricare.service.DietSafety;
import com.nutricare.service.HealthRules;
import com.nutricare.service.OtpService;

/** Dependency-free smoke checks for environments where Maven is unavailable. */
public class CoreRuleSmoke {
    public static void main(String[] args) {
        require(HealthRules.bmi(70, 175) == 22.86, "BMI calculation");
        require(HealthRules.abnormal(31, 120, 80, 100.0), "BMI risk flag");
        require(!HealthRules.abnormal(22, 120, 80, 100.0), "healthy reading");
        require(HealthRules.significantWeightChange(80, 84), "diet review trigger");
        require(DietSafety.conflictingAllergies("Peanut, shellfish", "PEANUT butter").size() == 1, "allergy blocking");
        String otp = OtpService.generate();
        require(otp.matches("\\d{6}"), "OTP format");
        require(OtpService.hash(otp).length() == 64, "OTP hashing");
        System.out.println("Core NutriCare rule smoke checks passed");
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name + " failed");
    }
}
