#!/bin/bash

OLDNAME=$1
NEWNAME=$2

if [ -z "$OLDNAME" ] || [ -z "$NEWNAME" ]; then
    echo "Error: incorrect arguments provided"
    echo "Usage: ./duplicate_team.sh old_team_name new_team_name"
    exit
fi

if ! [ -d "$OLDNAME" ]; then
    echo "Error: it doesn't look like an existing bot named \"$OLDNAME\" exists."
    exit
fi

if [ -d "$NEWNAME" ]; then
    echo "Warning: a bot named \"$NEWNAME\" already exists. Do you wish to overwrite this bot? (y/n) "
    read OVERWRITE
    if [ "$OVERWRITE" == "y" ]; then
        echo "Overwriting \"$NEWNAME\"..."
        rm -rf "$NEWNAME"
    else
        echo "Not overwriting \"$NEWNAME\"... quitting."
        exit
    fi
fi

cp -rf "$OLDNAME" "$NEWNAME"
find "$NEWNAME" -type f -name '*.java' -exec sed -i '' "s/$OLDNAME/$NEWNAME/" {} +
git add "$NEWNAME"
echo "A bot named \"$NEWNAME\" has been generated from \"$OLDNAME\"."