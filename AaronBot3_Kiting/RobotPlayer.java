package AaronBot3_Kiting;

import battlecode.common.*;

public class RobotPlayer {

    public static void run(RobotController robotController) {

        final Robot robot = RobotPlayer.getRobotForType(robotController.getType());
    	try {

            robot.run(robotController);

    	}
    	catch (Exception e) {
        	
            System.out.println(e.getMessage());
            e.printStackTrace();
            
        }
    	
    }

    public static Robot getRobotForType(RobotType type) {

        if (type == RobotType.ARCHON) {

            return new RobotArchon();

        } else if (type == RobotType.GUARD) {

            return new RobotSoldier();

        } else if (type == RobotType.SCOUT) {

            return new RobotScout();

        } else if (type == RobotType.SOLDIER) {

            return new RobotSoldier();

        } else if (type == RobotType.TURRET) {

            return new RobotTurret();

        } else if (type == RobotType.VIPER) {

            return new RobotViper();

        }
        return null;

    }
}
