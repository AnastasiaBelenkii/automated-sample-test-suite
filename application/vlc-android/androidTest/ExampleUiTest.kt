package org.videolan.vlc

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleUiTest {

    // Replace "MainActivity" with VLC’s actual main activity
    @get:Rule
    val activityRule = ActivityScenarioRule(StartActivity::class.java)

    @Test
    fun helloWorldTest() {
        // Replace with a real UI element ID from VLC (e.g., "playButton")
        onView(withId(R.id.welcome_title))
            .check(matches(isDisplayed()))
	    }
}
