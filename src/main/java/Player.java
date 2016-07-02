import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {
	
	public static int MAP_WIDTH = 16001;
	public static int MAP_HEIGHT = 9001;
	public static int MAX_BUST_RANGE = 1760;
	public static int MIN_BUST_RANGE = 900;
	public static int MAX_RELEASE_RANGE = 1600;
	public static int VIEW_RANGE = 2200;
	
	private static Entity base;
	private static int bustersPerPlayer;
	
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right
        
        base = new Entity(myTeamId);
        
        System.err.println("bustersPerPlayer : " + bustersPerPlayer);
        System.err.println("ghostCount : " + ghostCount);
        System.err.println("myTeamId : " + myTeamId);
        
        // game loop
        while (true) {
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            System.err.println("I can see " + entities + " entities");
            List<Entity> myBusters = new ArrayList<>();
            List<Entity> ennBusters = new ArrayList<>();
            List<Entity> ghosts = new ArrayList<>();
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.
                Entity e = new Entity(entityId, x, y, entityType, state, value);
                if (entityType != -1) {
                	if (entityType == myTeamId) {
                		//Modify the id to be able to have number from 0 to bustersPerPlayer for my players
                    	e.id = entityType == 0 ? entityId : entityId - bustersPerPlayer;
                		myBusters.add(e);
                	} else ennBusters.add(e);                	
                } else ghosts.add(e);
            }
            System.err.println("myBuster size : " + myBusters.size());
            System.err.println("ennBuster size : " + ennBusters.size());
            System.err.println("ghosts size : " + ghosts.size());
            for (int i = 0; i < bustersPerPlayer; i++) {
            	Entity currentBuster = myBusters.get(i);
            	Action action = getAction(currentBuster, ghosts, ennBusters);
            	action.print();
            }
        }
    }

	private static Action getAction(Entity currentBuster, List<Entity> ghosts,
			List<Entity> ennBusters) {
		Action a = null;
		// Buster with ghost need to return to base
		if (currentBuster.state == 1) {
			Boolean isReleasePossible = isRealeasePossible(currentBuster, base);
			if (isReleasePossible) {
				return new Action("RELEASE");
			}
			return new Action("MOVE", base.x, base.y);
		}
		
		//Get action to stun an enemy with a ghost
		a = getEnemy(currentBuster, ennBusters, ghosts);
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

	private static Action getEnemy(Entity buster, List<Entity> enemies, List<Entity> ghosts) {
		if (!enemies.isEmpty()) {
			Entity closestEnemy = getClosestEnnemy(buster, enemies);
			Entity closestGhost = getClosestGhost(buster, ghosts);
			if (closestEnemy!= null && 
					(closestEnemy.state == 1 || closestEnemy.state == 3)) {
				return new Action("STUN", closestEnemy.id);
			}
		}
		return null;
	}

	private static Action getGhost(Entity currentBuster, List<Entity> ghosts) {
		if (!ghosts.isEmpty()) {
			Entity closestGhost = getClosestGhost(currentBuster, ghosts);
			if (closestGhost == null) {
				System.err.println("No closest ghost for buster : " + currentBuster.id);
				return null;
			}
			System.err.println("Closest ghost for buster : " + currentBuster.id	+ " is ghost : " + closestGhost.id);
			Boolean isBustingPossible = isBustingPossible(currentBuster, closestGhost);
			System.err.println("isBustPossible : " + isBustingPossible);
			if (isBustingPossible) {
				return new Action("BUST", closestGhost.id);
			} 
			return new Action("MOVE", closestGhost.x, closestGhost.y);
		}
		
		return null;
	}

	private static Action discoverBase(Entity buster) {
		double discoverZoneSize = MAP_HEIGHT / bustersPerPlayer;
		int posYInZone = (int) ((discoverZoneSize * (buster.id + 1)) - (discoverZoneSize / 2));
		int posXInZone = base.x == 0 ? MAP_WIDTH - VIEW_RANGE : 0 + VIEW_RANGE;
		if (buster.x != posXInZone) {
			if (buster.y != posYInZone) {
				return new Action("MOVE", buster.x, posYInZone);
			} 
			System.err.println("GO TO ZONE");
			return new Action ("MOVE", posXInZone, posYInZone);
		}
		System.err.println("GO TO ENNEMIE BASE");
		int ennemieBaseY = base.y == 0 ? MAP_HEIGHT - MAX_RELEASE_RANGE: 0 + MAX_RELEASE_RANGE;
		return new Action ("MOVE", posXInZone, ennemieBaseY);
	}

	private static Entity getClosestGhost(Entity buster, List<Entity> ghosts) {
		double minDist = Double.MAX_VALUE;
		Entity closestEntity = null; 
		for (Entity ghost : ghosts) {
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
		return closestEntity;
	}
	
	private static Entity getClosestEnnemy(Entity buster, List<Entity> ennemies) {
		double minDist = Double.MAX_VALUE;
		Entity closestEntity = null; 
		for (Entity ennemy : ennemies) {
			double distance = getDistance(buster, ennemy);
			//if distance is greater than MAX_BUST_RANGE that's mean that our buster is too far away to attack any ennemy
			if (distance > MAX_BUST_RANGE) continue;
			if (distance < minDist) {
				closestEntity = ennemy;
				minDist = distance;
			}
		}
		return closestEntity;
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
	
    public Entity(int entityId, int x, int y, int entityType, int state,
			int value) {
		this.id = entityId;
		this.x = x;
		this.y = y;
		this.type = entityType;
		this.state = state;
		this.value = value;
	}

	public Entity(int id) {
		if (id == 0) {
			this.x = 0;
			this.y = 0;
		} else {
			this.x = 16000;
			this.y = 9000;			
		}
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
