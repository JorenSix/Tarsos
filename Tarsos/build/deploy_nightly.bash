#!/bin/bash

manual_location="Tarsos-latest-Manual.pdf"
deploy_location="/var/www/be.0110/current/public/releases/Tarsos"

#Remove old releases
rm -R Tarsos-*
wget "http://tarsos.0110.be/releases/Tarsos/Tarsos-latest/Tarsos-latest-Manual.pdf"

#build new release
ant clean
ant release

#Find the current version
filename=$(basename Tarsos-*.jar)
version=${filename:7:3}

#Build the readme file
textile2html ../README.textile Tarsos-$version-Readme.html

#rename the manual
mv $manual_location Tarsos-$version-Manual.pdf

#Remove old version from the release repository on the server server:
ssh joren@0110.be rm -R $deploy_location/Tarsos-nightly
ssh joren@0110.be mkdir $deploy_location/Tarsos-nightly

#Deploy to release the release repository 
scp -r Tarsos-* joren@0110.be:$deploy_location/Tarsos-nightly

#Create symlinks for the latest version
ssh joren@0110.be mv $deploy_location/Tarsos-nightly/Tarsos-$version.jar 			$deploy_location/Tarsos-nightly/Tarsos-nightly.jar
ssh joren@0110.be mv $deploy_location/Tarsos-nightly/Tarsos-$version-Manual.pdf 	$deploy_location/Tarsos-nightly/Tarsos-nightly-Manual.pdf
ssh joren@0110.be mv $deploy_location/Tarsos-nightly/Tarsos-$version-Documentation  $deploy_location/Tarsos-nightly/Tarsos-nightly-Documentation
ssh joren@0110.be mv $deploy_location/Tarsos-nightly/Tarsos-$version-Readme.html 	$deploy_location/Tarsos-nightly/Tarsos-nightly-Readme.html 
