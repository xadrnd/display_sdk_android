#!/bin/bash
#step 0
#remove all build dirs
rm -rf displaysdk/build customeventgooglemobileads/build customeventmopub/build
echo "Cleaned all build dirs"

#step 1
#gradle build displaysdk
./gradlew :displaysdk:install --refresh-dependencies

#step 2
# ask for if upload displaysdk
echo -e "Do you want to upload displaysdk to maven center? [y/n]"
read -r answer
if [ "$answer" = "y" ]; then
	./gradlew :displaysdk:bintrayUpload
else
	echo "Skip uploading displaysdk"
fi

#ask for if make a new release in github
echo -e "Do you want to make a new release in github? [y/n]"
read -r answer
if [ "$answer" = "y" ]; then
	cd displaysdk/build/outputs || exit
	tar -czf displaysdk.tar.gz aar
	mv displaysdk.tar.gz ../../ || exit
	cd ../.. || exit
	python3.6 release.py
else
	echo "Skip creating new release"
fi

#step 3
# ask for if build google customevent
echo -e "Do you want to build \"customeventgooglemobileads\"? [y/n]"
read -r answer
if [ "$answer" = "y" ]; then
	./gradlew :customeventgooglemobileads:install --refresh-dependencies

	#step 4
	# ask for if upload google customevent
	echo -e "Do you want to upload \"customeventgooglemobileads\" to maven center? [y/n]"
	read -r answer
	if [ "$answer" = "y" ]; then
		./gradlew :customeventgooglemobileads:bintrayUpload
	else
		echo "Skip uploading \"customeventgooglemobileads\""
	fi

else
	echo "Skip building \"customeventgooglemobileads\""
fi

#step 5
# ask for if build mopub customevent
echo -e "Do you want to build \"customeventmopub\"? [y/n]"
read -r answer
if [ "$answer" = "y" ]; then
	./gradlew :customeventmopub:install --refresh-dependencies

	#step 6
	# ask for if upload mopub customevent
	echo -e "Do you want to upload \"customeventmopub\" to maven center? [y/n]"
	read -r answer
	if [ "$answer" = "y" ]; then
		./gradlew :customeventmopub:bintrayUpload
	else
		echo "Skip uploading \"customeventmopub\""
	fi

else
	echo "Skip building \"customeventmopub\""
fi