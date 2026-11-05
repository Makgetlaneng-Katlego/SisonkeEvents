package com.example.sisonkeevents

import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sisonkeevents.data.EventRepository
import com.example.sisonkeevents.data.EventsResult
import com.example.sisonkeevents.data.OfflineDatabaseHelper
import com.example.sisonkeevents.sso.GoogleSignInManager
import com.example.sisonkeevents.sso.GoogleSignInResult
import com.example.sisonkeevents.sso.SSOConfig
import com.example.sisonkeevents.sso.SSODataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val categories = segmentIDs()

    // FIXED: Made lazy to prevent initialization crash on launch
    val user: user by lazy { user() }

    private val repository: EventRepository by lazy {
        (application as? SisonkeApp)?.repository
            ?: EventRepository(applicationContext, com.example.sisonkeevents.data.TicketMasterEventApi())
    }

    // Google SSO manager (initialized in onCreate)
    private lateinit var googleSignInManager: GoogleSignInManager

    private var currentCategoryName: String? = null
    private var currentCategoryId: String? = null
    private var currentPage: String = "login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google SSO manager
        googleSignInManager = GoogleSignInManager(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPage) {
                    "details" -> {
                        val n = currentCategoryName; val i = currentCategoryId
                        if (n != null && i != null) showEvents(n, i) else showHomePage()
                    }
                    "events", "my_events", "recently_viewed", "settings",
                    "change_email", "change_password", "change_profile" -> showProfilePage()
                    "explore", "profile" -> showHomePage()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        // Restore from DB if logged in; otherwise show login.
        val db = OfflineDatabaseHelper.get()
        if (db.isLoggedIn()) {
            val state = db.loadSessionState()
            if (state != null) {
                currentPage = state.page ?: "home"
                currentCategoryName = state.categoryName
                currentCategoryId = state.categoryId
                navigateToPage(currentPage, currentCategoryName, currentCategoryId)
            } else {
                showHomePage()
            }
        } else {
            showLoginPage()
        }
    }

    private fun persistNavigationState() {
        try {
            val db = OfflineDatabaseHelper.get()
            if (db.isLoggedIn()) {
                db.saveSessionState(
                    loggedIn = true,
                    page = currentPage,
                    categoryName = currentCategoryName,
                    categoryId = currentCategoryId
                )
            }
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "persistNavigationState failed", t)
        }
    }

    private fun navigateToPage(page: String, categoryName: String?, categoryId: String?) {
        when (page) {
            "login" -> showLoginPage()
            "create_account" -> showCreateAccountPage()
            "home" -> showHomePage()
            "explore" -> showExplorePage()
            "my_events" -> showMyEvents()
            "profile" -> showProfilePage()
            "settings" -> showSettingsPage()
            "recently_viewed" -> showRecentlyViewed()
            "change_email" -> showChangeEmailPage()
            "change_password" -> showChangePasswordPage()
            "change_profile" -> showChangeProfileDetailsPage()
            "events" -> {
                if (categoryName != null && categoryId != null) showEvents(categoryName, categoryId)
                else showHomePage()
            }
            "details" -> {
                if (categoryName != null && categoryId != null) showEvents(categoryName, categoryId)
                else showHomePage()
            }
            else -> showHomePage()
        }
    }

    // ------------------------------------------------------------------ LOGIN

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
                OfflineDatabaseHelper.get().setLoggedIn(true)
                showHomePage()
            } else {
                Toast.makeText(this, getString(R.string.toast_invalid_credentials), Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnCreateAcc).setOnClickListener { showCreateAccountPage() }

        // Google Sign-In button (safe call: no-op if layout doesn't define it)
        findViewById<Button>(R.id.btnGoogleSignIn)?.setOnClickListener {
            startGoogleSignIn()
        }
    }

    fun showCreateAccountPage() {
        currentPage = "create_account"
        setContentView(R.layout.createacc)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        findViewById<Button>(R.id.btnCreateAccountSubmit).setOnClickListener {
            val fn = etFirstName.text.toString().trim()
            val ln = etLastName.text.toString().trim()
            val em = etEmail.text.toString().trim()
            val ph = etPhoneNumber.text.toString().trim()
            val pw = etPassword.text.toString().trim()
            val cp = etConfirmPassword.text.toString().trim()

            if (fn.isEmpty() || ln.isEmpty() || em.isEmpty() || ph.isEmpty() || pw.isEmpty() || cp.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!user.verifyPasswords(pw, cp)) {
                Toast.makeText(this, getString(R.string.toast_passwords_dont_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Calls the new BCrypt backed function
            user.updateUserDetails(fn, ln, em, ph, pw)
            OfflineDatabaseHelper.get().setLoggedIn(true)
            showExplorePage()
        }
    }

    // ------------------------------------------------------------------ GOOGLE SSO

    private fun startGoogleSignIn() {
        lifecycleScope.launch {
            when (val result = googleSignInManager.signIn()) {
                is GoogleSignInResult.Success -> {
                    onGoogleSignInSuccess(
                        email = result.profile.email,
                        displayName = result.profile.displayName,
                        subject = result.profile.subject
                    )
                }
                is GoogleSignInResult.Error -> {
                    Toast.makeText(
                        this@MainActivity,
                        "Google sign-in failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Completes a Google sign-in:
     *
     *   1. Look up an existing link by Google `sub` (stable across email
     *      changes). Fall back to email match for the "local signup, then
     *      Google with same email" case.
     *   2. If linked → refresh profile fields, leave authProvider alone.
     *      If new    → create the single user_account row with
     *                  authProvider = "GOOGLE" and a throwaway password.
     *   3. Upsert the (provider, sub) identity row.
     *   4. Only on full success: set the logged-in flag and navigate home.
     *
     * All SQLite work happens on Dispatchers.IO.
     */
    private fun onGoogleSignInSuccess(
        email: String?,
        displayName: String?,
        subject: String
    ) {
        val emailSafe = email ?: run {
            Toast.makeText(this, "Google did not return an email", Toast.LENGTH_SHORT).show()
            return
        }
        val safeName = displayName ?: emailSafe.substringBefore("@")
        val parts = safeName.split(" ", limit = 2)
        val firstName = parts.getOrNull(0) ?: ""
        val lastName = parts.getOrNull(1) ?: ""

        lifecycleScope.launch {
            val dataStore = SSODataStore()

            val localUserId: Long? = withContext(Dispatchers.IO) {
                // Prefer the stable (provider, sub) link. Fall back to email
                // so an existing local account with the same email is reused.
                val linkedId = dataStore.findLinkedUserAccountId(subject)
                    ?: dataStore.findUserAccountIdByEmail(emailSafe)

                if (linkedId != null) {
                    // Returning user — sync profile fields, keep authProvider
                    user.updateEmail(emailSafe)
                    user.updateProfileDetails(firstName, lastName, user.PhoneNumber ?: "")
                    linkedId
                } else {
                    // New Google user — random throwaway password; user can
                    // replace it later via Settings → Change password (which
                    // flips authProvider back to "LOCAL").
                    user.updateUserDetails(
                        firstName,
                        lastName,
                        emailSafe,
                        "",
                        generateRandomPassword(),
                        "GOOGLE"
                    )
                    dataStore.findUserAccountIdByEmail(emailSafe)
                }
            }

            if (localUserId == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Could not create local account for Google user.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val linked = withContext(Dispatchers.IO) {
                dataStore.linkOrUpdateIdentity(
                    subject = subject,
                    email = emailSafe,
                    displayName = safeName,
                    localUserId = localUserId
                )
            }
            if (!linked) {
                Toast.makeText(
                    this@MainActivity,
                    "Could not link Google account to local profile.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            OfflineDatabaseHelper.get().setLoggedIn(true)
            Toast.makeText(this@MainActivity, "Welcome, $safeName!", Toast.LENGTH_SHORT).show()
            // showHomePage() persists navigation state itself.
            showHomePage()
        }
    }

    private fun generateRandomPassword(): String {
        val bytes = ByteArray(24)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        )
    }

    // ------------------------------------------------------------------ EXPLORE

    private fun showExplorePage() {
        currentPage = "explore"
        setContentView(R.layout.explorepage)
        setupExplorePageListeners()
        setupBottomNavListeners()
        persistNavigationState()
    }

    private fun setupExplorePageListeners() {
        findViewById<View>(R.id.musicCategory).setOnClickListener {
            showEvents(getString(R.string.category_music), categories.musicID)
        }
        findViewById<View>(R.id.comedyCategory).setOnClickListener {
            showEvents(getString(R.string.category_comedy), categories.comedyID)
        }
        findViewById<View>(R.id.sportsCategory).setOnClickListener {
            showEvents(getString(R.string.category_sports), categories.sportsID)
        }
        findViewById<View>(R.id.foodCategory).setOnClickListener {
            showEvents(getString(R.string.category_food_drinks), categories.foodID)
        }
        findViewById<View>(R.id.educationCategory).setOnClickListener {
            showEvents(getString(R.string.category_education), categories.educationID)
        }
        findViewById<View>(R.id.familyCategory).setOnClickListener {
            showEvents(getString(R.string.category_family), categories.familyID)
        }
        findViewById<View>(R.id.communityCategory).setOnClickListener {
            showEvents(getString(R.string.category_community), categories.communityID)
        }
        findViewById<View>(R.id.festivalCategory).setOnClickListener {
            showEvents(getString(R.string.category_festival), categories.festivalID)
        }
    }

    // ------------------------------------------------------------------ EVENTS

    private fun showEvents(categoryName: String, categoryId: String) {
        currentPage = "events"
        currentCategoryName = categoryName
        currentCategoryId = categoryId

        setContentView(R.layout.show_events)
        setupBottomNavListeners()

        findViewById<TextView>(R.id.categoryTitle).text = categoryName
        findViewById<ImageView>(R.id.backButton).setOnClickListener { showExplorePage() }

        val recyclerView = findViewById<RecyclerView>(R.id.eventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val lastUpdatedText = findOptionalView("lastUpdatedText") as? TextView
        val refreshButton = findOptionalView("refreshButton")

        fun load(forceRefresh: Boolean) {
            lifecycleScope.launch {
                val result = repository.getEventsForCategory(categoryId, forceRefresh)
                recyclerView.adapter = eventAdapter(result.events) { showEventDetails(it) }
                lastUpdatedText?.let { tv ->
                    tv.text = formatRelativeTime(result.lastUpdated)
                    tv.visibility =
                        if (result.fromCache && result.lastUpdated != null) View.VISIBLE
                        else View.GONE
                }
                if (forceRefresh && result.fromCache) {
                    Toast.makeText(this@MainActivity, R.string.updated_just_now, Toast.LENGTH_SHORT).show()
                }
            }
        }

        load(false)
        refreshButton?.setOnClickListener { load(true) }
        persistNavigationState()
    }

    fun showMyEvents() {
        currentPage = "my_events"
        setContentView(R.layout.my_events)
        setupBottomNavListeners()

        val myEventsManager = MyEventsManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.myEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val interestedEvents = myEventsManager.getInterestedEvents()
        recyclerView.adapter = myEventsAdapter(interestedEvents) { showEventDetails(it) }

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
            recyclerView.adapter = myEventsAdapter(interestedEvents) { showEventDetails(it) }
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

        persistNavigationState()
    }

    private fun setupBottomNavListeners() {
        findViewById<View>(R.id.navHome)?.setOnClickListener { showHomePage() }
        findViewById<View>(R.id.navExplore)?.setOnClickListener { showExplorePage() }
        findViewById<View>(R.id.navMyEvents)?.setOnClickListener { showMyEvents() }
        findViewById<View>(R.id.navNotifications)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_notifications_clicked), Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.navProfile)?.setOnClickListener { showProfilePage() }
    }

    // ------------------------------------------------------------------ DETAILS

    fun showEventDetails(event: event) {
        currentPage = "details"
        setContentView(R.layout.event_details)

        RecentlyViewedManager(this).saveViewedEvent(event)

        findViewById<TextView>(R.id.detailCategory).text = event.category
        findViewById<TextView>(R.id.detailTitle).text = event.name
        findViewById<TextView>(R.id.detailInterestedCount).text =
            resources.getQuantityString(R.plurals.interested_count, event.interestedCount, event.interestedCount)
        findViewById<TextView>(R.id.detailDate).text = event.date
        findViewById<TextView>(R.id.detailTime).text = event.time
        findViewById<TextView>(R.id.detailLocation).text = event.location
        findViewById<TextView>(R.id.detailDescription).text = event.description
        findViewById<TextView>(R.id.detailAge).text = getString(R.string.format_age, event.age)
        findViewById<TextView>(R.id.detailParking).text = getString(R.string.format_parking, event.parking)

        findViewById<ImageView>(R.id.detailBackButton).setOnClickListener {
            val n = currentCategoryName; val i = currentCategoryId
            if (n != null && i != null) showEvents(n, i) else showHomePage()
        }

        findViewById<View>(R.id.btnGetTickets).setOnClickListener {
            if (!event.ticketUrl.isNullOrEmpty()) {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.ticketUrl))) }
                catch (e: Exception) { e.printStackTrace() }
            } else {
                Toast.makeText(this, getString(R.string.toast_ticket_url_unavailable), Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btnInterested).setOnClickListener {
            MyEventsManager(this).saveInterestedEvent(event)
            lifecycleScope.launch {
                val queued = repository.markInterested(event.id)
                val msg = if (queued) R.string.action_queued_offline else R.string.toast_marked_interested
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        if (!event.imageUrl.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(event.imageUrl)
                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                    withContext(Dispatchers.Main) {
                        findViewById<ImageView>(R.id.detailEventImage)?.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        persistNavigationState()
    }

    // ------------------------------------------------------------------ HOME

    fun showHomePage() {
        currentPage = "home"
        currentCategoryName = null
        currentCategoryId = null

        setContentView(R.layout.homepage)
        findViewById<TextView>(R.id.homepageName).text = user.Firstname ?: ""
        setupBottomNavListeners()

        val weekendRecyclerView = findViewById<RecyclerView>(R.id.weekendRecyclerView)
        weekendRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val nearbyRecyclerView = findViewById<RecyclerView>(R.id.nearbyRecyclerView)
        nearbyRecyclerView.layoutManager = LinearLayoutManager(this)

        val homeRefreshButton = findOptionalView("homeRefreshButton")

        fun load(forceRefresh: Boolean) {
            lifecycleScope.launch {
                val result: EventsResult = repository.getWeekendEvents("ZA", forceRefresh)
                weekendRecyclerView.adapter =
                    WeekendEventAdapter(result.events, lifecycleScope) { showEventDetails(it) }
                nearbyRecyclerView.adapter =
                    eventAdapter(result.events) { showEventDetails(it) }
            }
        }

        load(false)
        homeRefreshButton?.setOnClickListener { load(true) }
        persistNavigationState()
    }

    // ------------------------------------------------------------------ PROFILE

    fun showProfilePage() {
        currentPage = "profile"
        setContentView(R.layout.profilepage)

        val displayName = if (!user.Firstname.isNullOrEmpty() && !user.LastName.isNullOrEmpty()) {
            "${user.Firstname} ${user.LastName}"
        } else if (!user.Firstname.isNullOrEmpty()) user.Firstname
        else getString(R.string.fallback_user_profile)

        findViewById<TextView>(R.id.Username).text = displayName
        findViewById<TextView>(R.id.email).text = user.email ?: getString(R.string.fallback_no_email)
        setupBottomNavListeners()

        findViewById<View>(R.id.sectionSavedEvents).setOnClickListener { showMyEvents() }
        findViewById<View>(R.id.sectionRecentlyViewed).setOnClickListener { showRecentlyViewed() }
        findViewById<View>(R.id.sectionSettings).setOnClickListener { showSettingsPage() }
        findViewById<View>(R.id.logout).setOnClickListener {
            OfflineDatabaseHelper.get().setLoggedIn(false)
            SSODataStore().clearAllSessions()
            showLoginPage()
        }

        persistNavigationState()
    }

    // ------------------------------------------------------------------ SETTINGS

    fun showSettingsPage() {
        currentPage = "settings"
        setContentView(R.layout.settingspage)
        setupBottomNavListeners()

        findViewById<View>(R.id.settingsBack).setOnClickListener { showProfilePage() }

        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        switchDarkMode.isChecked = (nightMode == Configuration.UI_MODE_NIGHT_YES)
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        findViewById<View>(R.id.itemEmail).setOnClickListener { showChangeEmailPage() }
        findViewById<View>(R.id.itemPassword).setOnClickListener { showChangePasswordPage() }
        findViewById<View>(R.id.itemProfile).setOnClickListener { showChangeProfileDetailsPage() }

        updateCurrentLanguageLabel()
        findViewById<View>(R.id.itemLanguage).setOnClickListener { showLanguagePickerDialog() }

        persistNavigationState()
    }

    private fun getCurrentLanguageCode(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val rawCode = if (!appLocales.isEmpty) appLocales.toLanguageTags()
        else Locale.getDefault().language
        return when {
            rawCode.startsWith("nso") -> "nso"
            rawCode.startsWith("ve") -> "ve"
            else -> "en"
        }
    }

    private fun updateCurrentLanguageLabel() {
        val tv = findViewById<TextView>(R.id.tvCurrentLanguage) ?: return
        tv.text = when (getCurrentLanguageCode()) {
            "nso" -> getString(R.string.language_sepedi)
            "ve" -> getString(R.string.language_tshivenda)
            else -> getString(R.string.language_english)
        }
    }

    private fun showLanguagePickerDialog() {
        val codes = arrayOf("en", "nso", "ve")
        val options = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_sepedi),
            getString(R.string.language_tshivenda)
        )
        val currentIndex = codes.indexOf(getCurrentLanguageCode()).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_choose_language))
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codes[which]))
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    // ------------------------------------------------------------------ CHANGE DETAILS

    fun showChangeProfileDetailsPage() {
        currentPage = "change_profile"
        setContentView(R.layout.change_profile_details_page)

        val etNewFirstName = findViewById<EditText>(R.id.etNewFirstName)
        val etNewLastName = findViewById<EditText>(R.id.etNewLastName)
        val etNewPhoneNumber = findViewById<EditText>(R.id.etNewPhoneNumber)
        etNewFirstName.setText(user.Firstname)
        etNewLastName.setText(user.LastName)
        etNewPhoneNumber.setText(user.PhoneNumber)

        findViewById<View>(R.id.btnChangeProfileBack).setOnClickListener { showSettingsPage() }
        findViewById<Button>(R.id.btnUpdateProfileSubmit).setOnClickListener {
            val fn = etNewFirstName.text.toString().trim()
            val ln = etNewLastName.text.toString().trim()
            val ph = etNewPhoneNumber.text.toString().trim()
            if (fn.isEmpty() || ln.isEmpty() || ph.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_all_details), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Calls DB update
            user.updateProfileDetails(fn, ln, ph)
            Toast.makeText(this, getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show()
            showSettingsPage()
        }

        persistNavigationState()
    }

    fun showChangePasswordPage() {
        currentPage = "change_password"
        setContentView(R.layout.change_password_page)

        findViewById<View>(R.id.btnChangePasswordBack).setOnClickListener { showSettingsPage() }
        findViewById<Button>(R.id.btnUpdatePasswordSubmit).setOnClickListener {
            val np = findViewById<EditText>(R.id.etNewPasswordInput).text.toString().trim()
            val cp = findViewById<EditText>(R.id.etConfirmNewPasswordInput).text.toString().trim()
            if (np.isEmpty() || cp.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_password_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!user.verifyPasswords(np, cp)) {
                Toast.makeText(this, getString(R.string.toast_passwords_dont_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Calls DB update (hashes the password and flips authProvider to LOCAL)
            user.updatePassword(np)
            Toast.makeText(this, getString(R.string.toast_password_updated), Toast.LENGTH_SHORT).show()
            showSettingsPage()
        }

        persistNavigationState()
    }

    fun showChangeEmailPage() {
        currentPage = "change_email"
        setContentView(R.layout.change_email_page)

        val etNewEmailInput = findViewById<EditText>(R.id.etNewEmailInput)
        etNewEmailInput.setText(user.email)
        etNewEmailInput.setSelection(etNewEmailInput.text?.length ?: 0)

        findViewById<View>(R.id.btnChangeEmailBack).setOnClickListener { showSettingsPage() }
        findViewById<Button>(R.id.btnUpdateEmailSubmit).setOnClickListener {
            val updated = etNewEmailInput.text.toString().trim()
            if (updated.isNotEmpty()) {
                // Calls DB update
                user.updateEmail(updated)
                Toast.makeText(this, getString(R.string.toast_email_updated), Toast.LENGTH_SHORT).show()
                showSettingsPage()
            } else {
                Toast.makeText(this, getString(R.string.toast_enter_valid_email), Toast.LENGTH_SHORT).show()
            }
        }

        persistNavigationState()
    }

    // ------------------------------------------------------------------ RECENTLY VIEWED

    fun showRecentlyViewed() {
        currentPage = "recently_viewed"
        setContentView(R.layout.recently_viewed)
        setupBottomNavListeners()

        val recyclerView = findViewById<RecyclerView>(R.id.recentEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val list = RecentlyViewedManager(this).getViewedEvents()
        recyclerView.adapter = eventAdapter(list) { showEventDetails(it) }
        findViewById<View>(R.id.recentlyViewedBack).setOnClickListener { showProfilePage() }

        persistNavigationState()
    }

    // ------------------------------------------------------------------ HELPERS

    private fun findOptionalView(resIdName: String): View? {
        val id = resources.getIdentifier(resIdName, "id", packageName)
        return if (id != 0) findViewById(id) else null
    }

    private fun formatRelativeTime(ts: Long?): String {
        if (ts == null) return ""
        val mins = ((System.currentTimeMillis() - ts) / 60_000L).coerceAtLeast(0)
        return when {
            mins < 1 -> getString(R.string.updated_just_now)
            mins < 60 -> getString(R.string.updated_minutes_ago, mins)
            else -> getString(R.string.updated_hours_ago, mins / 60)
        }
    }
}