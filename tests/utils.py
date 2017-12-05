from time import sleep
import re
from appium.webdriver.common.touch_action import TouchAction

def assert_exists(self, name):
    element = None
    try:
        element = self.driver.find_element_by_android_uiautomator('new UiSelector().text("%s")' % name)
    except:
        try:
            element = self.driver.find_element_by_android_uiautomator('new UiSelector().description("%s")' % name)
        except:
            pass
    
    self.assertTrue(element is not None)
    print("Asserted exists: %s" % name)


def assert_not_exists(self, name):
    element = None
    try:
        element = self.driver.find_element_by_android_uiautomator('new UiSelector().text("%s")' % name)
    except:
        try:
            element = self.driver.find_element_by_android_uiautomator('new UiSelector().description("%s")' % name)
        except:
            pass

    self.assertTrue(element is None)
    print("Asserted not exists: %s" % name)


def assert_landscape_view(self):
    print(self.driver.orientation)
    self.assertTrue(self.driver.orientation == "UIA_DEVICE_ORIENTATION_LANDSCAPERIGHT" or
        self.driver.orientation == "LANDSCAPE")


def assert_href_called(self, patterns):
    if not isinstance(patterns, list):
        patterns = [patterns]

    table_items = [
        x.get_attribute("text") for x in
        self.driver.find_elements_by_class_name("android.widget.TextView")
    ]

    el = self.driver.find_element_by_class_name("android.support.v7.widget.RecyclerView")

    for i in range(15):
        try:
            action = TouchAction(self.driver)
            action.press(el).move_to(x=0, y=-30).release().perform()

            table_items += [
                x.get_attribute("text") for x in
                self.driver.find_elements_by_class_name("android.widget.TextView")
            ]
        except:
            pass

    for i in range(15):
        try:
            action = TouchAction(self.driver)
            action.press(el).move_to(x=0, y=100).release().perform()
        except:
            pass

    table_items = list(set(table_items))
    print(table_items)

    for pattern in patterns:
        found = False
        for table_item in table_items:
            if re.search(pattern, table_item):
                print("HREF called: %s" % pattern)
                found = True
                break

        if not found:
            self.assertFalse("HREF was not called: %s" % pattern)


def assert_viewing_browser(self):
    self.assertIn("browser", self.driver.current_activity.lower())


def assert_not_viewing_browser(self):
    self.assertNotIn("browser", self.driver.current_activity.lower())


def assert_viewing_google_play(self):
    if "browser" in self.driver.current_activity.lower():
        print("Google Play Store is not install, opening in browser")
    else:    
        self.assertIn("finsky", self.driver.current_activity)


def assert_viewing_maps(self):
    self.assertIn("maps", self.driver.current_activity)


def assert_viewing_video(self):
    self.assertIn("video", self.driver.current_activity.lower())


def assert_not_viewing_video(self):
    self.assertNotIn("video", self.driver.current_activity.lower())


def assert_viewing_call(self):
    self.assertIn("dialer", self.driver.current_activity)


def assert_viewing_sms(self):
    self.assertIn("ComposeMessageActivity", self.driver.current_activity)


def block_until_webview(self, tries=5):
    count = 0
    element = None
    while True:
        try:
            try:
                element = self.driver.find_element_by_class_name("android.webkit.WebView")
            except:
                try:
                    element = self.driver.find_element_by_id("com.xad.sdk.sdkdemo:id/banner_ad_container")
                except:
                    for x in self.driver.contexts:
                        if "WEBVIEW" in x:
                            element = x

            if element:
                print("Webview ready")
                break
        except:
            pass

        print("Webview not ready")
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until webview timed out")

    return element


def block_until_not_webview(self, tries=5):
    return block_until_not_class_name(self, "android.webkit.WebView")


def switch_to_web_context(self):
    self.driver.switch_to.context(self.driver.contexts[-1])


def switch_to_native_context(self):
    self.driver.switch_to.context(self.driver.contexts[0])


def block_until_element(self, names, tries=5):
    count = 0
    element = None

    if isinstance(names, str):
        names = [names]

    while not element:
        try:
            for name in names:
                try:
                    element = self.driver.find_element_by_name(name)
                except:
                    try:
                        element = self.driver.find_element_by_android_uiautomator('new UiSelector().description("%s")' % name)
                    except:
                        pass

                if element:
                    print("Element %s ready" % name)
                    break
        except:
            pass

        print("Element %s not ready" % name)
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until element timed out")

    return element


def block_until_css_element(self, name, tries=5):
    count = 0
    element = None
    while True:
        try:
            try:
                element = self.driver.find_elements_by_css_selector(name)[0]
            except:
                pass

            if element:
                print("Element %s ready" % name)
                break
        except:
            pass

        print("Element %s not ready" % name)
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until element timed out")

    return element


def block_until_class_name(self, name, tries=5):
    count = 0
    element = None
    while True:
        try:
            save_source(self)

            element = self.driver.find_element_by_class_name(name)

            if element:
                print("Element %s ready" % name)
                break
        except:
            pass

        print("Element %s not ready" % name)
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until element timed out")

    return element


def block_until_not_element(self, name, tries=5):
    count = 0
    element = True
    while True:
        try:
            try:
                element = self.driver.find_element_by_name(name)
            except:
                try:
                    element = self.driver.find_element_by_android_uiautomator('new UiSelector().description("%s")' % name)
                except:
                    pass

            if not element:
                print("Element %s not ready" % name)
                break
        except:
            print("Element %s not ready" % name)
            break

        print("Element %s ready" % name)
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until element timed out")

    return element


def block_until_not_class_name(self, name, tries=5):
    count = 0
    element = True
    while True:
        try:
            try:
                element = self.driver.find_element_by_class_name(name)
            except:
                pass

            if not element:
                print("Element %s not ready" % name)
                break
        except:
            print("Element %s not ready" % name)
            break

        print("Element %s ready" % name)
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until element timed out")

    return element


def block_until_not_css_element(self, name, tries=5):
    count = 0
    element = True
    while True:
        try:
            try:
                element = self.driver.find_elements_by_css_selector(name)[0]
            except:
                pass

            if not element:
                print("Element %s not ready" % name)
                break
        except:
            print("Element %s not ready" % name)
            break

        print("Element %s ready" % name)
        sleep(1)
        count += 1

        if count == tries:
            self.assertTrue(True, "Block until element timed out")

    return element


def click_btn(self, name):
    try:
        self.driver.find_element_by_android_uiautomator('new UiSelector().text("%s")' % name).click()
        print("Clicked on: %s (UIAutomator Name)" % name)
    except:
        try:
            self.driver.find_element_by_android_uiautomator('new UiSelector().description("%s")' % name).click()
            print("Clicked on: %s (UIAutomator Desc)" % name)
        except:
            try:
                self.driver.find_element_by_name(name).click()
                print("Clicked on: %s (Element Id)" % name)
            except:
                try:
                    self.driver.find_elements_by_css_selector(name)[0].click()
                    print("Clicked on: %s (CSS Selector)" % name)
                except:
                    pass


click_parent_btn = click_btn


def click_on_webview(self):
    webview = self.driver.find_element_by_class_name("android.webkit.WebView")
    webview.click()
    print("Clicked on web view")


def print_source(self):
    print(self.driver.page_source)


def save_source(self):
    with open("tmp.xml", "w+") as fd:
        fd.write(self.driver.page_source.encode("utf8"))


def accept_location(self):
    # accept location
    try:
        allow_location = self.driver.find_element_by_name("Allow")

        if allow_location:
            allow_location.click()
            print("Allowing location")
    except:
        pass


def click_load_ad_btn(self, type="Banner", size="300x50"):
    size_spinner = self.driver.find_element_by_id("com.xad.sdk.sdkdemo:id/banner_size_spinner")
    size_spinner.click()
    sleep(1)
    size_element = self.driver.find_element_by_android_uiautomator('new UiSelector().text("%s")' % size)
    size_element.click()
    sleep(1)
    button = self.driver.find_element_by_id("com.xad.sdk.sdkdemo:id/%s_button" % type.lower())
    button.click()
    print("Clicking %s load button" % type)


def set_channel_id(self, id):
    channel_text_field = self.driver.find_element_by_class_name('android.widget.EditText')
    channel_text_field.send_keys("%s\n" % id)
    print("Setting channel id: %s" % id)


def click_x_btn(self):
    close_btn = self.driver.find_elements_by_class_name("android.widget.ImageButton")[0]
    close_btn.click()
    print("Clicked on close button")


def click_btn_path(self, path):
    btn = self.driver.find_element_by_xpath(path)
    btn.click()


def click_back_btn(self):
    self.driver.press_keycode(4)
    print("Clicked on back button")

def switch_on_testmode(self):
    test_mode_switch = self.driver.find_element_by_id("com.xad.sdk.sdkdemo:id/test_mode_switch")
    if test_mode_switch is not None:        
        if not test_mode_switch.get_attribute('checked'):
            print("Test mode is not checked, turning on test mode")
            test_mode_switch.click()
        else:
            print("Test mode is checked")
    else:
        print('Not able to find test mode switch')

