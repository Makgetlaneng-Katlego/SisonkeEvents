package com.example.sisonkeevents

import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : FragmentActivity() {
    private val ticketMaster = ticketMaster()
    private val categories = segmentIDs()
    val user = user()
    
    private var currentCategoryName: String? = null
    private var currentCategoryId: String? = null
    private var currentPage: String = "explore"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPage == "details") {
                    val catName = currentCategoryName
                    val catId = currentCategoryId
                    if (catName != null && catId != null) {
                        showEvents(catName, catId)
                    } else {
                        showHomePage()
                    }
                } else if (currentPage == "events" || currentPage == "my_events" || currentPage == "recently_viewed" || currentPage == "settings" || currentPage == "change_email" || currentPage == "change_password") {
                    showProfilePage()
                } else if (currentPage == "explore" || currentPage == "profile") {
                    showHomePage()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        showLoginPage()
    }

    fun showLoginPage() {
        currentPage = "login"
        setContentView(R.layout.login_page)

        if (user.email == null) {
            user.updateUserDetails("Pfano", "Dzivhani", "example@email.com", "0820000000", "******")
        }
        
        findViewById<Button>(R.id.login_button).setOnClickListener {
            val inputEmail = findViewById<EditText>(R.id.email).text.toString().trim()
            val inputPassword = findViewById<EditText>(R.id.password).text.toString().trim()
            
            if (user.verifyUser(inputEmail, inputPassword)) {
                showHomePage()
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnCreateAcc).setOnClickListener {
            showCreateAccountPage()
        }
    }
    fun showCreateAccountPage() {
        setContentView(R.layout.createacc)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        findViewById<Button>(R.id.btnCreateAccountSubmit).setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhoneNumber.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!user.verifyPasswords(password, confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            user.updateUserDetails(firstName, lastName, email, phone, password)
            showExplorePage()
        }
    }

    private fun showExplorePage() {
        currentPage = "explore"
        setContentView(R.layout.explorepage)
        setupExplorePageListeners()
        setupBottomNavListeners()
    }

    private fun setupExplorePageListeners() {
        findViewById<View>(R.id.musicCategory).setOnClickListener {
            showEvents("Music", categories.musicID)
        }
        findViewById<View>(R.id.comedyCategory).setOnClickListener {
            showEvents("Comedy", categories.comedyID)
        }
        findViewById<View>(R.id.sportsCategory).setOnClickListener {
            showEvents("Sports", categories.sportsID)
        }
        findViewById<View>(R.id.foodCategory).setOnClickListener {
            showEvents("Food and Drinks", categories.foodID)
        }
        findViewById<View>(R.id.educationCategory).setOnClickListener {
            showEvents("Education", categories.educationID)
        }
        findViewById<View>(R.id.familyCategory).setOnClickListener {
            showEvents("Family Events", categories.familyID)
        }
        findViewById<View>(R.id.communityCategory).setOnClickListener {
            showEvents("Community Events", categories.communityID)
        }
        findViewById<View>(R.id.festivalCategory).setOnClickListener {
            showEvents("Festival Events", categories.festivalID)
        }
    }

    private fun showEvents(categoryName: String, categoryId: String) {
        currentPage = "events"
        currentCategoryName = categoryName
        currentCategoryId = categoryId
        
        setContentView(R.layout.show_events)
        setupBottomNavListeners()

        val titleTextView = findViewById<TextView>(R.id.categoryTitle)
        titleTextView.text = categoryName

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            showExplorePage()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.eventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val eventsList = withContext(Dispatchers.IO) {
                ticketMaster.fetchEvents(categoryId)
            }
            recyclerView.adapter = eventAdapter(eventsList) { selectedEvent ->
                showEventDetails(selectedEvent)
            }
        }
    }

    fun showMyEvents() {
        currentPage = "my_events"
        setContentView(R.layout.my_events)
        setupBottomNavListeners()

        val myEventsManager = MyEventsManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.myEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val interestedEvents = myEventsManager.getInterestedEvents()
        recyclerView.adapter = myEventsAdapter(interestedEvents) { selectedEvent ->
            showEventDetails(selectedEvent)
        }

        val tabInterested = findViewById<TextView>(R.id.tabInterested)
        val tabRsvpd = findViewById<TextView>(R.id.tabRsvpd)
        val tabPast = findViewById<TextView>(R.id.tabPast)

        tabInterested.setOnClickListener {
            tabInterested.setBackgroundResource(R.drawable.bg_chip_selected)
            tabInterested.setTextColor(resources.getColor(R.color.white, null))
            tabRsvpd.setBackgroundResource(R.drawable.bg_chip_unselected)
            tabRsvpd.setTextColor(resources.getColor(R.color.light_gray, null))
            tabPast.setBackgroundResource(R.drawable.bg_chip_unselected)
            tabPast.setTextColor(resources.getColor(R.color.light_gray, null))

            recyclerView.adapter = myEventsAdapter(interestedEvents) { selectedEvent ->
                showEventDetails(selectedEvent)
            }
        }

        tabRsvpd.setOnClickListener {
            tabInterested.setBackgroundResource(R.drawable.bg_chip_unselected)
            tabInterested.setTextColor(resources.getColor(R.color.light_gray, null))
            tabRsvpd.setBackgroundResource(R.drawable.bg_chip_selected)
            tabRsvpd.setTextColor(resources.getColor(R.color.white, null))
            tabPast.setBackgroundResource(R.drawable.bg_chip_unselected)
            tabPast.setTextColor(resources.getColor(R.color.light_gray, null))

            recyclerView.adapter = myEventsAdapter(emptyList()) {}
        }

        tabPast.setOnClickListener {
            tabInterested.setBackgroundResource(R.drawable.bg_chip_unselected)
            tabInterested.setTextColor(resources.getColor(R.color.light_gray, null))
            tabRsvpd.setBackgroundResource(R.drawable.bg_chip_unselected)
            tabRsvpd.setTextColor(resources.getColor(R.color.light_gray, null))
            tabPast.setBackgroundResource(R.drawable.bg_chip_selected)
            tabPast.setTextColor(resources.getColor(R.color.white, null))

            recyclerView.adapter = myEventsAdapter(emptyList()) {}
        }
    }

    private fun setupBottomNavListeners() {
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            showHomePage()
        }
        findViewById<View>(R.id.navExplore)?.setOnClickListener {
            showExplorePage()
        }
        findViewById<View>(R.id.navMyEvents)?.setOnClickListener {
            showMyEvents()
        }
        findViewById<View>(R.id.navNotifications)?.setOnClickListener {
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            showProfilePage()
        }
    }

    fun showEventDetails(event: event) {
        currentPage = "details"
        setContentView(R.layout.event_details)

        RecentlyViewedManager(this).saveViewedEvent(event)

        findViewById<TextView>(R.id.detailCategory).text = event.category
        findViewById<TextView>(R.id.detailTitle).text = event.name
        findViewById<TextView>(R.id.detailInterestedCount).text = "${event.interestedCount} interested"
        findViewById<TextView>(R.id.detailDate).text = event.date
        findViewById<TextView>(R.id.detailTime).text = event.time
        findViewById<TextView>(R.id.detailLocation).text = event.location
        findViewById<TextView>(R.id.detailDescription).text = event.description
        findViewById<TextView>(R.id.detailAge).text = "• Age: ${event.age}"
        findViewById<TextView>(R.id.detailParking).text = "• Parking: ${event.parking}"

        findViewById<ImageView>(R.id.detailBackButton).setOnClickListener {
            val catName = currentCategoryName
            val catId = currentCategoryId
            if (catName != null && catId != null) {
                showEvents(catName, catId)
            } else {
                showHomePage()
            }
        }

        findViewById<View>(R.id.btnGetTickets).setOnClickListener {
            if (!event.ticketUrl.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.ticketUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Ticket URL not available", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btnInterested).setOnClickListener {
            MyEventsManager(this).saveInterestedEvent(event)
            Toast.makeText(this, "Marked as interested!", Toast.LENGTH_SHORT).show()
        }

        if (!event.imageUrl.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(event.imageUrl)
                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                    withContext(Dispatchers.Main) {
                        findViewById<ImageView>(R.id.detailEventImage)?.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun showHomePage() {
        currentPage = "home"
        currentCategoryName = null
        currentCategoryId = null

        setContentView(R.layout.homepage)
        findViewById<TextView>(R.id.homepageName).setText("Welcome, ${user.Firstname}")
        setupBottomNavListeners()

        val weekendRecyclerView = findViewById<RecyclerView>(R.id.weekendRecyclerView)
        weekendRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val nearbyRecyclerView = findViewById<RecyclerView>(R.id.nearbyRecyclerView)
        nearbyRecyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val weekendEvents = withContext(Dispatchers.IO) {
                ticketMaster.fetchWeekendEvents("ZA")
            }
            weekendRecyclerView.adapter = WeekendEventAdapter(weekendEvents, lifecycleScope) { selectedEvent ->
                showEventDetails(selectedEvent)
            }

            val nearbyEvents = withContext(Dispatchers.IO) {
                ticketMaster.fetchWeekendEvents("ZA")
            }
            nearbyRecyclerView.adapter = eventAdapter(nearbyEvents) { selectedEvent ->
                showEventDetails(selectedEvent)
            }
        }
    }

    fun showProfilePage() {
        currentPage = "profile"
        setContentView(R.layout.profilepage)
        val displayName = if (!user.Firstname.isNullOrEmpty() && !user.LastName.isNullOrEmpty()) {
            "${user.Firstname} ${user.LastName}"
        } else if (!user.Firstname.isNullOrEmpty()) {
            user.Firstname
        } else {
            "User Profile"
        }
        findViewById<TextView>(R.id.Username).text = displayName
        findViewById<TextView>(R.id.email).text = user.email ?: "no-email@example.com"
        
        setupBottomNavListeners()

        findViewById<View>(R.id.sectionSavedEvents).setOnClickListener {
            showMyEvents()
        }

        findViewById<View>(R.id.sectionRecentlyViewed).setOnClickListener {
            showRecentlyViewed()
        }

        findViewById<View>(R.id.sectionSettings).setOnClickListener {
            showSettingsPage()
        }
        findViewById<View>(R.id.logout).setOnClickListener {
            showLoginPage()
        }
    }

    fun showSettingsPage() {
        currentPage = "settings"
        setContentView(R.layout.settingspage)
        setupBottomNavListeners()

        findViewById<View>(R.id.settingsBack).setOnClickListener {
            showProfilePage()
        }

        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        switchDarkMode.isChecked = (currentNightMode == Configuration.UI_MODE_NIGHT_YES)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        findViewById<View>(R.id.itemEmail).setOnClickListener {
            showChangeEmailPage()
        }
        findViewById<View>(R.id.itemPassword).setOnClickListener {
            showChangePasswordPage()
        }
        findViewById<View>(R.id.itemProfile).setOnClickListener {
            showChangeProfileDetailsPage()
        }
    }

    fun showChangeProfileDetailsPage() {
        currentPage = "change_profile"
        setContentView(R.layout.change_profile_details_page)

        val etNewFirstName = findViewById<EditText>(R.id.etNewFirstName)
        val etNewLastName = findViewById<EditText>(R.id.etNewLastName)
        val etNewPhoneNumber = findViewById<EditText>(R.id.etNewPhoneNumber)

        etNewFirstName.setText(user.Firstname)
        etNewLastName.setText(user.LastName)
        etNewPhoneNumber.setText(user.PhoneNumber)

        findViewById<View>(R.id.btnChangeProfileBack).setOnClickListener {
            showSettingsPage()
        }

        findViewById<Button>(R.id.btnUpdateProfileSubmit).setOnClickListener {
            val updatedFirstName = etNewFirstName.text.toString().trim()
            val updatedLastName = etNewLastName.text.toString().trim()
            val updatedPhone = etNewPhoneNumber.text.toString().trim()

            if (updatedFirstName.isEmpty() || updatedLastName.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            user.Firstname = updatedFirstName
            user.LastName = updatedLastName
            user.PhoneNumber = updatedPhone

            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            showSettingsPage()
        }
    }

    fun showChangePasswordPage() {
        currentPage = "change_password"
        setContentView(R.layout.change_password_page)

        findViewById<View>(R.id.btnChangePasswordBack).setOnClickListener {
            showSettingsPage()
        }

        findViewById<Button>(R.id.btnUpdatePasswordSubmit).setOnClickListener {
            val newPassword = findViewById<EditText>(R.id.etNewPasswordInput).text.toString().trim()
            val confirmPassword = findViewById<EditText>(R.id.etConfirmNewPasswordInput).text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!user.verifyPasswords(newPassword, confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            user.password = newPassword
            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            showSettingsPage()
        }
    }

    fun showChangeEmailPage() {
        currentPage = "change_email"
        setContentView(R.layout.change_email_page)

        val etNewEmailInput = findViewById<EditText>(R.id.etNewEmailInput)
        etNewEmailInput.setText(user.email)
        etNewEmailInput.setSelection(etNewEmailInput.text?.length ?: 0)

        findViewById<View>(R.id.btnChangeEmailBack).setOnClickListener {
            showSettingsPage()
        }

        findViewById<Button>(R.id.btnUpdateEmailSubmit).setOnClickListener {
            val updatedEmail = etNewEmailInput.text.toString().trim()
            if (updatedEmail.isNotEmpty()) {
                user.email = updatedEmail
                Toast.makeText(this, "Email updated successfully!", Toast.LENGTH_SHORT).show()
                showSettingsPage()
            } else {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showRecentlyViewed() {
        currentPage = "recently_viewed"
        setContentView(R.layout.recently_viewed)
        setupBottomNavListeners()

        val recyclerView = findViewById<RecyclerView>(R.id.recentEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val recentlyViewedList = RecentlyViewedManager(this).getViewedEvents()
        recyclerView.adapter = eventAdapter(recentlyViewedList) { selectedEvent ->
            showEventDetails(selectedEvent)
        }

        findViewById<View>(R.id.recentlyViewedBack).setOnClickListener {
            showProfilePage()
        }
    }
}
