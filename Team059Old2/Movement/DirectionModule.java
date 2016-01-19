package Team059Old2.Movement;

import battlecode.common.*;

import java.util.Random;

public class DirectionModule {

    public static final Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
    private Random random = null;

    public DirectionModule(int randomSeed) {

        this.random = new Random(randomSeed);

    }

    public Direction randomDirection() {

        return DirectionModule.directions[this.random.nextInt(DirectionModule.directions.length)];

    }

    // checks D, D.L, D.R, D.LL, D.RR for availability
    public Direction recommendedMovementDirectionForDirection(final Direction direction, final RobotController robotController, boolean use90) {

        if (robotController.canMove(direction)) {

            return direction;

        }

        final boolean divisible = robotController.getID() % 2 == 0;
        Direction movementDirection = divisible ? direction.rotateLeft() : direction.rotateRight();
        if (robotController.canMove(movementDirection)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight() : direction.rotateLeft();
        if (robotController.canMove(movementDirection)) {

            return movementDirection;

        }
        if (use90) {

            return null;

        }
        movementDirection = divisible ? direction.rotateLeft().rotateLeft() : direction.rotateRight().rotateRight();
        if (robotController.canMove(movementDirection)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight().rotateRight() : direction.rotateLeft().rotateLeft();
        if (robotController.canMove(movementDirection)) {

            return movementDirection;

        }
        return null;

    }

    // will do the same as recommendedMovementDirectionForDirection but will not move within 3 squares of a wall
    public Direction recommendedFleeDirectionForDirection(final Direction direction, final RobotController robotController, boolean use90) throws GameActionException {

        final MapLocation currentLocation = robotController.getLocation();

        if (robotController.canMove(direction) && robotController.onTheMap(currentLocation.add(direction, 3))) {

            return direction;

        }

        final boolean divisible = robotController.getID() % 2 == 0;
        Direction movementDirection = divisible ? direction.rotateLeft() : direction.rotateRight();
        if (robotController.canMove(movementDirection) && robotController.onTheMap(currentLocation.add(movementDirection, 3))) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight() : direction.rotateLeft();
        if (robotController.canMove(movementDirection) && robotController.onTheMap(currentLocation.add(movementDirection, 3))) {

            return movementDirection;

        }
        if (use90) {

            return null;

        }
        movementDirection = divisible ? direction.rotateLeft().rotateLeft() : direction.rotateRight().rotateRight();
        if (robotController.canMove(movementDirection) && robotController.onTheMap(currentLocation.add(movementDirection, 3))) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight().rotateRight() : direction.rotateLeft().rotateLeft();
        if (robotController.canMove(movementDirection) && robotController.onTheMap(currentLocation.add(movementDirection, 3))) {

            return movementDirection;

        }
        return null;

    }

    // will do the same as recommendedMovementDirectionForDirection but will not move within the enemy attack range
    public Direction recommendedSafeMovementDirectionForDirection(final Direction direction, final RobotController robotController, final RobotInfo[] enemies, final double buffer, boolean use90) {

        final MapLocation mapLocation = robotController.getLocation();
        if (robotController.canMove(direction) && this.isMapLocationSafe(mapLocation.add(direction), enemies, buffer)) {

            return direction;

        }

        final boolean divisible = robotController.getID() % 2 == 0;
        Direction movementDirection = divisible ? direction.rotateLeft() : direction.rotateRight();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies, buffer)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight() : direction.rotateLeft();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies, buffer)) {

            return movementDirection;

        }
        if (use90) {

            return null;

        }
        movementDirection = divisible ? direction.rotateLeft().rotateLeft() : direction.rotateRight().rotateRight();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies, buffer)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight().rotateRight() : direction.rotateLeft().rotateLeft();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies, buffer)) {

            return movementDirection;

        }
        return null;

    }

    public final Direction averageDirectionTowardRobots(final RobotController robotController, final RobotInfo[] robots) throws GameActionException {

        if (robots == null || robots.length == 0) {

            return null;

        }

        final MapLocation currentLocation = robotController.getLocation();
        int totalRobotX = 0;
        int totalRobotY = 0;

        for (int i = 0; i < robots.length; i++) {

            final RobotInfo robot = robots[i];
            final MapLocation location = robot.location;
            totalRobotX += location.x;
            totalRobotY += location.y;

        }

        final double averageRobotX = (double)totalRobotX / robots.length;
        final double averageRobotY = (double)totalRobotY / robots.length;
        final double dx = (double)(averageRobotX - currentLocation.x);
        final double dy = (double)(averageRobotY - currentLocation.y);
        return Math.abs(dx) >= 2.414D * Math.abs(dy)?(dx > 0.0D?Direction.EAST:(dx < 0.0D?Direction.WEST:Direction.OMNI)):(Math.abs(dy) >= 2.414D * Math.abs(dx)?(dy > 0.0D?Direction.SOUTH:Direction.NORTH):(dy > 0.0D?(dx > 0.0D?Direction.SOUTH_EAST:Direction.SOUTH_WEST):(dx > 0.0D?Direction.NORTH_EAST:Direction.NORTH_WEST)));

    }

    public final Direction averageDirectionTowardDangerousRobots(final RobotController robotController, final RobotInfo[] robots) throws GameActionException {

        if (robots == null || robots.length == 0) {

            return null;

        }

        final MapLocation currentLocation = robotController.getLocation();
        int totalRobotX = 0;
        int totalRobotY = 0;
        int totalRobotsFound = 0;

        for (int i = 0; i < robots.length; i++) {

            final RobotInfo robot = robots[i];
            if (!this.isEnemyDangerous(robot, currentLocation, 1)) {

                continue;

            }

            final MapLocation location = robot.location;
            totalRobotX += location.x;
            totalRobotY += location.y;
            totalRobotsFound ++;

        }

        if (totalRobotsFound == 0) {

            return null;

        }

        final double averageRobotX = (double)totalRobotX / totalRobotsFound;
        final double averageRobotY = (double)totalRobotY / totalRobotsFound;
        final double dx = (double)(averageRobotX - currentLocation.x);
        final double dy = (double)(averageRobotY - currentLocation.y);
        return Math.abs(dx) >= 2.414D * Math.abs(dy)?(dx > 0.0D?Direction.EAST:(dx < 0.0D?Direction.WEST:Direction.OMNI)):(Math.abs(dy) >= 2.414D * Math.abs(dx)?(dy > 0.0D?Direction.SOUTH:Direction.NORTH):(dy > 0.0D?(dx > 0.0D?Direction.SOUTH_EAST:Direction.SOUTH_WEST):(dx > 0.0D?Direction.NORTH_EAST:Direction.NORTH_WEST)));

    }

    public final Direction averageDirectionTowardDangerousRobotsAndOuterBounds(final RobotController robotController, final RobotInfo[] robots) throws GameActionException {

        if (robots == null || robots.length == 0) {

            return null;

        }

        final MapLocation currentLocation = robotController.getLocation();
        int totalRobotX = 0;
        int totalRobotY = 0;
        int totalRobotsFound = 0;

        for (int i = 0; i < robots.length; i++) {

            final RobotInfo robot = robots[i];
            if (!this.isEnemyDangerous(robot, currentLocation, 1)) {

                continue;

            }

            final MapLocation location = robot.location;
            totalRobotX += location.x;
            totalRobotY += location.y;
            totalRobotsFound ++;

        }

        if (totalRobotsFound == 0) {

            return null;

        }

        for (int i = 0; i < directions.length; i += 2) {

            final Direction direction = directions[i];
            final int sightDistance = (int)Math.round(Math.sqrt((double)robotController.getType().sensorRadiusSquared)) - 1;

            for (int j = 1; j <= sightDistance; j++) {

                final MapLocation location = currentLocation.add(direction, j);
                if (!robotController.onTheMap(location)) {

                    totalRobotX += location.x;
                    totalRobotY += location.y;
                    totalRobotsFound ++;

                }

            }

        }

        final double averageRobotX = (double)totalRobotX / totalRobotsFound;
        final double averageRobotY = (double)totalRobotY / totalRobotsFound;
        final double dx = (double)(averageRobotX - currentLocation.x);
        final double dy = (double)(averageRobotY - currentLocation.y);
        return Math.abs(dx) >= 2.414D * Math.abs(dy)?(dx > 0.0D?Direction.EAST:(dx < 0.0D?Direction.WEST:Direction.OMNI)):(Math.abs(dy) >= 2.414D * Math.abs(dx)?(dy > 0.0D?Direction.SOUTH:Direction.NORTH):(dy > 0.0D?(dx > 0.0D?Direction.SOUTH_EAST:Direction.SOUTH_WEST):(dx > 0.0D?Direction.NORTH_EAST:Direction.NORTH_WEST)));

    }

    public boolean isMapLocationSafe(final MapLocation mapLocation, final RobotInfo[] enemies, final double buffer) {

        return this.getEnemyInRangeOfMapLocation(mapLocation, enemies, buffer) == null;

    }

    public RobotInfo getEnemyInRangeOfMapLocation(final MapLocation mapLocation, final RobotInfo[] enemies, final double buffer) {

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (this.isEnemyDangerous(enemy, mapLocation, buffer)) {

                return enemy;

            }

        }
        return null;

    }

    public RobotInfo getNearestEnemyInRangeOfMapLocation(final MapLocation mapLocation, final RobotInfo[] enemies, final double buffer) {

        RobotInfo nearestEnemy = null;
        int nearestEnemyDistance = Integer.MAX_VALUE;

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            final int distance = enemy.location.distanceSquaredTo(mapLocation);
            if (!this.isEnemyDangerous(enemy, distance, buffer)) {

                continue;

            }
            if (distance < nearestEnemyDistance) {

                nearestEnemy = enemy;
                nearestEnemyDistance = distance;

            }

        }
        return nearestEnemy;

    }

    /*
    ENEMY RANKING
     */

    private boolean isEnemyTypeDangerous(final RobotInfo enemy) {

        if (enemy.type == RobotType.ZOMBIEDEN || enemy.type == RobotType.ARCHON || enemy.type == RobotType.SCOUT) {

            return false;

        }
        return true;

    }

    private boolean isEnemyDangerous(final RobotInfo enemy, final MapLocation currentLocation, final double buffer) {

        final int distance = currentLocation.distanceSquaredTo(enemy.location);
        return this.isEnemyDangerous(enemy, distance, buffer);

    }

    private boolean isEnemyDangerous(final RobotInfo enemy, final int distance, final double buffer) {

        if (!this.isEnemyTypeDangerous(enemy)) {

            return false;

        }
        if (distance > this.enemyAttackRadiusSquaredWithBuffer(enemy, buffer)) {

            return false;

        }
        return true;

    }

    private int enemyAttackRadiusSquaredWithBuffer(final RobotInfo enemy, double buffer) {

        return buffer == 0 ? enemy.type.attackRadiusSquared : (int)Math.round(Math.pow(Math.sqrt(enemy.type.attackRadiusSquared) + buffer, 2));

    }

}
