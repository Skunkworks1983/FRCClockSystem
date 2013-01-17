#!/usr/bin/env bash

# Test UID
if [ ! `id -u` == 0 ]; then
	echo "This script must be run as root!"
	exit 1
fi
# Test Programs
type java 2>/dev/null || { echo >&2 "I require 'java' but it's not installed.  Aborting."; exit 1; }
type xdpyinfo 2>/dev/null || { echo >&2 "I require 'xdpyinfo' but it's not installed.  Aborting."; exit 1; }

# Get user name
echo "Please enter a user name to run under..."
read username

# Create user if it isn't already there.
if [ ! "`getent passwd $username`" > /dev/null ]; then
    echo "Adding user for '$username'"
    adduser --shell /bin/false --home "/home/$username" "$username"
fi

# Copy Files
if [ ! -d "/home/$username/clock" ]; then
	echo "Making directory '/home/$username/clock'"
	mkdir -p "/home/$username/clock"
fi
echo "Copying files..."
cp "clock.jar" "/home/$username/clock/clock.jar"

# Create database folder
if [ ! -d "/home/$username/clock/data" ]; then
	echo "Making database directory '/home/$username/clock/data/'"
	mkdir "/home/$username/clock/data"
fi

if [ ! -d "/home/$username/clock/data/mugs_large" ]; then
	echo "Making raw image directory '/home/$username/clock/data/mugs_large'"
	mkdir "/home/$username/clock/data/mugs_large"
fi

if [ ! -d "/home/$username/clock/data/mugs" ]; then
	echo "Making processed image directory '/home/$username/clock/data/mugs'"
	mkdir "/home/$username/clock/data/mugs"
fi

if [ ! -f "/home/$username/clock/data/members.csv" ]; then
	echo "Making empty member database '/home/$username/clock/data/members.csv'"
	touch "/home/$username/clock/data/members.csv"
fi

if [ -f "/home/$username/.xinitrc" ]; then
	echo "Moving .xinitrc to .xinitrc.old"
	mv "/home/$username/.xinitrc" "/home/$username/.xinitrc.old"
fi

echo "Creating .xinitrc"
echo "#!/bin/bash
res=\`xdpyinfo | grep dim | sed -r \"s/[^\S]*dimensions:[^0-9]*([0-9]+)x([0-9]+) pixels.*/\1x\2/\"\`
cd \"/home/$username/clock\"
java -jar \"clock.jar\" size=\$res" >> "/home/$username/.xinitrc"
chmod +x "/home/$username/.xinitrc"
chown $username:$username -R "/home/$username/"

echo "Registering X-server as local user startup script"
echo "startx" >> "/home/$username/.bashrc"

echo "Patching inittab to login as user on startup..."
cp /etc/inittab /etc/inittab.old
echo "54c54,55
< 1:2345:respawn:/sbin/getty --noclear 38400 tty1 
---
> #1:2345:respawn:/sbin/getty --noclear 38400 tty1 
> 1:2345:respawn:/bin/login -f $username tty1 </dev/tty1 >/dev/tty1 2>&1
" | patch -p1 /etc/inittab