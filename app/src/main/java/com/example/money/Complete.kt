
package com.example.shoppinglist

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat

class Complete : AppCompatActivity() {

    private var placeExpanded = false
    private var categoryExpanded = false
    private var itemExpanded = false
    private var editMode = false

    private val statePreferences by lazy {
        getSharedPreferences("complete_list_state", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.complete)

        window.setBackgroundDrawableResource(android.R.color.black)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                bars.left + 20,
                bars.top + 50,
                bars.right + 20,
                bars.bottom + 50
            )

            insets
        }

        // LOAD SAVED EXPANSION STATES
        placeExpanded = statePreferences.getBoolean("place_expanded", false)
        categoryExpanded = statePreferences.getBoolean("category_expanded", false)
        itemExpanded = statePreferences.getBoolean("item_expanded", false)

        updateExpansionVisibility()

        // EDIT MODE
        findViewById<View>(R.id.c_edit).setOnClickListener {
            editMode = !editMode
            updateEditVisibility()
        }

        // PLACE DROPDOWN
        findViewById<View>(R.id.a_p_expand).setOnClickListener {
            placeExpanded = !placeExpanded

            statePreferences.edit()
                .putBoolean("place_expanded", placeExpanded)
                .apply()

            updateExpansionVisibility()
        }

        // CATEGORY DROPDOWN
        findViewById<View>(R.id.a_c_expand).setOnClickListener {
            categoryExpanded = !categoryExpanded

            statePreferences.edit()
                .putBoolean("category_expanded", categoryExpanded)
                .apply()

            updateExpansionVisibility()
        }

        // ITEM DROPDOWN
        findViewById<View>(R.id.a_i_expand).setOnClickListener {
            itemExpanded = !itemExpanded

            statePreferences.edit()
                .putBoolean("item_expanded", itemExpanded)
                .apply()

            updateExpansionVisibility()
        }

        // SHOPPING PAGE
        findViewById<TextView>(R.id.a_shopping).setOnClickListener {
            finish()
        }

        // INITIAL EDIT MODE
        updateEditVisibility()
    }

    private fun updateExpansionVisibility() {

        findViewById<View>(R.id.category_section).visibility =
            if (placeExpanded) View.VISIBLE else View.GONE

        findViewById<View>(R.id.item_section).visibility =
            if (placeExpanded && categoryExpanded) View.VISIBLE else View.GONE

        findViewById<View>(R.id.item_brand_bar).visibility =
            if (placeExpanded && categoryExpanded && itemExpanded) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updateEditVisibility() {

        val visibility = if (editMode) View.VISIBLE else View.GONE

        // PLUS BUTTONS
        findViewById<View>(R.id.c_p_add).visibility = visibility
        findViewById<View>(R.id.c_c_add).visibility = visibility
        findViewById<View>(R.id.c_i_add).visibility = visibility

        // ADD TO LIST BUTTONS
        findViewById<View>(R.id.c_p_add_list).visibility = visibility
        findViewById<View>(R.id.c_c_add_list).visibility = visibility
        findViewById<View>(R.id.c_i_add_list).visibility = visibility
    }
}
