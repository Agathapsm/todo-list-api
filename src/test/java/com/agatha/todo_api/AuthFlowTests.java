package com.agatha.todo_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

	@Autowired
	MockMvc mvc;

	private String token(String user) throws Exception {
		String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return response.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
	}

	@Test
	void tasksRequireToken() throws Exception {
		mvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
		mvc.perform(get("/tasks").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		token("carol");
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"carol\",\"password\":\"wrong-pass\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usersOnlySeeTheirOwnTasks() throws Exception {
		String alice = "Bearer " + token("alice");
		String bob = "Bearer " + token("bob");

		String created = mvc.perform(post("/tasks").header("Authorization", alice)
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Buy milk\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = created.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

		mvc.perform(get("/tasks/" + id).header("Authorization", alice))
				.andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Buy milk"));
		mvc.perform(get("/tasks/" + id).header("Authorization", bob)).andExpect(status().isNotFound());
		mvc.perform(get("/tasks").header("Authorization", bob))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}
}