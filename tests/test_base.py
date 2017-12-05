import unittest
import os
from appium import webdriver
from time import sleep
from utils import *

from selenium.webdriver.common.keys import Keys


uuid = "44e89a1349b5c756920212d535f97ad44e6ca936"


class DisplaySDKTest(unittest.TestCase):

    def setUp(self):
        self.driver = webdriver.Remote(
            command_executor='http://127.0.0.1:4723/wd/hub',
            desired_capabilities={
                'appPackage': 'com.xad.sdk.sdkdemo',
                'appActivity': 'MainActivity',
                'deviceName': 'Android Simulator',
                'platformName': 'Android',
                'autoAcceptAlerts': True,
                'autoGrantPermissions': True
            })

    def tearDown(self):
        self.driver.quit()
        