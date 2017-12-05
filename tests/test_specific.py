from test_base import DisplaySDKTest
from utils import *

SLEEP_INTERVAL = 2


class SpecificTest(DisplaySDKTest):
    def dev_test(self):
        switch_on_testmode(self)

    def test_banner(self):
        set_channel_id(self, "22394")

        click_load_ad_btn(self, "Banner")

        accept_location(self)

        webview = block_until_webview(self)

        sleep(SLEEP_INTERVAL)

        webview = self.driver.find_element_by_id("com.xad.sdk.sdkdemo:id/banner_ad_container")
        webview.click()

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        sleep(SLEEP_INTERVAL)

        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        # Assert bid agent is called when clicked
        assert_href_called(self, [r"/landingpage", r"notify.bidagent.xad.com"])

    def test_interstitial(self):
        set_channel_id(self, "15999")

        click_load_ad_btn(self, "Interstitial")

        accept_location(self)

        sleep(SLEEP_INTERVAL)

        block_until_webview(self)

        sleep(SLEEP_INTERVAL)

        switch_to_web_context(self)

        sleep(SLEEP_INTERVAL)

        xad_div = self.driver.find_element_by_id("xad")
        self.assertIsNotNone(xad_div)

        sleep(SLEEP_INTERVAL)

        switch_to_native_context(self)

        sleep(SLEEP_INTERVAL)

        # nagivate back
        try:
            click_x_btn(self)
        except:
            pass

        sleep(SLEEP_INTERVAL) 

        # Assert impression pixel is called
        assert_href_called(self, [r"eastads.simpli.fi", r"notify.bidagent.xad.com"])

    def test_vast_inline_linear(self):
        set_channel_id(self, "24373")
        
        click_load_ad_btn(self, "VIDEO")

        accept_location(self)

        block_until_class_name(self, "android.widget.ImageButton")

        positions = []
        positions.append((50, 50))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        sleep(SLEEP_INTERVAL)

        click_back_btn(self)

        sleep(30)

        positions = []
        positions.append((50, 50))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        sleep(SLEEP_INTERVAL)

        click_back_btn(self)

        sleep(40)

        block_until_class_name(self, "android.widget.ImageButton")

        btns = self.driver.find_elements_by_class_name("android.widget.ImageButton")
        btns[0].click()

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, [
            r"/click$",
            r"/impression$",
            r"/creativeView$",
            r"/start$",
            r"/firstQuartile$",
            r"/midpoint$",
            r"/thirdQuartile$",
            r"/complete$"
        ])

    def test_vast_inline_linear_error(self):
        set_channel_id(self, "24403")
        
        click_load_ad_btn(self, "Video")

        accept_location(self)

        sleep(SLEEP_INTERVAL)

        positions = []
        positions.append((50, 50))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        # Assert error url called
        assert_href_called(self, r"/error")

    def test_vast_urlencode_error(self):
        set_channel_id(self, "28228")
        
        click_load_ad_btn(self, "Video")

        accept_location(self)

        sleep(SLEEP_INTERVAL)

        positions = []
        positions.append((50, 50))
        self.driver.tap(positions)

    def test_vast_wrapper_linear_1_error(self):
        set_channel_id(self, "33838")
        
        click_load_ad_btn(self, "Video")

        accept_location(self)

        sleep(SLEEP_INTERVAL)

        positions = []
        positions.append((50, 50))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        # Assert error url called
        assert_href_called(self, r"/wrapper/error$")

        assert_href_called(self, r"/error$")

    def test_vast_wrapper_linear_1(self):
        set_channel_id(self, "24383")
        
        click_load_ad_btn(self, "Video")

        accept_location(self)

        block_until_class_name(self, "android.widget.ImageButton")

        # Click middle video
        positions = []
        positions.append((5, 5))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        click_back_btn(self)

        # Poll until video is done
        sleep(35)

        # Click middle video
        positions = []
        positions.append((5, 5))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        click_back_btn(self)

        sleep(45)

        btns = self.driver.find_elements_by_class_name("android.widget.ImageButton")
        btns[0].click()

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, [
            r"/click$",
            r"/impression$",
            r"/creativeView$",
            r"/start$",
            r"/firstQuartile$",
            r"/midpoint$",
            r"/thirdQuartile$",
            r"/complete$",
            r"/wrapper/click$",
            r"/wrapper/impression$",
            r"/wrapper/start$",
            r"/wrapper/firstQuartile$",
            r"/wrapper/pause$",
            r"/wrapper/resume$",
            r"/wrapper/midpoint$",
            r"/wrapper/thirdQuartile$",
            r"/wrapper/complete$"
        ])

    def test_vast_wrapper_linear_2(self):
        set_channel_id(self, "24388")
        
        click_load_ad_btn(self, "Video")

        block_until_class_name(self, "android.widget.ImageButton")

        sleep(SLEEP_INTERVAL)

        sleep(30)

        # Click middle video
        positions = []
        positions.append((5, 5))
        self.driver.tap(positions)

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        click_back_btn(self)

        # Poll until video is done
        sleep(30)

        btns = self.driver.find_elements_by_class_name("android.widget.ImageButton")
        btns[0].click()

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, [
            r"/click$",
            r"/impression$",
            r"/creativeView$",
            r"/start$",
            r"/firstQuartile$",
            r"/midpoint$",
            r"/thirdQuartile$",
            r"/complete$",
            r"/wrapper/impression$"
        ])
