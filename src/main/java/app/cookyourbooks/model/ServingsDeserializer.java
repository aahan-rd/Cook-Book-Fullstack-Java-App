package app.cookyourbooks.model;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom JSON deserializer for {@link Servings} that supports both the native Servings format
 * ({@code {"amount": N, "description": "..."}}) and the legacy Quantity format ({@code {"type":
 * "exact", "amount": N, "unit": "WHOLE"}}) for backward compatibility.
 */
public final class ServingsDeserializer extends JsonDeserializer<Servings> {

  @Override
  public @Nullable Servings deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    if (node == null || node.isNull()) {
      return null;
    }

    int amount;
    String description = null;

    // Legacy Quantity format: {"type": "exact", "amount": N, "unit": "WHOLE"}
    if (node.has("type") && "exact".equals(node.get("type").asText())) {
      amount = node.get("amount").asInt();
      if (amount <= 0) {
        throw new IllegalArgumentException("amount must be positive");
      }
      return new Servings(amount);
    }

    // Native Servings format: {"amount": N, "description": "..."} or {"amount": N}
    if (node.has("amount")) {
      amount = node.get("amount").asInt();
      if (node.has("description") && !node.get("description").isNull()) {
        description = node.get("description").asText();
      }
      return new Servings(amount, description);
    }

    throw new IllegalArgumentException("Cannot deserialize Servings from: " + node);
  }
}
