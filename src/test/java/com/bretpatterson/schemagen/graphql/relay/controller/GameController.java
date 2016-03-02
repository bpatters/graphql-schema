package com.bretpatterson.schemagen.graphql.relay.controller;

import com.bretpatterson.schemagen.graphql.annotations.GraphQLController;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLMutation;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLParam;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLQuery;
import com.bretpatterson.schemagen.graphql.relay.ConnectionCursor;
import com.bretpatterson.schemagen.graphql.relay.Edge;
import com.bretpatterson.schemagen.graphql.relay.PageInfo;
import com.bretpatterson.schemagen.graphql.relay.RelayConnection;
import com.bretpatterson.schemagen.graphql.relay.manager.GameManager;
import com.bretpatterson.schemagen.graphql.relay.model.IGame;
import com.bretpatterson.schemagen.graphql.relay.model.IUser;
import com.bretpatterson.schemagen.graphql.relay.model.PagedList;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.AddUserToGameInput;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.AddUserToGamePayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.DeleteGameInput;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.DeleteGamePayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.DeleteUserInput;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.DeleteUserPayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewGameInput;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewGamePayload;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewUserInput;
import com.bretpatterson.schemagen.graphql.relay.model.payloads.NewUserPayload;
import com.bretpatterson.schemagen.graphql.relay.model.relay.factories.RelayGameFactory;
import com.bretpatterson.schemagen.graphql.relay.model.relay.factories.RelayUserFactory;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by bpatterson on 1/27/16.
 */
@GraphQLController(rootQueriesObjectName = "Queries", rootMutationsObjectName = "Mutations")
public class GameController {

	GameManager gameManager = new GameManager();
	/**
	 * our game factories used by relay
	 */
	RelayGameFactory gameFactory = new RelayGameFactory(this);
	RelayUserFactory userFactory = new RelayUserFactory(this);

	@GraphQLQuery(name = "games")
	public RelayConnection<GameDTO> findGames(@GraphQLParam(name = "first") Integer first,
			@GraphQLParam(name = "last") Integer last,
			@GraphQLParam(name = "before") ConnectionCursor beforeCursor,
			@GraphQLParam(name = "after") ConnectionCursor afterCursor) {
		Optional<Long> before = Optional.absent();
		Optional<Long> after = Optional.absent();
		if (beforeCursor != null) {
			before = Optional.of(Long.parseLong(beforeCursor.getValue()));
		}
		if (afterCursor != null) {
			after = Optional.of(Long.parseLong(afterCursor.getValue()));
		}
		PagedList<IGame> games = gameManager.findGames(Optional.fromNullable(first), Optional.fromNullable(last), before, after);

		return convertToGameConnection(games);
	}

	@GraphQLMutation(name = "addUserToGame")
	public AddUserToGamePayload addUserToGame(@GraphQLParam(name = "input") AddUserToGameInput input) {

		Optional<IGame> foundGame = gameManager.findGame(gameFactory.getGameId(input.getGameId()));
		Optional<IUser> foundUser = gameManager.findUser(userFactory.getUserId(input.getUserId()));

		if (foundGame.isPresent() && foundUser.isPresent()) {
			IGame game = foundGame.get();
			IUser user = foundUser.get();
			game.getUsers().add(user);
			game = gameManager.updateGame(game);
			GameDTO gameDTO = convertToGameDTO(game);

			return new AddUserToGamePayload().setGame(gameDTO).setClientMutationId(input.getClientMutationId());
		} else {
			return new AddUserToGamePayload().setClientMutationId(input.getClientMutationId()).setGame(null);
		}

	}

	@GraphQLQuery(name = "users")
	public RelayConnection<UserDTO> findUsers(@GraphQLParam(name = "first") Integer first,
			@GraphQLParam(name = "first") Integer last,
			@GraphQLParam(name = "before") ConnectionCursor beforeCursor,
			@GraphQLParam(name = "after") ConnectionCursor afterCursor) {
		Optional<Long> before = Optional.absent();
		Optional<Long> after = Optional.absent();
		if (beforeCursor != null) {
			before = Optional.of(Long.parseLong(beforeCursor.getValue()));
		}
		if (afterCursor != null) {
			after = Optional.of(Long.parseLong(afterCursor.getValue()));
		}
		PagedList<IUser> users = gameManager.findUsers(Optional.fromNullable(first), Optional.fromNullable(last), before, after);

		return convertToUserConnection(users);
	}

	@GraphQLMutation(name = "createGame")
	public NewGamePayload createGame(@GraphQLParam(name = "input", required = true) NewGameInput newGameInput) {
		checkNotNull(newGameInput.getGame());
		checkNotNull(newGameInput.getGame().getName());
		checkNotNull(newGameInput.getClientMutationId());
		IGame newGame = gameManager.newGame().setName(newGameInput.getGame().getName());

		newGame = gameManager.createGame(newGame);

		return new NewGamePayload(convertToGameDTO(newGame), newGameInput.getClientMutationId());
	}

	@GraphQLMutation(name = "deleteGame")
	public DeleteGamePayload deleteGame(@GraphQLParam(name = "input", required = true) DeleteGameInput game) {
		checkNotNull(game);
		checkNotNull(game.getId());
		checkNotNull(game.getClientMutationId());
		gameManager.deleteGame(gameFactory.getGameId(game.getId()));

		return new DeleteGamePayload(game.getClientMutationId());

	}

	@GraphQLMutation(name = "createUser")
	public NewUserPayload createUser(@GraphQLParam(name = "input", required = true) NewUserInput userInput) {
		checkNotNull(userInput);
		checkNotNull(userInput.getUser());
		checkNotNull(userInput.getUser().getEmail());
		checkNotNull(userInput.getUser().getName());
		checkNotNull(userInput.getClientMutationId());

		IUser user = gameManager.newUser().setName(userInput.getUser().getName()).setEmail(userInput.getUser().getEmail());
		IUser newUser = gameManager.createUser(user);

		return new NewUserPayload(convertToUserDTO(newUser), userInput.getClientMutationId());
	}

	@GraphQLMutation(name = "deleteUser")
	public DeleteUserPayload deleteUser(@GraphQLParam(name = "input", required = true) DeleteUserInput userInput) {
		checkNotNull(userInput);
		checkNotNull(userInput.getId());
		checkNotNull(userInput.getClientMutationId());
		gameManager.deleteUser(userFactory.getUserId(userInput.getId()));

		return new DeleteUserPayload(userInput.getClientMutationId());
	}

	public GameDTO findGame(Long id) {
		return convertToGameDTO(gameManager.findGame(id).orNull());
	}

	public UserDTO findUser(Long id) {
		return convertToUserDTO(gameManager.findUser(id).orNull());
	}

	private GameDTO convertToGameDTO(IGame game) {
		if (game == null) {
			return null;
		}
		return new GameDTO()
				.setId(gameFactory.getNodeId(game.getId()))
				.setName(game.getName())
				.setUsers(convertToUserDTOList(game.getUsers()));
	}

	private List<UserDTO> convertToUserDTOList(List<IUser> user) {
		return Lists.transform(user, new Function<IUser, UserDTO>() {
			@Override
			public UserDTO apply(IUser input) {
				return convertToUserDTO(input);
			}
		});
	}

	private UserDTO convertToUserDTO(IUser user) {
		if (user == null) {
			return null;
		}
		UserDTO rv = new UserDTO().setId(userFactory.getNodeId(user.getId())).setName(user.getName()).setEmail(user.getEmail());

		return rv;
	}

	private RelayConnection<UserDTO> convertToUserConnection(PagedList<IUser> users) {
		RelayConnection<UserDTO> rv = new RelayConnection<>();

		PageInfo pageInfo = new PageInfo();
		pageInfo.setHasNextPage(users.isHasNextPage());
		pageInfo.setHasPreviousPage(users.isHasPreviousPage());
		rv.setPageInfo(pageInfo);
		ImmutableList.Builder<Edge<UserDTO>> edges = ImmutableList.builder();

		for (IUser user : users.getItems()) {
			edges.add(new Edge<>(convertToUserDTO(user), new ConnectionCursor(user.getId().toString())));
		}

		rv.setEdges(edges.build());

		return rv;

	}

	private RelayConnection<GameDTO> convertToGameConnection(PagedList<IGame> games) {
		RelayConnection<GameDTO> rv = new RelayConnection<>();

		PageInfo pageInfo = new PageInfo();
		pageInfo.setHasNextPage(games.isHasNextPage());
		pageInfo.setHasPreviousPage(games.isHasPreviousPage());
		rv.setPageInfo(pageInfo);
		ImmutableList.Builder<Edge<GameDTO>> edges = ImmutableList.builder();

		for (IGame game : games.getItems()) {
			edges.add(new Edge<>(convertToGameDTO(game), new ConnectionCursor(game.getId().toString())));
		}

		rv.setEdges(edges.build());

		return rv;
	}
}
