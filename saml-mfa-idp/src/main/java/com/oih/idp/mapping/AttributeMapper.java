package com.oih.idp.mapping;

import com.oih.idp.identity.domain.User;
import com.oih.idp.sp.AttributeMapping;
import com.oih.idp.sp.ServiceProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AttributeMapper {

    public Map<String, String> release(User user, ServiceProvider sp) {
        Map<String, String> out = new LinkedHashMap<>();
        for (AttributeMapping mapping : sp.getMappings()) {
            if (AttributeMapping.BLOCKED.contains(mapping.getSourceField())) {
                continue;
            }
            String value = switch (mapping.getSourceField()) {
                case "email" -> user.getEmail();
                case "username" -> user.getUsername();
                case "firstName" -> user.getFirstName();
                case "lastName" -> user.getLastName();
                default -> null;
            };
            if (value != null) {
                out.put(mapping.getSamlName(), value);
            }
        }
        return out;
    }
}
