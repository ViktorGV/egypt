package com.uber.egypt.signature.canonicalization;

import com.uber.egypt.signature.InvalidDocumentFormatException;
import com.google.gson.*;

import java.io.IOException;

public class JsonUtils {

    /**
     * Transforms a valid document to the canonical format specified by the Egyptian Tax Authority
     * (ETA).
     * <p>
     * Refer to <a href=
     * "https://sdk.invoicing.eta.gov.eg/document-serialization-approach/#algorithm-overview">this
     * page</a> for the specification of the canonical format.
     */
    public static String canonicalize(String document) {
        JsonElement documentAsJson = convertToJson(document);
        return dispatchToCanonicalize(documentAsJson);
    }

    public static String merge(String document, String signature) {
        JsonObject result = new Gson().fromJson(document, JsonObject.class);
        JsonArray signatures = new JsonArray();
        signatures.add(buildIssuerTypeSignature(signature));
        result.add("signatures", signatures);
        return result.toString();
    }

    private static JsonElement convertToJson(String json) {
        TypeAdapter<JsonElement> strictAdapter = new Gson()
                .getAdapter(JsonElement.class);
        try {
            return strictAdapter.fromJson(json);
        } catch (JsonSyntaxException | IOException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    private static String dispatchToCanonicalize(JsonElement jsonElement) {
        return dispatchToCanonicalize(jsonElement, "");
    }

    private static String dispatchToCanonicalize(JsonElement jsonElement, String key) {
        if (jsonElement.isJsonNull()) {
            return "";
        } else if (jsonElement.isJsonPrimitive()) {
            return canonicalizeJsonPrimitive(jsonElement.getAsJsonPrimitive());
        } else if (jsonElement.isJsonArray()) {
            return canonicalizeJsonArray(jsonElement.getAsJsonArray(), key);
        } else if (jsonElement.isJsonObject()) {
            return canonicalizeJsonObject(jsonElement.getAsJsonObject());
        } else {
            throw new JsonSyntaxException(
                    jsonElement + " is not a valid JsonElement"
            );
        }
    }

    // Base case.
    private static String canonicalizeJsonPropertyName(String propertyName) {
        return "\"" + propertyName.toUpperCase() + "\"";
    }

    // Base case.
    private static String canonicalizeJsonPrimitive(JsonPrimitive jsonPrimitive) {
        return "\"" + jsonPrimitive.getAsString() + "\"";
    }

    // Recursive step.
    private static String canonicalizeJsonArray(JsonArray jsonArray, String key) {
        StringBuilder result = new StringBuilder();
        for (JsonElement jsonElement : jsonArray) {
            result.append(canonicalizeJsonPropertyName(key));
            result.append(dispatchToCanonicalize(jsonElement));
        }
        return result.toString();
    }

    // Recursive step.
    private static String canonicalizeJsonObject(JsonObject jsonObject) {
        StringBuilder result = new StringBuilder();
        for (String key : jsonObject.keySet()) {
            JsonElement jsonElement = jsonObject.get(key);
            String canonicalizedElement = dispatchToCanonicalize(jsonElement, key);
            result.append(canonicalizeJsonPropertyName(key));
            result.append(canonicalizedElement);
        }
        return result.toString();
    }

    private static JsonObject buildIssuerTypeSignature(String signature) {
        JsonObject result = new JsonObject();
        final String ISSUER_TYPE = "I";
        result.add("signatureType", new JsonPrimitive(ISSUER_TYPE));
        result.add("value", new JsonPrimitive(signature));
        return result;
    }
}
