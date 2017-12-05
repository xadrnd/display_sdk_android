from test_base import DisplaySDKTest
from utils import *


class RandomTest(DisplaySDKTest):
    def test_banner_random(self):
        click_btn(self, "All Errors")

        for i in range(10):    
            click_load_ad_btn(self, "Banner")

            accept_location(self)

            webview = block_until_webview(self)

            # switch to web view
            print("Switching to webview")
            self.driver.switch_to.context(webview)

            # inspect inside html
            #print(self.driver.)

            # switch back to native view
            print("Switching to native view")
            self.driver.switch_to.context(self.driver.contexts[0])

            # get web view
            webview = self.driver.find_elements_by_ios_predicate('wdType == "XCUIElementTypeWebView"')

            # if web view then click
            if len(webview) > 0:
                click_on_webview(self)

                sleep(5)

                try:
                    click_btn(self, "OK")
                except:
                    pass

                try:
                    click_btn(self, "Cancel")
                except:
                    pass

                # nagivate back
                try:
                    click_back_btn(self)
                except:
                    pass

    def test_banner_random_large(self):
        click_btn(self, "All Errors")

        for i in range(10):    
            click_load_ad_btn(self, "Banner", "300x250")

            accept_location(self)

            webview = block_until_webview(self)

            # switch to web view
            print("Switching to webview")
            self.driver.switch_to.context(webview)

            # inspect inside html
            #print(self.driver.)

            # switch back to native view
            print("Switching to native view")
            self.driver.switch_to.context(self.driver.contexts[0])

            # get web view
            webview = self.driver.find_elements_by_ios_predicate('wdType == "XCUIElementTypeWebView"')

            # if web view then click
            if len(webview) > 0:
                click_on_webview(self)

                sleep(5)

                try:
                    click_btn(self, "OK")
                except:
                    pass

                try:
                    click_btn(self, "Cancel")
                except:
                    pass

                # nagivate back
                try:
                    click_back_btn(self)
                except:
                    pass

    def test_interstitial_random(self):
        for i in range(10):
            click_load_ad_btn(self, "Interstitial")

            accept_location(self)

            block_until_webview(self)

            # switch to web view
            """
            self.driver.switch_to.context(webview)

            sleep(1)

            # inspect inside html
            #print(self.driver.

            # switch back to native view
            self.driver.switch_to.context(self.driver.contexts[0])
            """

            sleep(1)

            # find close button
            close_btn = self.driver.find_elements_by_ios_predicate('wdType == "XCUIElementTypeButton"')[0]

            close_btn.click()

            sleep(1)

    def test_video_random(self):
        for i in range(10):
            click_load_ad_btn(self, "Video")

            accept_location(self)

            # TODO Assert video player appeared

            # TODO Poll until video is done

            # TODO Close video