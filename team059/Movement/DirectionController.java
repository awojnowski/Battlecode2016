package team059.Movement;

import battlecode.common.*;
import java.util.Random;

public class DirectionController {

    public enum ErrorType {
        UNKNOWN,
        BLOCKED_FRIENDLIES,
        BLOCKED_ZOMBIES,
        BLOCKED_OPPONENTS,
        BLOCKED_RUBBLE,
        BLOCKED_OFF_MAP,
        BLOCKED_WITHIN_ENEMY_RANGE
    }

    public class Result {

        public Direction direction = null;
        public ErrorType error = null;

        public Result(final Direction direction, final ErrorType error) {

            this.direction = direction;
            this.error = error;

        }

    }

    // constants

    public static final Direction[] DIRECTIONS = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
    public static final int ADJUSTMENT_THRESHOLD_NONE = 0;
    public static final int ADJUSTMENT_THRESHOLD_LOW = 1;
    public static final int ADJUSTMENT_THRESHOLD_MEDIUM = 2;
    public static final int ADJUSTMENT_THRESHOLD_HIGH = 3;

    // location

    public MapLocation currentLocation = null;

    // enemies

    public RobotType[] safeEnemyTypes = new RobotType[]{ RobotType.ARCHON, RobotType.SCOUT, RobotType.ZOMBIEDEN, RobotType.TTM };
    public boolean shouldAvoidEnemies = false;
    public RobotInfo[] nearbyEnemies = null;
    public int enemyBufferDistance = 0;
    public int minimumEnemyAttackRadiusSquared = 0;

    // friendlies

    public RobotInfo[] nearbyFriendlies = null;

    // random

    public Random random = null;

    // robot controller

    public RobotController robotController = null;

    // internal


    /*
    INITIALIZATION
     */

    public DirectionController(final RobotController robotController) {

        this.robotController = robotController;

    }

    /*
    DIRECTION FETCHING
     */

    public Direction getRandomDirection() {

        return DirectionController.DIRECTIONS[this.random.nextInt(DirectionController.DIRECTIONS.length)];

    }

    public Result getDirectionResultFromDirection(final Direction direction, final int adjustmentThreshold) throws GameActionException {

        return this.getDirectionResultFromDirection(direction, 0, adjustmentThreshold);

    }

    public Result getDirectionResultFromDirection(final Direction direction, final int adjustmentThresholdStart, final int adjustmentThreshold) throws GameActionException {

        Direction directionA = direction;
        Direction directionB = direction;

        Result initialResult = null; // this will be used if result is null
        Result result = null;

        for (int i = adjustmentThresholdStart; i <= adjustmentThreshold; i++) {

            Result resultA = this.getResultForDirection(directionA);
            if (initialResult == null) {

                initialResult = resultA;

            }
            if (resultA.direction != null) {

                result = resultA;
                break;

            }

            if (i > 0) {

                Result resultB = this.getResultForDirection(directionB);
                if (resultB.direction != null) {

                    result = resultB;
                    break;

                }

            }

            if (i < adjustmentThreshold) {

                directionA = directionA.rotateLeft();
                directionB = directionB.rotateRight();

            }

        }

        if (result == null) {

            result = initialResult;

        }
        return result;

    }

    public final Direction getAverageDirectionTowardsEnemies(final RobotInfo[] enemies, final boolean moveAwayFromWalls, final boolean moveAwayFromRubble, final boolean useBetterRubbleDetection) throws GameActionException {

        return this.getAverageDirectionTowardsRobots(enemies, true, moveAwayFromWalls, moveAwayFromRubble, useBetterRubbleDetection);

    }

    public Direction getAverageDirectionTowardFriendlies(final RobotInfo[] friendlies, final boolean moveAwayFromWalls, final boolean moveAwayFromRubble, final boolean useBetterRubbleDetection) throws GameActionException {

        return this.getAverageDirectionTowardsRobots(friendlies, false, moveAwayFromWalls, moveAwayFromRubble, useBetterRubbleDetection);

    }

    private Direction getAverageDirectionTowardsRobots(final RobotInfo[] robots, final boolean onlyAllowDangerousRobots, final boolean moveAwayFromWalls, final boolean moveAwayFromRubble, final boolean useBetterRubbleDetection) throws GameActionException {

        if (robots == null || robots.length == 0) {

            return null;

        }

        final float randomSeed = (random.nextFloat() - 0.5f) / 100; // between -0.005 and 0.005
        int totalLocationsSampled = 0;
        int totalRobotX = 0;
        int totalRobotY = 0;

        for (int i = 0; i < robots.length; i++) {

            final RobotInfo robot = robots[i];

            MapLocation location = null;
            if (onlyAllowDangerousRobots) {

                if (this.isRobotDangerous(robot)) {

                    location = robot.location;

                }

            } else {

                location = robot.location;

            }
            if (location == null) {

                continue;

            }

            totalRobotX += location.x * 3; // enemies weighted 3x as much as walls
            totalRobotY += location.y * 3;
            totalLocationsSampled += 3;

        }

        if (totalLocationsSampled == 0) {

            return null;

        }

        if (moveAwayFromWalls || moveAwayFromRubble) {

            final int sightDistance = (int)Math.floor(Math.sqrt((double)robotController.getType().sensorRadiusSquared));
            Direction probeDirection = Direction.EAST;
            for (int i = 0; i < 4; i++) {

                boolean directionIsBlocked = false;

                for (int j = 1; j <= sightDistance; j++) {

                    final MapLocation location = this.currentLocation.add(probeDirection, j);
                    if (directionIsBlocked || (moveAwayFromWalls && !this.robotController.onTheMap(location)) || (moveAwayFromRubble && !useBetterRubbleDetection && this.robotController.senseRubble(location) >= 100)) {

                        totalRobotX += location.x;
                        totalRobotY += location.y;
                        totalLocationsSampled ++;
                        directionIsBlocked = true;

                    } else if (moveAwayFromRubble && useBetterRubbleDetection && this.robotController.senseRubble(location) >= 100) {

                        MapLocation beside1 = location.add(probeDirection.rotateLeft().rotateLeft());
                        MapLocation beside2 = location.add(probeDirection.rotateRight().rotateRight());

                        if (this.robotController.senseRubble(beside1) >= 100 && this.robotController.senseRubble(beside2) >= 100) {

                            totalRobotX += location.x;
                            totalRobotY += location.y;
                            totalLocationsSampled ++;
                            directionIsBlocked = true;

                        }

                    }

                }
                probeDirection = probeDirection.rotateLeft().rotateLeft();

            }

        }

        final double averageRobotX = ((double)totalRobotX + randomSeed) / totalLocationsSampled;
        final double averageRobotY = ((double)totalRobotY + randomSeed) / totalLocationsSampled;
        final double dx = averageRobotX - currentLocation.x;
        final double dy = averageRobotY - currentLocation.y;
        return Math.abs(dx) >= 2.414D * Math.abs(dy)?(dx > 0.0D?Direction.EAST:(dx < 0.0D?Direction.WEST:Direction.OMNI)):(Math.abs(dy) >= 2.414D * Math.abs(dx)?(dy > 0.0D?Direction.SOUTH:Direction.NORTH):(dy > 0.0D?(dx > 0.0D?Direction.SOUTH_EAST:Direction.SOUTH_WEST):(dx > 0.0D?Direction.NORTH_EAST:Direction.NORTH_WEST)));

    }

    /*
    DIRECTION VALIDATION
     */

    private Result getResultForDirection(final Direction direction) throws GameActionException {

        final MapLocation location = currentLocation.add(direction);

        // check to make sure that there is no rubble

        if (!this.robotController.getType().ignoresRubble) {

            if (this.robotController.senseRubble(location) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {

                return new Result(null, ErrorType.BLOCKED_RUBBLE);

            }

        }

        // check to make sure that the location isn't occupied

        final RobotInfo robot = this.robotController.senseRobotAtLocation(location);
        if (robot != null) {

            final Team currentTeam = this.robotController.getTeam();
            if (robot.team == currentTeam) {

                return new Result(null, ErrorType.BLOCKED_FRIENDLIES);

            } else if (robot.team == currentTeam.opponent()) {

                return new Result(null, ErrorType.BLOCKED_OPPONENTS);

            } else if (robot.team == Team.ZOMBIE) {

                return new Result(null, ErrorType.BLOCKED_ZOMBIES);

            }

        }

        // check to make sure it is on the map

        if (!this.robotController.onTheMap(location)) {

            return new Result(null, ErrorType.BLOCKED_OFF_MAP);

        }

        // check to make sure it isn't within enemy range

        if (this.shouldAvoidEnemies) {

            for (int i = 0; i < this.nearbyEnemies.length; i++) {

                final RobotInfo enemy = this.nearbyEnemies[i];
                if (this.isRobotDangerous(enemy)) {

                    return new Result(null, ErrorType.BLOCKED_WITHIN_ENEMY_RANGE);

                }

            }

        }

        // finally, check to confirm we can move there

        if (!this.robotController.canMove(direction)) {

            return new Result(null, ErrorType.UNKNOWN);

        }

        return new Result(direction, null);

    }

    /*
    ENEMIES
     */

    public int attackRadiusSquaredWithBuffer(final int attackDistanceSquared, final int buffer) {

        return buffer > 0 ? (int)Math.floor(Math.pow(Math.sqrt((double)attackDistanceSquared) + buffer, 2.0)) : attackDistanceSquared;

    }

    private boolean isRobotDangerous(final RobotInfo enemy) {

        if (!this.isRobotTypeDangerous(enemy.type)) {

            return false;

        }
        final int distance = this.currentLocation.distanceSquaredTo(enemy.location);
        final int attackRadiusSquared = Math.max(enemy.type.attackRadiusSquared, this.minimumEnemyAttackRadiusSquared);
        if (distance < this.attackRadiusSquaredWithBuffer(attackRadiusSquared, this.enemyBufferDistance)) {

            return true;

        }
        return false;

    }

    private boolean isRobotTypeDangerous(final RobotType robotType) {

        for (int i = 0; i < this.safeEnemyTypes.length; i++) {

            if (robotType.equals(this.safeEnemyTypes[i])) {

                return false;

            }

        }
        return true;

    }

}
