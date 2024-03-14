package com.github.mostafaism1.etaeinvoicesigner.signature.merge;

import static org.assertj.core.api.BDDAssertions.then;

import com.github.mostafaism1.etaeinvoicesigner.signature.canonicalization.JsonUtils;
import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ETASignatureMergeStrategyTest {

    @Test
    public void merged_document_should_have_correct_structure_and_value() {
        // Given.
        String document = "{key: \"value\"}";
        String signatureValue = "signatureValue";
        String signatures = """
                [
                      {
                        "signatureType": "I",
                        "value": "signatureValue"
                      }
                    ]""";
        Gson gson = new Gson();
        JsonObject documentAsJson = gson.fromJson(document, JsonObject.class);
        JsonArray signaturesAsJson = gson.fromJson(signatures, JsonArray.class);
        JsonObject expected = documentAsJson.deepCopy();
        expected.add("signatures", signaturesAsJson);

        // When.
        String merged = JsonUtils.merge(document, signatureValue);
        JsonObject actual = gson.fromJson(merged, JsonObject.class);

        // Then.
        then(actual).isEqualTo(expected);
    }

}
