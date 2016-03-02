package com.bretpatterson.schemagen.graphql.relay.manager;

import com.bretpatterson.schemagen.graphql.relay.IGameFactory;
import com.bretpatterson.schemagen.graphql.relay.IUserFactory;
import com.bretpatterson.schemagen.graphql.relay.dao.GameDAO;
import com.bretpatterson.schemagen.graphql.relay.model.IGame;
import com.bretpatterson.schemagen.graphql.relay.model.IUser;
import com.bretpatterson.schemagen.graphql.relay.model.PagedList;
import com.bretpatterson.schemagen.graphql.relay.model.impl.Game;
import com.bretpatterson.schemagen.graphql.relay.model.impl.User;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by bpatterson on 1/30/16.
 */
public class GameManager implements IGameFactory, IUserFactory {
	GameDAO gameDAO = new GameDAO();

	@Override
    public IGame newGame() {
		return new Game();
	}
	@Override
    public IUser newUser() {
		return new User();
	}

	public PagedList<IGame> findGames(Optional<Integer> first, Optional<Integer> last, Optional<Long> before, Optional<Long> after) {
		return gameDAO.findGames(first, last, before, after);
	}

	public PagedList<IUser> findUsers(Optional<Integer> first, Optional<Integer> last, Optional<Long> before, Optional<Long> after) {
		return gameDAO.findUsers(first, last, before, after);
	}

	public IUser createUser(IUser user) {
		checkNotNull(user.getName());
		checkNotNull(user.getEmail());

		return gameDAO.saveUser(user);
	}
	public IUser updateUser(IUser user) {
		checkNotNull(user.getName());
		checkNotNull(user.getEmail());

		return gameDAO.saveUser(user);
	}
	public void deleteUser(Long id) {
		gameDAO.removeUser(id);
	}

	public IGame createGame(IGame game) {
		checkNotNull(game.getName());

		return gameDAO.saveGame(game);
	}
	public IGame updateGame(IGame game) {
		checkNotNull(game.getId());
		checkNotNull(game.getName());

		return gameDAO.saveGame(game);
	}

	public void deleteGame(Long id) {
		gameDAO.removeGame(id);
	}


	public Optional<IGame> findGame(Long id) {
		return gameDAO.findGame(id);
	}

	public Optional<IUser> findUser(Long id) {
		return gameDAO.findUser(id);
	}
}
