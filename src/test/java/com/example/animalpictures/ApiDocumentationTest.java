package com.example.animalpictures;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void publishesTheOpenApiDescription() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Snuggle Post · Animal Pictures API"))
                .andExpect(jsonPath("$.paths['/api/pictures/{animal}'].post.summary").value("Fetch and store new pictures"))
                .andExpect(jsonPath("$.paths['/api/pictures/{animal}/latest'].get").exists())
                .andExpect(jsonPath("$.paths['/api/pictures'].get").exists())
                .andExpect(jsonPath("$.paths['/api/pictures/counts'].get").exists());
    }

    @Test
    void servesTheSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
