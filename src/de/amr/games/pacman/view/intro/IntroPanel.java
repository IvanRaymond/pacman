package de.amr.games.pacman.view.intro;

import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Set;

import de.amr.easy.game.entity.GameEntity;
import de.amr.easy.game.view.ViewController;
import de.amr.statemachine.StateMachine;

public class IntroPanel implements ViewController {

	private final int width;
	private final int height;
	private final StateMachine<Integer, Void> animation;
	private Set<GameEntity> entities = new HashSet<>();
	private Title title;
	private Text startText;
	private GhostsChasingPacMan ghostsChasingPacMan;
	private PacManChasingGhosts pacManChasingGhosts;

	public IntroPanel(int width, int height) {
		this.width = width;
		this.height = height;
		animation = buildStateMachine();
	}

	private StateMachine<Integer, Void> buildStateMachine() {
		return
		/*@formatter:off*/
		StateMachine.define(Integer.class, Void.class)
			.description("")
			.initialState(0)
	
			.states()
			
					.state(0) // scroll title image from bottom into view
						.onEntry(() -> {
							entities.add(title = new Title());
							title.start();
						})
						
					.state(1) // ghosts chasing Pac-Man
						.onEntry(() -> {
							entities.add(ghostsChasingPacMan = new GhostsChasingPacMan());
							ghostsChasingPacMan.start();
						})
						.onExit(() -> entities.remove(ghostsChasingPacMan))
						
					.state(2) // Pac-Man chasing ghosts
						.onEntry(() -> {
							entities.add(pacManChasingGhosts = new PacManChasingGhosts());
							pacManChasingGhosts.start();
						})
						.onExit(() -> entities.remove(pacManChasingGhosts))
						
					.state(3) // wait
					.onEntry(() -> {
						entities.add(startText = new Text("Press SPACE to start!", 16));
						startText.center(width, height);
					})
					
			.transitions()

					.when(0).then(1)
						.condition(() -> title.tf.getY() < 10)
						.act(() -> title.stop())
						
					.when(1).then(2).condition(() -> ghostsChasingPacMan.isComplete())
						
					.when(2).then(3).condition(() -> pacManChasingGhosts.isComplete())
				
		.endStateMachine();
	  /*@formatter:on*/
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void draw(Graphics2D g) {
		entities.forEach(e -> e.draw(g));
	}

	@Override
	public void init() {
		animation.init();
	}

	@Override
	public void update() {
		animation.update();
		entities.forEach(GameEntity::update);
	}
}