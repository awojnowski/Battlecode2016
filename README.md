Battlecode 2016
===============

Team #trump2016
---------------

Good luck!

Installation Instructions
-------------------------

1. Open `Terminal`
2. `cd` to the `battlecode-scaffold-master` directory
3. Type `rm -rf src`
4. Type `git clone git@github.com:awojnowski/Battlecode2016.git`
5. Type `mv Battlecode2016 src`
6. Run Battlecode!

Building Bots
-------------

A shell script is included which will build the bot named `team059`. This is the bot number for the team.

1. Open `Terminal`
2. `cd` to the `battlecode-scaffold-master` directory
3. `cd` to the `src` directory
4. Type `./build_team.sh`

Duplicating Bots
----------------

A shell script is included which will duplicate existing bots.

1. Open `Terminal`
2. `cd` to the `battlecode-scaffold-master` directory
3. `cd` to the `src` directory
4. Type `./duplicate_team.sh old_team_name new_team_name` (example: `./duplicate_team.sh team059 KillerBot12`)