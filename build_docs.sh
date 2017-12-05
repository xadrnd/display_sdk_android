
grip README.md --export README.html --user-content
grip LICENSE.md --export LICENSE.html --user-content
markdown-pdf -z docs/default.min.css -o README.pdf README.md

echo "Do you want to review the README? [y/n]"
read answer
if [ "$answer" = "y" ]; then
	open README.pdf
else
	echo "Skip openning README"
fi
