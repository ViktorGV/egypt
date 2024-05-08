package com.uber.egypt.signature;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.uber.egypt.document.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

@Component
public class DocumentSigningService {
    private final String DOCUMENTS_ARRAY_KEY = "documents";
    private final CadesBesSigningStrategy cadesBesSigningStrategy;
    private final Gson gson;

    protected DocumentSigningService(CadesBesSigningStrategy cadesBesSigningStrategy) {
        this.gson = new Gson();
        this.cadesBesSigningStrategy = cadesBesSigningStrategy;
    }

    public String generateSignedDocuments(String documents) {
        var unsignedDocuments = extractUnsignedDocuments(documents);
        var signedDocuments = signDocuments(unsignedDocuments);
        return wrapSignedDocuments(signedDocuments).toString();
    }

    public String generateSignedDocument(String document) {
        var canonicalizedDocument = JsonUtils.canonicalize(document);
        var signature = cadesBesSigningStrategy.sign(canonicalizedDocument);
        return JsonUtils.merge(document, signature);
    }

    private JsonArray extractUnsignedDocuments(String documents) {
        return gson
                .fromJson(documents, JsonObject.class)
                .get(DOCUMENTS_ARRAY_KEY)
                .getAsJsonArray();
    }

    private JsonObject wrapSignedDocuments(JsonArray signedDocuments) {
        JsonObject result = new JsonObject();
        result.add(DOCUMENTS_ARRAY_KEY, signedDocuments);
        return result;
    }

    private JsonArray signDocuments(JsonArray unsignedDocuments) {
        return StreamSupport
                .stream(unsignedDocuments.spliterator(), true)
                .map(
                        unsignedDocument -> generateSignedDocument(unsignedDocument.toString())
                )
                .map(signedDocument -> gson.fromJson(signedDocument, JsonObject.class))
                .collect(JsonObjectCollector.toJsonObjectCollector());
    }

    private static class JsonObjectCollector
            implements Collector<JsonObject, JsonArray, JsonArray> {

        public static JsonObjectCollector toJsonObjectCollector() {
            return new JsonObjectCollector();
        }

        @Override
        public Supplier<JsonArray> supplier() {
            return JsonArray::new;
        }

        @Override
        public BiConsumer<JsonArray, JsonObject> accumulator() {
            return (array, object) -> array.add(object);
        }

        @Override
        public BinaryOperator<JsonArray> combiner() {
            return (array1, array2) -> {
                array1.addAll(array2);
                return array1;
            };
        }

        @Override
        public Function<JsonArray, JsonArray> finisher() {
            return jsonArray -> jsonArray;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of(Characteristics.UNORDERED);
        }
    }
}
