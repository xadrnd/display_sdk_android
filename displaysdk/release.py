#!/usr/local/bin/python3.6
import requests
import sys

#This is github release script for Display-SDK android

print("Creating a new release...")
# owner = 'ray4xad'
owner = 'xadrnd'
repo = "display_sdk_android"
zip_file_path = "displaysdk.tar.gz"
label = "displaysdk.tar.gz"
github_token = None

tag_name = "v"
description = ""
draft = False
prerelease = False
with open("build.gradle", "r") as build_gradle_file:
	for line in build_gradle_file:
		pair = line.split('=')
		if len(pair) < 2:
			continue
		if "libraryVersion" == pair[0].strip():
			tag_name += pair[1].strip()[1 : -1]
		if "libraryVersionDescription" == pair[0].strip():
			description = pair[1].strip()[1 : -1]
			break
is_rc_release = input("Is this a RC release [y/n]:")
# TODO get latest release and increase RC #
if is_rc_release == "y":
	tag_name += "-RC"
	prerelease = True
release_data = {"tag_name": tag_name, "target_commitish": "master", "name": tag_name, "body": description, "draft": draft, "prerelease": prerelease}
# headers = {"Accept": "application/vnd.github.v3+json"}
with open("../local.properties") as local_properties:
	for line in local_properties:
		pair = line.split("=")
		if len(pair) < 2: 
			continue
		if "github.token" == pair[0].strip():
			github_token = "token " + pair[1].strip()
if github_token is None:
	print("Could not find valid github token, abort creating new release\nPlease add \"githug.token\" in your \"local.properties\" file")
	sys.exit()
release_headers = {"Authorization": github_token}
create_url = 'https://api.github.com/repos/{owner}/{repo}/releases'.format(owner=owner, repo=repo)
res = requests.post(create_url, json = release_data, headers = release_headers)
print("Completed creating a new release in {owner}: {repo}, start to upload aar files...".format(owner = owner, repo = repo))

new_release_json = res.json()
new_release_id = new_release_json["id"]
upload_url = "https://uploads.github.com/repos/{owner}/{repo}/releases/{id}/assets".format(owner=owner, repo=repo ,id=new_release_id)
upload_parmas = {"name": zip_file_path, "label": label}
upload_headers = {"Content-Type": "application/gzip", "Authorization": github_token}
with open(zip_file_path, "rb") as zip_file_path:
	if zip_file_path is None:
		print("Could not find zip file to upload, abort creating new release")
		#TODO delete new release
		sys.exit()
	upload_files = {"file": zip_file_path}
	upload_res = requests.post(upload_url, headers = upload_headers, files = upload_files, params = upload_parmas)
print("Completed uploading")