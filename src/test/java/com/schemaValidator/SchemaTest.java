package com.schemaValidator;

import java.io.InputStream;
import java.net.URL;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import com.SchemaService;
import com.schema.SchemaValidator;  

@SpringBootTest(classes = SchemaValidator.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
class SchemaValidatorTest {
    @Autowired
    private ResourceLoader resourceLoader;

    private SchemaValidator schemaValidator;

    @BeforeEach
    void setup() {
        schemaValidator = new SchemaValidator(resourceLoader);
    }

    @Test
    void testMovieListRequest1() throws Exception {
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieListRequest",
                "correlatorId": 5555,
                "genre": "ACTION"
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieListRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    void testMovieListRequest2() throws Exception {
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieListRequest",
                "correlatorId": 5555,
                "movieName": "dummy"
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieListRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    void testMovieListRequest3() throws Exception {
        // JSON that satisfies the schema
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieListRequest",
                "correlatorId": 5556,
                "startingShowtime": "2025-11-10T00:00:00-06:00",
                "endingShowtime": "2025-11-12T23:59:59-06:00"
            }
        """);

        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieListRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertTrue(validate(topicName, validJson));
    }

    @Test
    void testInvalidJson() throws Exception {
        JSONObject validJson = new JSONObject("""
            {
                "topicName": "MovieListRequest",
                "correlatorId": 5555,
            }
        """);
        
        // Testing with a dynamically loaded schema from the SchemaService
        String topicName = "MovieListRequest";  

        // Assert the validation result (assuming valid schema)
        Assertions.assertFalse(validate(topicName, validJson));
    }

    private boolean validate(String topicName, JSONObject validJson)
    {
        URL schemaUrl = getClass().getClassLoader().getResource(SchemaService.getPathFor(topicName));

        // Check if the schema is found
        if (schemaUrl == null) {
            throw new RuntimeException("Schema not found for topic: " + topicName);
        }

        // Load schema stream from resource
        InputStream schemaStream = schemaValidator.getSchemaStream(SchemaService.getPathFor(topicName));

        if (schemaStream == null) {
            System.out.println("No schema found for topic: " + topicName);
        }

        // Validate the JSON using the schema stream
        return schemaValidator.validateJson(schemaStream, validJson);
    }
}
