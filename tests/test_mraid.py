# encoding: utf8

from test_base import DisplaySDKTest
from utils import *


SLEEP_INTERVAL = 2


class MRAIDTest(DisplaySDKTest):
    def test_mraid_single_expand(self):
        self.driver.orientation = 'PORTRAIT'

        set_channel_id(self, "24338")

        click_load_ad_btn(self, "BANNER")

        accept_location(self)

        block_until_webview(self)

        sleep(SLEEP_INTERVAL)

        click_on_webview(self)

        sleep(SLEEP_INTERVAL)

        assert_exists(self, "Lock to Landscape")

        # Rotate phone
        self.driver.orientation = 'LANDSCAPE'

        sleep(SLEEP_INTERVAL)

        assert_exists(self, "Rotate To Portrait")

        # Rotate phone back
        self.driver.orientation = 'PORTRAIT'

        sleep(SLEEP_INTERVAL)

        assert_exists(self, "Lock to Landscape")

        click_btn(self, "Lock to Landscape")

        sleep(SLEEP_INTERVAL)

        assert_exists(self, "Lock to Portrait")

        self.driver.orientation = 'LANDSCAPE'

        click_btn(self, "Lock to Portrait")

        sleep(SLEEP_INTERVAL)

        assert_exists(self, "Release Lock")

        # Rotate phone
        #self.driver.orientation = 'LANDSCAPE'

        #sleep(SLEEP_INTERVAL)

        assert_exists(self, "Release Lock")

        # Rotate phone back
        #self.driver.orientation = 'PORTRAIT'

        sleep(SLEEP_INTERVAL)

        assert_exists(self, "Release Lock")

        click_btn(self, "Release Lock")

        sleep(SLEEP_INTERVAL)

        assert_not_exists(self, "Lock to Portrait")
        assert_not_exists(self, "Lock To Landscape")
        assert_not_exists(self, "Release Lock")

        # Rotate phone
        self.driver.orientation = 'LANDSCAPE'

        sleep(SLEEP_INTERVAL)

        assert_not_exists(self, "Lock To Portrait")
        assert_not_exists(self, "Lock to Landscape")
        assert_not_exists(self, "Release Lock")

        # Rotate phone back
        self.driver.orientation = 'PORTRAIT'

        sleep(SLEEP_INTERVAL)

        click_x_btn(self)

    def test_mraid_two_part_expand(self):
        self.driver.orientation = 'PORTRAIT'

        set_channel_id(self, "24343")        

        click_load_ad_btn(self, "Banner")

        accept_location(self)

        block_until_webview(self)

        save_source(self)

        block_until_element(self, ["Two Part Expand", "Two Part Expand Link"])

        click_btn(self, "Two Part Expand")
        click_btn(self, "Two Part Expand Link")

        sleep(SLEEP_INTERVAL)

        switch_to_web_context(self)

        block_until_css_element(self, "#openiab")

        self.driver.orientation = 'LANDSCAPE'

        sleep(SLEEP_INTERVAL)

        self.driver.orientation = 'PORTRAIT'

        sleep(SLEEP_INTERVAL)

        # Click on open iab.com button
        click_btn(self, "#openiab")

        sleep(SLEEP_INTERVAL)

        if len(self.driver.find_elements_by_id("android:id/resolver_grid")) != 0:
            click_btn(self, "Chrome")

            sleep(SLEEP_INTERVAL)

            click_btn(self, "Always")

            sleep(SLEEP_INTERVAL)

            click_btn(self, "OK")

        switch_to_native_context(self)

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        # Close browser
        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        switch_to_web_context(self)

        sleep(SLEEP_INTERVAL)

        # Play video
        click_btn(self, "#openvideo")

        sleep(SLEEP_INTERVAL*3)

        assert_viewing_video(self)

        # Close video
        click_back_btn(self)

        # Assert expand again does nothing
        click_btn(self, "Expand Again")

        sleep(SLEEP_INTERVAL)

        assert_not_viewing_browser(self)

        # Close expanded view
        click_btn(self, "Click here to close.")

        sleep(SLEEP_INTERVAL)

        switch_to_native_context(self)

        block_until_element(self, ["Two Part Expand", "Two Part Expand Link"])

        sleep(SLEEP_INTERVAL)

        click_btn(self, "Two Part Expand")
        click_btn(self, "Two Part Expand Link")

        click_btn(self, "Two Part Expand")
        click_btn(self, "Two Part Expand Link")

        sleep(SLEEP_INTERVAL)

        block_until_element(self, "Click here to close.")

        # TODO Click upper top corner and assert close
        click_btn(self, "Click here to close.")

        sleep(SLEEP_INTERVAL)

        save_source(self)

        assert_href_called(self, r"mraid://useCustomClose")

        assert_href_called(self, r"mraid://setOrientationProperties")

        assert_href_called(self, r"mraid://expand\?url=")

        assert_href_called(self, r"mraid://open\?url=")

        assert_href_called(self, r"mraid://playVideo")

        assert_href_called(self, r"mraid://close")

    def test_mraid_resize(self):
        set_channel_id(self, "24348")

        click_load_ad_btn(self, "Banner")

        accept_location(self)

        block_until_webview(self)

        sleep(SLEEP_INTERVAL)

        block_until_element(self, "Click to Resize")

        click_btn(self, "Click to Resize")

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://resize")

        # Click open url
        click_btn(self, "Open URL")

        sleep(SLEEP_INTERVAL)

        assert_viewing_browser(self)

        # Close browser
        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://open\?url=.*www\.iab\.net")

        # Open map
        click_btn(self, "Click to Map")

        sleep(SLEEP_INTERVAL)

        if len(self.driver.find_elements_by_id("android:id/resolver_grid")) != 0:
            click_btn(self, "Maps")

            sleep(SLEEP_INTERVAL)

            click_btn(self, "Always")

            sleep(SLEEP_INTERVAL)

            click_btn(self, "OK")

        sleep(SLEEP_INTERVAL)

        assert_viewing_maps(self)

        # Close map
        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://open\?url=.*maps\.google\.com")

        # Open app
        click_btn(self, "Click to App")

        sleep(SLEEP_INTERVAL)

        if len(self.driver.find_elements_by_id("android:id/resolver_grid")) != 0:
            click_btn(self, "Play Store")

            sleep(SLEEP_INTERVAL)

            click_btn(self, "Always")

            sleep(SLEEP_INTERVAL)

            click_btn(self, "OK")

        sleep(SLEEP_INTERVAL)

        assert_viewing_google_play(self)

        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://open\?url=.*play.google.com")

        # Open video
        click_btn(self, "Play Video")

        sleep(SLEEP_INTERVAL)

        assert_viewing_video(self)

        sleep(SLEEP_INTERVAL)

        # Close video
        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        self.driver.orientation = 'PORTRAIT'

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://playVideo")

        """
        click_btn(self, "Click to Resize")

        # Send sms
        click_btn(self, "SMS")

        sleep(SLEEP_INTERVAL)

        assert_viewing_sms(self)

        sleep(SLEEP_INTERVAL)

        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://open\?url=sms")

        # Click to call
        click_btn(self, "Click to Call")

        sleep(SLEEP_INTERVAL)

        assert_viewing_call(self)

        # Close call
        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        click_back_btn(self)

        sleep(SLEEP_INTERVAL)

        assert_href_called(self, r"mraid://open\?url=tel")
        """

    def test_mraid_full_page(self):
        set_channel_id(self, "24353")        

        click_load_ad_btn(self, "Banner")

        accept_location(self)

        block_until_webview(self)

        sleep(SLEEP_INTERVAL)

        save_source(self)

        click_btn(self, "HIDE")
        click_btn(self, "Hide")

        sleep(SLEEP_INTERVAL)

        click_btn(self, "SHOW")
        click_btn(self, "Show")

        sleep(SLEEP_INTERVAL)

        switch_to_web_context(self)

        sleep(SLEEP_INTERVAL)

        # Assert that off screen timer is not all zeros
        timer = self.driver.find_elements_by_css_selector("#offscreentimer")[0]
        self.assertNotEquals(timer.text, "00:00:00")

        # Assert that on screen timer is not all zeros
        timer = self.driver.find_elements_by_css_selector("#onscreentimer")[0]
        self.assertNotEquals(timer.text, "00:00:00")

    def test_mraid_resize_error(self):
        set_channel_id(self, "24358")        

        # Call specific size
        click_load_ad_btn(self, "Banner", "300x250")

        accept_location(self)

        block_until_webview(self)

        sleep(SLEEP_INTERVAL)

        # Click bad timing
        click_parent_btn(self, "bad timing")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "bad values")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "small")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "big")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "←")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "→")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "↑")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "↓")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "TRUE")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "←")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "→")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "↑")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "↓")

        sleep(SLEEP_INTERVAL)

        click_parent_btn(self, "X")

    def test_mraid_video_interstitial(self):
        set_channel_id(self, "24363")        

        click_load_ad_btn(self, "Interstitial")

        sleep(SLEEP_INTERVAL)

        accept_location(self)

        block_until_webview(self)

        switch_to_web_context(self)

        sleep(SLEEP_INTERVAL)

        block_until_css_element(self, "video")

        sleep(SLEEP_INTERVAL)

        # Assert landscape view
        assert_landscape_view(self)

        switch_to_native_context(self)

        sleep(30)

        assert_href_called(self, r"mraid://useCustomClose")

        assert_href_called(self, r"mraid://close")

        