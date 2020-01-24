# A hopefully comprehensible Pac-Man implementation using finite-state machines        

[![Pac-Man](https://i3.ytimg.com/vi/_3GhJGIOTp4/maxresdefault.jpg)](https://www.youtube.com/watch?v=_3GhJGIOTp4)

## Pac-Man? Really? How uncool!

My personal fascination for "Pac-Man" comes from the fact that the single computer game I played regularly was  ["Snack Attack"](https://www.youtube.com/watch?v=ivAZkuBbpsM), running on my Apple II+ clone in the mid-eighties, on a monochrome monitor, but what a sound came out of my crappy PC speaker! 

But also today, a seemingly simple game like Pac-Man can be very instructive from a programmer's point of view.
  
## The programming challenge
Implementing Pac-Man is challenging not because of the core game functionality like implementing a game loop, updating and drawing entities, handling collisions etc. but for others reasons:

First, implementing a good representation of the maze and the correct movement of the game characters 
through the maze are not trivial. Pac-Man's movement direction is controlled by the keyboard and the intended move direction can be selected already before Pac-Man actually can turn to that direction. 

After having implemented this correctly, the next challenge is the logic and the control of the game itself. You have to sort out the different states of the game and the actors, you have to understand how the user interface should behave depending on the current state and which game "events" lead from one state to the other (state transitions).

Maybe you will start with a single ghost and implement its behavior: waiting (bouncing) in the ghost house, leaving the house to chase Pac-Man or scattering out to the ghost's maze corner. What should happen when Pac-Man and a ghost are colliding? 
Which part of your program should coordinate this? Should the code be distributed over the actors or should you have 
some kind of mediator, some central game control? Where should the game rules (points, lives, levels etc.) be implemented? 
Should this be placed in some kind of *model* (in the sense of the Model-View-Controller pattern)?

I looked into existing code, for example [here](https://github.com/leonardo-ono/Java2DPacmanGame) or [here](https://github.com/yichen0831/Pacman_libGdx) or [here](https://github.com/urossss/Pac-Man) which I find not bad at all. But I wanted something different, namely an implementation where you can directly see the underlying state machines.

## State machines

There are many possibilities of implementing *finite state machines* in software: from basic switch-statements, function pointers (C, C++) to object-oriented "state pattern"-based implementations. There are also ready-to-use libraries like [Appcelerate](http://www.appccelerate.com/), [Stateless4j](https://github.com/oxo42/stateless4j) or [Squirrel](http://hekailiang.github.io/squirrel/). What should you do? 

The low-level implementations using switch-statements or function pointers (if your programming language supports this) are the most performant ones but as long a you achieve the performance goals for your game (60 frames/updates per second) you can use whatever you like. Of course, using  a higher-level implementation should make your code more readable and easier to maintain.

I decided to write my own [state machine implementation](https://github.com/armin-reichert/statemachine), which was a good exercise and really fun because of the availability of lambda expressions and method references.

After you have decided which implementation you want to use for your state machines you can finally focus on the game itself.

Which entities in the Pac-Man game are candidates for getting controlled by state machines?

Of course, Pac-Man and the four ghosts, but also the global game control, maybe also the screen selection logic or even simpler entities in your game. It is interesting to look at your program parts through the state machine glasses and find out where an explicit state machine becomes useful.

All state machines in this implementation are implemented in a declarative way (*builder pattern*). A single large Java expression defines the complete state graph together with node and edge annotations representing the actions, conditions, event conditions and timers. Lambda expressions (anonymous functions) and function references allow to embed code directly inside the state machine definition. If the state definition becomes more complex it is possible to implement it in a separate state class. Both variants are used here.

## State machines in action

Sounds all well and nice, but how does that look in the real code? To give a first example how the state machine implementations look like, let's consider the **intro screen** ([IntroView](PacManGame/src/main/java/de/amr/games/pacman/view/intro/IntroView.java)) which shows different animations that have to be coordinated using timers and conditions. As this state machine only uses timers and no other events, we can use *Void* as the event type. The states are identified using an enumeration type.

```java
beginStateMachine(IntroState.class, Void.class)
	.description(String.format("[%s]", name))
	.initialState(SCROLLING_LOGO)

	.states()

		.state(SCROLLING_LOGO)
			.onEntry(() -> {
				theme.snd_insertCoin().play();
				pacManLogo.tf.setY(height());
				pacManLogo.tf.setVelocityY(-2f);
				pacManLogo.setCompletion(() -> pacManLogo.tf.getY() <= 20);
				pacManLogo.show(); 
				pacManLogo.start(); 
			})
			.onTick(() -> {
				pacManLogo.update();
			})

		.state(SHOWING_ANIMATIONS)
			.onEntry(() -> {
				chasePacMan.setStartPosition(width(), 100);
				chasePacMan.setEndPosition(-chasePacMan.tf.getWidth(), 100);
				chaseGhosts.setStartPosition(-chaseGhosts.tf.getWidth(), 200);
				chaseGhosts.setEndPosition(width(), 200);
				chasePacMan.start();
				chaseGhosts.start();
			})
			.onTick(() -> {
				chasePacMan.update();
				chaseGhosts.update();
			})
			.onExit(() -> {
				chasePacMan.stop();
				chaseGhosts.stop();
				chasePacMan.tf.centerX(width());
			})

		.state(WAITING_FOR_INPUT)
			.timeoutAfter(sec(10))
			.onEntry(() -> {
				ghostPointsAnimation.tf.setY(200);
				ghostPointsAnimation.tf.centerX(width());
				ghostPointsAnimation.start();
				gitHubLink.show();
			})
			.onTick(() -> {
				ghostPointsAnimation.update();
			})
			.onExit(() -> {
				ghostPointsAnimation.stop();
				ghostPointsAnimation.hide();
				gitHubLink.hide();
			})

		.state(READY_TO_PLAY)

	.transitions()

		.when(SCROLLING_LOGO).then(SHOWING_ANIMATIONS)
			.condition(() -> pacManLogo.isComplete())

		.when(SHOWING_ANIMATIONS).then(WAITING_FOR_INPUT)
			.condition(() -> chasePacMan.isComplete() && chaseGhosts.isComplete())

		.when(WAITING_FOR_INPUT).then(SHOWING_ANIMATIONS)
			.onTimeout()

		.when(WAITING_FOR_INPUT).then(READY_TO_PLAY)
			.condition(() -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE))

.endStateMachine();
```

A more complex state machine is used for implementing the **global game controller** ([GameController](PacManGame/src/main/java/de/amr/games/pacman/controller/GameController.java)). It processes game events which
are created during the game play, for example when Pac-Man finds food or meets ghosts. Also the different
game states like changing the level or the dying animations of Pac-Man and the ghosts are controlled by this
state machine. Further, the more complex states are implemented as subclasses of the generic `State` class. This
has the advantage that actions which are state-specific can be realized as methods of the state subclass.

The **ghost attack waves** (scattering, chasing) with their level-specific timing are realized by the following state machine:

See [GhostCommand](PacManGame/src/main/java/de/amr/games/pacman/controller/GhostCommand.java)

```java
beginStateMachine()
	.description("[GhostCommand]")
	.initialState(SCATTERING)
.states()
	.state(SCATTERING)
		.timeoutAfter(() -> game.level.scatterTicks(round))
	.state(CHASING)
		.timeoutAfter(() -> game.level.chasingTicks(round))
		.onExit(() -> ++round)
.transitions()
	.when(SCATTERING).then(CHASING).onTimeout()
	.when(CHASING).then(SCATTERING).onTimeout()
.endStateMachine();
```

The actors in this implementation are also controlled by finite-state machines:

**Pac-Man** ([Pac-Man](PacManGame/src/main/java/de/amr/games/pacman/actor/PacMan.java))

The **ghosts** ([Ghost](PacManGame/src/main/java/de/amr/games/pacman/actor/Ghost.java))

Even a simple entity like the **bonus symbol** ([Bonus](PacManGame/src/main/java/de/amr/games/pacman/actor/Bonus.java)) which appears at certain scores uses a finite-state machine to implement its lifecycle:

```java
beginStateMachine(BonusState.class, PacManGameEvent.class)
	.description(String.format("[%s]", name))
	.initialState(ACTIVE)
	.states()
		.state(ACTIVE)
			.timeoutAfter(() -> sec(9 + new Random().nextFloat()))
			.onEntry(() -> sprites.select("symbol"))
		.state(CONSUMED)
			.timeoutAfter(sec(3))
			.onEntry(() -> sprites.select("number"))
		.state(INACTIVE)
			.onEntry(cast::removeBonus)
	.transitions()
		.when(ACTIVE).then(CONSUMED).on(BonusFoundEvent.class)
		.when(ACTIVE).then(INACTIVE).onTimeout()
		.when(CONSUMED).then(INACTIVE).onTimeout()
.endStateMachine();
```

When an actor leaves the board inside a tunnel it enters *teleporting* mode. In this implementation, the teleporting duration can be specified for each actor individually (no idea if this makes much sense) and the movement state of an actor is controlled by the following state machine:

```java
movement = new StateMachine<MoveState, Void>(MoveState.class) {

	{
		//@formatter:off
		beginStateMachine()
			.description(String.format("[%s movement]", name))
			.initialState(MOVING)
			.states()
				.state(MOVING)
					.onTick(MovingActor.this::moveInsideMaze)
				.state(TELEPORTING)
					.timeoutAfter(() -> teleportingTicks)
					.onEntry(() -> setVisible(false))
					.onExit(() -> setVisible(true))
			.transitions()
				.when(MOVING).then(TELEPORTING)
					.condition(() -> enteredLeftPortal() || enteredRightPortal())
				.when(TELEPORTING).then(MOVING)
					.onTimeout().act(MovingActor.this::teleport)
		.endStateMachine();
		//@formatter:on
	}
};
```

Using an explicit state machine for such a simple control case may seem like shooting at sparrows with cannons but serves to illustrate how seamlessly state machines can be integrated in the code.

## Tracing

The processing of all used state machines can be traced. If a state machine processes an event and does not 
find a suitable state transition, a runtime exception is thrown by default. This is very useful for finding 
gaps in the state machine definition in the development stage. Afterwards, this behavior can be changed so
that only a message is logged for unhandled events. This avoids the need for specifying "empty" transitions
for any event that has no effect in the current state. The Ghost's state machine makes use of that feature.

## Pac-Man steering

Pac-Man is steered by holding a key indicating its **intended** direction. As soon as Pac-Man reaches a tile where it can move towards this direction it changes its move direction accordingly. ("Cornering" is not yet implemented). In the code, this is implemented by setting the steering function as shown below. This makes it very easy to replace the manual steering by some sort of automatic steering ("AI"):

```java
pacMan.steering(pacMan.isFollowingKeys(VK_UP, VK_RIGHT, VK_DOWN, VK_LEFT));

default Steering isFollowingKeys(int... keys) {
	return () -> Direction.dirs()
			.filter(dir -> Keyboard.keyDown(keys[dir.ordinal()]))
			.findAny()
			.ifPresent(this::setWishDir);
}
```

## Ghost steering ("AI")

What makes the game so entertaining is the individual behavior of each ghost when chasing Pac-Man. The red ghost (Blinky) attacks Pac-Man directly, the pink ghost (Pinky) tries to ambush Pac-Man, the orange ghost (Clyde) either attacks directly or rejects, depending on its distance to Pac-Man, and finally the pink ghost (Pinky) uses Blinky's current position to get in Pac-Man's way. 

To realize these different ghost behaviors each ghost has a map of functions mapping each state (*scattering*, *chasing*, *frightened*, ...) to the corresponding behavior implementation. In terms of OO design patterns, one could call this a *strategy pattern*. 

<img src="PacManDoc/pacman.png"/>

The ghost behavior only differs for the *chasing* state namely in the logic for calculating the target tile. Beside the different target tiles, the ghost behavior is equal. Each ghost uses the same algorithm to calculate the next move direction to take for reaching the target tile as described in the references given at the end of this article.

The *frightened* behavior has two different implementations (just as a demonstration how the behavior can be exchanged during the game) and can be toggled for all ghosts at once by pressing the 'f'-key.

### Blinky (the red ghost)

Blinky's chasing behavior is to directly attack Pac-Man:

```java
blinky.behavior(ENTERING_HOUSE, blinky.isTakingSeat(seatPosition(2)));
blinky.behavior(LEAVING_HOUSE, blinky.isLeavingGhostHouse());
blinky.behavior(FRIGHTENED, blinky.isMovingRandomlyWithoutTurningBack());
blinky.behavior(SCATTERING, blinky.isHeadingFor(game().maze().horizonNE));
blinky.behavior(CHASING, blinky.isHeadingFor(pacMan::tile));
blinky.setTeleportingDuration(sec(0.5f));
```
<img src="PacManDoc/blinky.png"/>

### Inky (the cyan ghost)

Inky heads for a position that depends on Blinky's current position and the position two tiles ahead of Pac-Man:

Consider the vector `V` from Blinky's position `B` to the position `P` two tiles ahead of Pac-Man, so `V = (P - B)`. 
Add the doubled vector to Blinky's position: `B + 2 * (P - B) = 2 * P - B` to get Inky's target:

```java
inky.behavior(LOCKED, inky.isJumpingUpAndDown(seatPosition(1)));
inky.behavior(ENTERING_HOUSE, inky.isTakingSeat(seatPosition(1)));
inky.behavior(LEAVING_HOUSE, inky.isLeavingGhostHouse());
inky.behavior(FRIGHTENED, inky.isMovingRandomlyWithoutTurningBack());
inky.behavior(SCATTERING, inky.isHeadingFor(game().maze().horizonSE));
inky.behavior(CHASING, inky.isHeadingFor(() -> {
	Tile b = blinky.tile(), p = pacMan.tilesAhead(2);
	return game().maze().tileAt(2 * p.col - b.col, 2 * p.row - b.row);
}));
inky.setTeleportingDuration(sec(0.5f));
```

<img src="PacManDoc/inky.png"/>

### Pinky

Pinky, the *ambusher*, heads for the position 4 tiles ahead of Pac-Man's current position. In the original game there is an overflow error leading to a different behavior: when Pac-Man looks upwards, the tile ahead of Pac-Man is falsely computed with an additional number of steps to the west. This behavior is active by default and can be toggled using the 'o'-key.

```java
pinky.behavior(LOCKED, pinky.isJumpingUpAndDown(seatPosition(2)));
pinky.behavior(ENTERING_HOUSE, pinky.isTakingSeat(seatPosition(2)));
pinky.behavior(LEAVING_HOUSE, pinky.isLeavingGhostHouse());
pinky.behavior(FRIGHTENED, pinky.isMovingRandomlyWithoutTurningBack());
pinky.behavior(SCATTERING, pinky.isHeadingFor(game().maze().horizonNW));
pinky.behavior(CHASING, pinky.isHeadingFor(() -> pacMan.tilesAhead(4)));
pinky.setTeleportingDuration(sec(0.5f));
```

<img src="PacManDoc/pinky.png"/>

### Clyde (the orange ghost)

Clyde attacks Pac-Man directly (like Blinky) if his straight line distance from Pac-Man is more than 8 tiles. If closer, he behaves like in scattering mode.

```java
clyde.behavior(LOCKED, clyde.isJumpingUpAndDown(seatPosition(3)));
clyde.behavior(ENTERING_HOUSE, clyde.isTakingSeat(seatPosition(3)));
clyde.behavior(LEAVING_HOUSE, clyde.isLeavingGhostHouse());
clyde.behavior(FRIGHTENED, clyde.isMovingRandomlyWithoutTurningBack());
clyde.behavior(SCATTERING, clyde.isHeadingFor(game().maze().horizonSW));
clyde.behavior(CHASING, clyde.isHeadingFor(
		() -> Tile.distanceSq(clyde.tile(), pacMan.tile()) > 8 * 8 ? pacMan.tile() : game().maze().horizonSW));
clyde.setTeleportingDuration(sec(0.5f));
```

<img src="PacManDoc/clyde.png"/>

The visualization of the attack behaviors can be toggled during the game by pressing the 'r'-key ("show/hide routes").

### Scattering

In *scattering* mode, each ghost tries to reach his "scattering target" which is a tile outside of the maze. Because ghosts
cannot reverse direction this results in a cyclic movement around the walls in the corresponding corner of the maze.

<img src="PacManDoc/scattering.png"/>

## Graph-based pathfinding

The original Pac-Man game did not use any graph-based pathfinding. To still give an example how graph-based pathfinding can be useful, there is an additional implementation of the *frightened* behavior: when Pac-Man eats a power-pill each frightened ghost choses the "safest" corner to flee to. It computes the shortest path to each corner and selects the one with the largest distance to Pac-Man's current position. Here, the distance of a path from Pac-Man's position is defined as the minimum distance of any tile on the path from Pac-Man's position.

The wrapper class `MazeGraph`adds a graph structure (grid graph) to the maze. This allows running the graph algorithms from my [graph library](https://github.com/armin-reichert/graph) on the maze. For example, shortest paths in the maze can then be computed by just calling the *findPath(Tile source, Tile target)* method on the maze graph. This method runs an [A* search](http://theory.stanford.edu/~amitp/GameProgramming/AStarComparison.html) on the underlying graph. The graph library provides a whole number of search algorithms like BFS or Dijkstra. The code to compute a shortest path between two tiles using the A* algorithm with Manhattan distance heuristics looks like this:

```java
GraphSearch pathfinder = new AStarSearch(grid, (u, v) -> 1, grid::manhattan);
Path path = pathfinder.findPath(vertex(source), vertex(target));
```
However, for a graph of such a small size, the used algorithm doesn't matter very much, a Breadth-First Search would also run with sufficient performance in this use case.

## Additional features

- The following settings can be changed via command-line arguments (`java -jar pacman.jar arguments...`):
  - Scaling: e.g. `-scale 2.5`
  - Full-screen mode on start: `-fullScreenOnStart`
  - Full-screen resolution & depth: e.g. `-fullScreenMode 800,600,32`
  - Window title: e.g. `-title "Pac-Man Game"`
- General
  - CTRL-p pauses/resumes the game
  - F2 opens a dialog where the game loop frequency and (full-)screen resolution can be changed
  - F11 toggles between window and full-screen exclusive mode
- Game
  - Command-line parameters:
    - `-skipIntro`: skips intro screen
    - `-overflowBug true/false`: simulates overflow bug from Arcade version
    - `-pacManImmortable`: Pac-Man keeps live after being killed
  - The overall speed can be changed during the game; 
    - Continuosly: CTRL-LEFT = slower, CTRL-RIGHT = faster
    - Fixed speeds: '1' = normal speed, '2' = fast, '3' = very fast
  - 'b' toggles the presence of Blinky
  - 'c' toggles the presence of Clyde
  - 'd' toggles between normal play mode and demo mode where Pac-Man moves randomly
  - 'e' eats all pellets except the energizers
  - 'f' toggles the ghost's *frightened* behavior between "random" (original) and "select safe corner"
  - 'g' toggles the display of the grid and the alignment of the actors on the grid
  - 'i' toggles the presence of Inky
  - 'k' kills all ghosts
  - 'l' toggles the tracing of the used state machines
  - 'm' makes Pac-Man immortable (does not lose live after being killed)
  - 'o' toggles the simulation of the overflow bug which occurs in the original Arcade game when Pac-Man is looking upwards
  - 'p' toggles the presence of Pinky
  - 'r' toggles the display of actor routes and target tiles
  - 's' toggles the display of actor states and timers
  - 't' toggles display of timing information (target vs. actual framerate)
  - '+' switches to the next level

## References

This work would not have been possible without these invaluable sources of information:

- [GameInternals - Understanding Pac-Man Ghost Behavior](http://gameinternals.com/post/2072558330/understanding-pac-man-ghost-behavior)
- [Gamasutra - The Pac-Man Dossier](http://www.gamasutra.com/view/feature/3938/the_pacman_dossier.php)

Just recently I found this excellent video on YouTube:

- [Pac-Man ghost AI explained](https://www.youtube.com/watch?v=ataGotQ7ir8)

## Summary

The goal of this project is to provide a [Pac-Man](https://en.wikipedia.org/wiki/List_of_Pac-Man_video_games) implementation in which the game's inner workings can be understood from the code more easily. The implementation follows the MVC pattern and uses *finite state machines* for the control logic of the actors and the game. The state machines are implemented in a declarative way using the *builder* pattern. 

A home-grown library is used for the basic game infrastructure (active rendering, game loop, full-screen mode, 
keyboard and mouse handling etc.), but it should be not too difficult to implement these parts from scratch or 
use some real game library instead.

It could be useful to further decouple UI, model and controller to enable an easy replacement of the complete UI 
or to implement the state machines using some other state machine library. 

*Armin Reichert, November 2019*
