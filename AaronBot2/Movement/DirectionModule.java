package AaronBot2.Movement;

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
    public Direction recommendedMovementDirectionForDirection(final Direction direction, final RobotController robotController) {

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

    // will do the same as recommendedMovementDirectionForDirection but will not move within the enemy attack range
    public Direction recommendedSafeMovementDirectionForDirection(final Direction direction, final RobotController robotController, final RobotInfo[] enemies) {

        final MapLocation mapLocation = robotController.getLocation();
        if (robotController.canMove(direction) && this.isMapLocationSafe(mapLocation.add(direction), enemies)) {

            return direction;

        }

        final boolean divisible = robotController.getID() % 2 == 0;
        Direction movementDirection = divisible ? direction.rotateLeft() : direction.rotateRight();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight() : direction.rotateLeft();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateLeft().rotateLeft() : direction.rotateRight().rotateRight();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies)) {

            return movementDirection;

        }
        movementDirection = divisible ? direction.rotateRight().rotateRight() : direction.rotateLeft().rotateLeft();
        if (robotController.canMove(movementDirection) && this.isMapLocationSafe(mapLocation.add(movementDirection), enemies)) {

            return movementDirection;

        }
        return null;

    }

    public boolean isMapLocationSafe(final MapLocation mapLocation, final RobotInfo[] enemies) {

        return this.getEnemyInRangeOfMapLocation(mapLocation, enemies) == null;

    }

    public RobotInfo getEnemyInRangeOfMapLocation(final MapLocation mapLocation, final RobotInfo[] enemies) {

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            final int distance = enemy.location.distanceSquaredTo(mapLocation);
            if (distance <= enemy.type.attackRadiusSquared) {

                return enemy;

            }

        }
        return null;

    }

    public RobotInfo getNearestEnemyInRangeOfMapLocation(final MapLocation mapLocation, final RobotInfo[] enemies) {

        RobotInfo nearestEnemy = null;
        int nearestEnemyDistance = Integer.MAX_VALUE;

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            final int distance = enemy.location.distanceSquaredTo(mapLocation);
            if (distance <= enemy.type.attackRadiusSquared) {

                if (distance < nearestEnemyDistance) {

                    nearestEnemy = enemy;
                    nearestEnemyDistance = distance;

                }

            }

        }
        return nearestEnemy;

    }

}
