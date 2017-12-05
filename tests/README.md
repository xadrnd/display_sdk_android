# Testing

### Step 1 - Install Appium

```
brew install node
npm install -g appium@1.6.3
npm install wd
sudo pip install Appium-Python-Client==0.24
sudo pip install selenium==3.0.2
```

### Step 2 - Start Appium

`appium`

### Step 3 - Add demo apk into `tests` root directory

### Step 4 - Start emulator

`/Users/<username>/Library/Android/sdk/emulator/emulator -list-avds` to get all AVDs

`/Users/<username>/Library/Android/sdk/emulator/emulator @<avd_name>` to run AVD

### Step 5 - Run tests

`export ANDROID_HOME=/Users/<username>/Library/Android/sdk/`

All tests:

`python -m unittest test_specific.SpecificTest`

`python -m unittest test_mraid.MRAIDTest`

`python -m unittest test_random.RandomTest`

Individual test:

`python -m unittest test_specific.SpecificTest.test_banner`

### Step 6 - Kill appium, if running another test gets stuck, and go back to step 2

## Troubleshooting
1. Issue about complaining webdriver version like:
`WebDriverException: Message: An unknown server-side error occurred while processing the command. Original error: unknown error: Chrome version must be >= 53.0.2785.0`
Solution: Go to `https://chromedriver.storage.googleapis.com/index.html` to download latest version of Chrome webdriver and add `--chromedriver-executable <path_webdriver>` when start appium by type `appium` in Terminal.
