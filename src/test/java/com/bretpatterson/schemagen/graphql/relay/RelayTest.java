package com.bretpatterson.schemagen.graphql.relay;

import com.bretpatterson.schemagen.graphql.GraphQLSchemaBuilder;
import com.bretpatterson.schemagen.graphql.impl.common.JacksonTypeFactory;
import com.bretpatterson.schemagen.graphql.relay.controller.GameController;
import com.bretpatterson.schemagen.graphql.relay.controller.GameDTO;
import com.bretpatterson.schemagen.graphql.relay.controller.UserDTO;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.DeleteGamePayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.DeleteUserPayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewGameInput;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewGamePayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewUserPayload;
import com.bretpatterson.schemagen.graphql.relay.model.relay.factories.RelayGameFactory;
import com.bretpatterson.schemagen.graphql.relay.model.relay.factories.RelayUserFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by bpatterson on 1/27/16.
 */
public class RelayTest {

	GraphQLSchema schema;
	ObjectMapper objectMapper = new ObjectMapper();
	GameController gameController = new GameController();

	@Before
	public void setup() {
		this.schema = GraphQLSchemaBuilder.newBuilder()
				.registerGraphQLControllerObjects(ImmutableList.<Object> of(gameController))
				.registerTypeFactory(new JacksonTypeFactory(new ObjectMapper()))
				.registerNodeFactories(ImmutableList.of(new RelayGameFactory(gameController), new RelayUserFactory(gameController)))
				.relayEnabled(true)
				.build();

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getQueryResults(ExecutionResult result, String queryName) {
		Map<String, Object> results = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("Queries");

		return (Map<String, Object>) results.get(queryName);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMutationResults(ExecutionResult result, String mutationName) {
		Map<String, Object> results = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("Mutations");

		return (Map<String,Object>) results.get(mutationName);
	}

	private NewGamePayload createGame(String name, String clientMutationId) throws IOException {
		NewGameInput input = objectMapper.readValue(String.format("{\"game\":{\"name\":\"%s\"}, \"clientMutationId\":\"%s\" }", name, clientMutationId),
				NewGameInput.class);
		ExecutionResult result = new GraphQL(schema).execute(String.format(
				"mutation M { Mutations { createGame(input:{ game: {name:\"%s\"}, clientMutationId:\"%s\"}) { game {id, name}, clientMutationId } } }",
				name,
				clientMutationId));
		assertEquals(0, result.getErrors().size());

		NewGamePayload payload = objectMapper.readValue(objectMapper.writeValueAsString(getMutationResults(result, "createGame")), NewGamePayload.class);

		assertEquals(name, payload.getGame().getName());
		assertEquals(clientMutationId, payload.getClientMutationId());

		return payload;
	}

	private DeleteGamePayload deleteGame(String id, String clientMutationId) throws IOException {
		ExecutionResult result = new GraphQL(schema).execute(
				String.format("mutation M { Mutations {deleteGame(input:{ id:\"%s\", clientMutationId:\"%s\"}) {clientMutationId} } }", id, clientMutationId));
		assertEquals(0, result.getErrors().size());

		DeleteGamePayload payload = objectMapper.readValue(objectMapper.writeValueAsString(getMutationResults(result, "deleteGame")), DeleteGamePayload.class);

		assertEquals(clientMutationId, payload.getClientMutationId());

		return payload;
	}

	private NewUserPayload createUser(String name, String email, String clientMutationId) throws IOException {
		ExecutionResult result = new GraphQL(schema).execute(String.format(
				"mutation M { Mutations {createUser(input: { user: { name:\"%s\", email:\"%s\"}, clientMutationId:\"%s\"}) { user {id, name, email}, clientMutationId} } }",
				name,
				email,
				clientMutationId));
		assertEquals(0, result.getErrors().size());

		NewUserPayload payload = objectMapper.readValue(objectMapper.writeValueAsString(getMutationResults(result, "createUser")), NewUserPayload.class);

		assertEquals(name, payload.getUser().getName());
		assertEquals(email, payload.getUser().getEmail());
		assertEquals(clientMutationId, payload.getClientMutationId());

		return payload;
	}

	private DeleteUserPayload deleteUser(String id, String clientMutationId) throws IOException {
		ExecutionResult result = new GraphQL(schema).execute(String
				.format("mutation M { Mutations { deleteUser(input: { id:\"%s\", clientMutationId:\"%s\"}) {clientMutationId} } }", id, clientMutationId));
		assertEquals(0, result.getErrors().size());

		DeleteUserPayload payload = objectMapper.readValue(objectMapper.writeValueAsString(getMutationResults(result, "deleteUser")), DeleteUserPayload.class);

		assertEquals(clientMutationId, payload.getClientMutationId());

		return payload;
	}

	@SuppressWarnings("unchecked")
    private GameDTO getGameNode(String id) throws IOException {

		ExecutionResult result = new GraphQL(schema).execute(String.format("{ node(id:\"%s\") {...on Game {id, name} } }", id));
		assertEquals(0, result.getErrors().size());

		return objectMapper.readValue(objectMapper.writeValueAsString(((Map<String, Object>) result.getData()).get("node")), GameDTO.class);
	}

	@SuppressWarnings("unchecked")
    private UserDTO getUserNode(String id) throws IOException {

		ExecutionResult result = new GraphQL(schema).execute(String.format("{ node(id:\"%s\") {...on User {id, name, email} } }", id));
		assertEquals(0, result.getErrors().size());

		return objectMapper.readValue(objectMapper.writeValueAsString(((Map<String, Object>) result.getData()).get("node")), UserDTO.class);
	}

	private RelayConnection<GameDTO> findGames(int first) throws IOException {

		ExecutionResult result = new GraphQL(schema).execute(String
				.format("{ Queries { games(first:%d) { edges { node {id, name}, cursor {value} } , pageInfo {hasPreviousPage, hasNextPage} } } }", first));
		assertEquals(0, result.getErrors().size());

		return deserialize(objectMapper.writeValueAsString(getQueryResults(result, "games")), new TypeReference<RelayConnection<GameDTO>>() {
		});
	}

	private <T> T deserialize(String value, TypeReference<?> type) {
		try {
			return objectMapper.readValue(value, type);
		} catch (IOException ex) {
			throw Throwables.propagate(ex);
		}
	}

	@Test
	public void testCreateGame() throws IOException {
		GameDTO newGame;
		GameDTO game;

		newGame = createGame("Game1", "Game1").getGame();
		game = getGameNode(newGame.getId());
		assertEquals(newGame.getId(), game.getId());
		assertEquals(newGame.getName(), game.getName());
	}

	@Test
	public void testCreateUser() throws IOException {
		UserDTO newUser = createUser("User1", "user1@gmail.com", "user1").getUser();
		UserDTO user = getUserNode(newUser.getId());

		assertEquals(newUser.getId(), user.getId());
		assertEquals(newUser.getName(), user.getName());
		assertEquals(newUser.getEmail(), user.getEmail());
	}

	@Test
	public void testDeleteGame() throws IOException {
		GameDTO newGame;
		GameDTO game;

		newGame = createGame("Game1", "Game1").getGame();
		game = getGameNode(newGame.getId());
		assertEquals(newGame.getId(), game.getId());
		assertEquals(newGame.getName(), game.getName());

		deleteGame(newGame.getId(), "deletGame");
		game = getGameNode(newGame.getId());
		assertNull(game);
	}

	@Test
	public void testDeleteUser() throws IOException {
		UserDTO newUser = createUser("User1", "user1@gmail.com", "user1").getUser();
		UserDTO user = getUserNode(newUser.getId());

		assertEquals(newUser.getId(), user.getId());
		assertEquals(newUser.getName(), user.getName());
		assertEquals(newUser.getEmail(), user.getEmail());

		deleteUser(newUser.getId(), "deleteUser");
		user = getUserNode(newUser.getId());
		assertNull(user);
	}

	@Test
	public void testFindGames() throws IOException {
		GameDTO newGame;

		for (int i = 0; i < 1000; i++) {
			newGame = createGame(String.format("Game %d", i), String.format("Mutation ID:%d", i)).getGame();
		}
		RelayConnection<GameDTO> games = findGames(100);

		assertEquals(100, games.getEdges().size());
		assertTrue(games.getPageInfo().isHasNextPage());
		assertTrue(!games.getPageInfo().isHasPreviousPage());
		for (int i = 0; i < 100; i++) {
			assertEquals(String.format("Game %d", i), games.getEdges().get(i).getNode().getName());
		}
	}
}
