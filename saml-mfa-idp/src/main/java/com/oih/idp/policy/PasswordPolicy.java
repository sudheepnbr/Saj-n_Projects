package com.oih.idp.policy;

import com.oih.idp.config.IdpProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private final IdpProperties.Password settings;

    public PasswordPolicy(IdpProperties properties) {
        this.settings = properties.getPassword();
    }

    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null) {
            errors.add("Password is required.");
            return errors;
        }
        if (password.length() < settings.getMinLength()) {
            errors.add("Password must be at least " + settings.getMinLength() + " characters.");
        }
        if (password.length() > settings.getMaxLength()) {
            errors.add("Password must be at most " + settings.getMaxLength() + " characters.");
        }
        int classes = 0;
        if (password.chars().anyMatch(Character::isLowerCase)) {
            classes++;
        }
        if (password.chars().anyMatch(Character::isUpperCase)) {
            classes++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            classes++;
        }
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            classes++;
        }
        if (classes < settings.getRequiredCharacterClasses()) {
            errors.add("Password must use at least " + settings.getRequiredCharacterClasses()
                    + " character classes (upper, lower, digit, symbol).");
        }
        return errors;
    }

    public int historySize() {
        return settings.getHistorySize();
    }
}
