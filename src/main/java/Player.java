import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {
	
	public static final int MAP_WIDTH = 16001;
	public static final int MAP_HEIGHT = 9001;
	public static final int MAX_BUST_RANGE = 1760;
	public static final int MIN_BUST_RANGE = 900;
	public static final int MAX_RELEASE_RANGE = 1600;
	public static final int VIEW_RANGE = 2200;
	public static final int TIME_FOR_RECHARGE_WEAPON = 20;
	
	private static Entity base;
	private static int bustersPerPlayer;
	private static List<Buster> busters;
	private static List<Entity> enemies;
	private static List<Entity> ghosts;
	
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right
        int enemyId = myId == 0 ? 1 : 0;
        
        busters = initBustersList(myId, bustersPerPlayer);
        enemies = initEntitiesList(bustersPerPlayer, enemyId);
        ghosts = initEntitiesList(ghostCount, -1);
        base = setupBase(myId);
        
        while (true) {
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            resetVisibility(enemies);
            resetVisibility(ghosts);
            refreshBusters(busters);
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt();
                int x = in.nextInt();
                int y = in.nextInt();
                int entityType = in.nextInt();
                int state = in.nextInt();
                int value = in.nextInt();
                setupEntity(myId, enemyId, entityId, x, y, entityType, state,
						value);
            }

            for (int i = 0; i < bustersPerPlayer; i++) {
            	Buster currentBuster = busters.get(i);
            	Action action = getAction(currentBuster);
            	action.print();
            }
            
        }
    }
    
    //Refresh all buster, decrease time for charging and reset target 
    private static void refreshBusters(List<Buster> busters) {
    	for (Buster b : busters) {
    		b.refresh();
    	}
	}

	//Reset the visibility of all entities 
    private static void resetVisibility(List<Entity> entities) {
    	for (Entity e : entities) {
    		e.isVisible = false;
    	}
	}

	//Setup base coordinate based on my team id
	private static Entity setupBase(int myId) {
		Entity base = new Entity(-1);
		if (myId == 0) {
			base.x = 0;
			base.y = 0;
		} else {
			base.x = 16000;
			base.y = 9000;
		}
		return base;
	}

	private static void setupEntity(int myId, int enemyId, int entityId, int x,
			int y, int entityType, int state, int value) {
		Entity e = null;
		if (entityType == -1) {
			System.err.println("Setup ghost : " + entityId);
			e = ghosts.get(entityId);
		} else {
		    if (entityType == myId) {
		    	e = busters.get(getNormalizedId(myId, entityId));
		    } else {
		    	e = enemies.get(getNormalizedId(enemyId, entityId));
		    }
		}
		e.setProperties(x, y, entityType, state, value);
	}
    
    private static int getNormalizedId(int teamId, int entityId) {
    	int normalizedId = teamId == 0 ? entityId : entityId - bustersPerPlayer;
    	return normalizedId;
	}

	private static List<Buster> initBustersList(int myTeamId,
			int nbBusters) {
		List<Buster> bustersList = new ArrayList<>(nbBusters);
		for (int i = 0; i < nbBusters; i++) {
			int id = myTeamId == 0 ? i : i + nbBusters;
			int normalizedId = myTeamId == 0 ? id : id - nbBusters; 
			Buster b = new Buster(id, normalizedId);
			bustersList.add(b);
		}
		return bustersList;
	}

	private static List<Entity> initEntitiesList(int nbEntities, int teamId) {
		List<Entity> entities = new ArrayList<>(nbEntities);
		for (int i = 0; i < nbEntities; i++) {
			int id = i;
			//if we setup enemies list
			if (teamId != -1) {
				//We need to have the real id of the enemy buster
				id = teamId == 0 ? i : i + nbEntities;
			}
			Entity b = new Entity(id); 
			entities.add(b);
		}
		return entities;
	}

	private static Action getAction(Buster currentBuster) {
		Action a = null;
		// Buster with ghost need to return to base
		if (currentBuster.state == 1) {
			Boolean isReleasePossible = isRealeasePossible(currentBuster, base);
			if (isReleasePossible) {
				return new Action("RELEASE");
			}
			return new Action("MOVE", base.x, base.y);
		}
		
		//Get action to stun an enemy
		a = getEnemy(currentBuster, enemies, ghosts);
		if (a == null) {
			//Get action to bust or move to catch a ghost
			a = getGhost(currentBuster, ghosts);
			if (a == null) {
				//if no action possible go discover base;
				return discoverBase(currentBuster);				
			}
		}
		
		return a;
	}

	private static Action getEnemy(Buster buster, List<Entity> enemies, List<Entity> ghosts) {
		if (!enemies.isEmpty()) {
			List<Entity> enemiesToStun = getTargets(buster, enemies);
			for (Entity enemy : enemiesToStun) {
				boolean isNotATarget = isNotATarget(enemy);
				if (enemy.state != 2 && isNotATarget && buster.rechargeWeapon == 0) {
					System.err.println("Target for : " + buster.id + " is enemy " + enemy.id);
					buster.fire(enemy.id);
					return new Action("STUN", enemy.id);
				}
			}
		}
		return null;
	}

	private static boolean isNotATarget(Entity enemy) {
		for (Buster buster : busters) {
			if (buster.targetId == enemy.id) {
				return false;
			}
		}
		return true;
	}

	private static Action getGhost(Entity currentBuster, List<Entity> ghosts) {
		if (!ghosts.isEmpty()) {
			Entity closestGhost = getClosestGhost(currentBuster, ghosts);
			if (closestGhost == null) {
				return null;
			}
			Boolean isBustingPossible = isBustingPossible(currentBuster, closestGhost);
			if (isBustingPossible) {
				return new Action("BUST", closestGhost.id);
			} 
			return new Action("MOVE", closestGhost.x, closestGhost.y);
		}
		
		return null;
	}

	private static Action discoverBase(Buster buster) {
		
		double discoverZoneSizeX = MAP_WIDTH / bustersPerPlayer;
		double discoverZoneSizeY = MAP_HEIGHT / bustersPerPlayer;
		int posXInZone = (int) ((discoverZoneSizeX * (buster.normalizedId + 1)) - (discoverZoneSizeX / 2));
		int posYInZone = MAP_HEIGHT - (int) ((discoverZoneSizeY * (buster.normalizedId + 1)) - (discoverZoneSizeY / 2));
		//int posXInZone = base.x == 0 ? MAP_WIDTH - VIEW_RANGE : 0 + VIEW_RANGE;
		return new Action ("MOVE", posXInZone, posYInZone);
//		if (buster.x != posXInZone) {
//			if (buster.y != posYInZone) {
//				return new Action("MOVE", buster.x, posYInZone);
//			} 
//			System.err.println("GO TO ZONE");
//			return new Action ("MOVE", posXInZone, posYInZone);
//		}
//		System.err.println("GO TO ENNEMIE BASE");
//		int ennemieBaseY = base.y == 0 ? MAP_HEIGHT - MAX_RELEASE_RANGE: 0 + MAX_RELEASE_RANGE;
//		return new Action ("MOVE", posXInZone, ennemieBaseY);
	}

	private static Entity getClosestGhost(Entity buster, List<Entity> ghosts) {
		double minDist = Double.MAX_VALUE;
		Entity closestEntity = null; 
		for (Entity ghost : ghosts) {
			if (ghost.isVisible) {
				double distance = getDistance(buster, ghost);
				//if distance is greater than view_range that's mean that our buster is too far away
				//if the entity is a ghost and the distance is < than MIN_BUST_RANGE then we can't catch it
				//if (distance > VIEW_RANGE || (entity.type == -1 && distance < MIN_BUST_RANGE)) continue;
				if (distance > VIEW_RANGE * 2) continue;
				if (distance < minDist) {
					closestEntity = ghost;
					minDist = distance;
				}
			}
		}
		return closestEntity;
	}
	
	private static List<Entity> getTargets(Entity buster, List<Entity> enemies) {
		List<Entity> targets = new ArrayList<>();
		for (Entity enemy : enemies) {
			if (enemy.isVisible) {
				double distance = getDistance(buster, enemy);
				//if distance is greater than MAX_BUST_RANGE that's mean that our buster is too far away to attack any enemy
				if (distance <= MAX_BUST_RANGE) {
					targets.add(enemy);
				}
			}
		}
		return targets;
	}


	private static double getDistance(Entity a, Entity b) {
		double d = Math.pow(Math.abs(a.x - b.x), 2) + Math.pow(Math.abs(a.y - b.y), 2);
		return Math.sqrt(d);
	}

	private static Boolean isBustingPossible(Entity currentBuster,
			Entity closestGhost) {
		double dist = getDistance(currentBuster, closestGhost);
		return (dist < MAX_BUST_RANGE && dist > MIN_BUST_RANGE);
	}
	
	private static Boolean isRealeasePossible(Entity currentBuster, Entity base) {
		double dist = getDistance(currentBuster, base);
		return dist < MAX_RELEASE_RANGE;
	}
	
}

class Entity {
	
	public int id;
	public int x;
	public int y;
	public int type;
    public int state;
    public int value;
    public boolean isVisible = false;

	public void setProperties(int x, int y, int entityType, int state,
			int value) {
		this.x = x;
		this.y = y;
		this.type = entityType;
		this.state = state;
		this.value = value;
		this.isVisible = true;
	}

	public Entity(int id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Entity other = (Entity) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
}

class Buster extends Entity {
	
	int rechargeWeapon = 0;
	int targetId = -1;
	int normalizedId;

	public Buster(int id) {
		super(id);
	}
	
	public Buster(int id, int normalizedId) {
		super(id);
		this.normalizedId = normalizedId;
	}

	public void fire(int targetId) {
		this.rechargeWeapon = Player.TIME_FOR_RECHARGE_WEAPON;
		this.targetId = targetId;
	}
	
	public void refresh() {
		rechargeWeapon = rechargeWeapon > 0 ? rechargeWeapon - 1 : 0;
		targetId = -1;
	}
	
}

class Action {
	
	public String name;
	public List<Integer> params = new ArrayList<>();
	
	public Action(String name) {
		this.name = name;
	}

	public Action(String name, int... params) {
		this.name = name;
		for (int param : params) {
			this.params.add(param);
		}
	}

	public void print() {
		System.out.print(this.name);
		for (int param : this.params) {
			System.out.print(" " + param);
		}
		System.out.print(System.getProperty("line.separator"));
	}
}
