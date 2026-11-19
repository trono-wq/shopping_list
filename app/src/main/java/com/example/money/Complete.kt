
package com.example.shoppinglist

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat

class Complete : AppCompatActivity() {

    private var placeExpanded = false
    private var categoryExpanded = false
    private var itemExpanded = false
    private var editMode = false

    private var placeChanged = false
    private var categoryChanged = false
    private var itemChanged = false

    private val gray = Color.rgb(102, 102, 102)
    private val muted = Color.rgb(153, 153, 153)
    private val white = Color.WHITE
    private val orange = Color.rgb(220, 101, 0)

    private val statePreferences by lazy {
        getSharedPreferences("complete_list_state", MODE_PRIVATE)
    }

    private val dataPreferences by lazy {
        getSharedPreferences("complete_list_data", MODE_PRIVATE)
    }

    private lateinit var editButton: AppCompatButton
    private lateinit var placeName: TextView
    private lateinit var categoryName: TextView
    private lateinit var itemName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.complete)

        window.setBackgroundDrawableResource(android.R.color.black)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(android.R.id.content)
        ) { view, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                bars.left + 20,
                bars.top + 50,
                bars.right + 20,
                bars.bottom + 50
            )

            insets
        }

        editButton = findViewById(R.id.c_edit)

        placeName = findViewById(R.id.a_p_name)
        categoryName = findViewById(R.id.a_c_name)
        itemName = findViewById(R.id.a_i_name)

        // LOAD SAVED NAMES

        placeName.text = dataPreferences.getString(
            "place_name",
            placeName.text.toString()
        )

        categoryName.text = dataPreferences.getString(
            "category_name",
            categoryName.text.toString()
        )

        itemName.text = dataPreferences.getString(
            "item_name",
            itemName.text.toString()
        )

        // LOAD SAVED EXPANSION STATES

        placeExpanded = statePreferences.getBoolean(
            "place_expanded", false
        )

        categoryExpanded = statePreferences.getBoolean(
            "category_expanded", false
        )

        itemExpanded = statePreferences.getBoolean(
            "item_expanded", false
        )

        updateExpansionVisibility()

        // EDIT / SAVE

        editButton.setOnClickListener {

            if (!editMode) {
                editMode = true

                placeChanged = false
                categoryChanged = false
                itemChanged = false

                updateEditVisibility()
                updateNameColors()
                updateSaveButton()

            } else {

                if (hasChanges()) {
                    dataPreferences.edit()
                        .putString("place_name", placeName.text.toString())
                        .putString("category_name", categoryName.text.toString())
                        .putString("item_name", itemName.text.toString())
                        .apply()
                }

                editMode = false

                placeChanged = false
                categoryChanged = false
                itemChanged = false

                updateEditVisibility()
                updateNameColors()
                updateSaveButton()
            }
        }

        // PLACE NAME

        placeName.setOnClickListener {
            if (editMode) {
                editName(placeName, "Place") {
                    placeChanged = true
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

        // CATEGORY NAME

        categoryName.setOnClickListener {
            if (editMode) {
                editName(categoryName, "Category") {
                    categoryChanged = true
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

        // ITEM NAME

        itemName.setOnClickListener {
            if (editMode) {
                editName(itemName, "Item") {
                    itemChanged = true
                    updateNameColors()
                    updateSaveButton()
                }
            }
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

        // INITIAL STATE

        updateEditVisibility()
        updateNameColors()
        updateSaveButton()
    }

    // EDIT NAME

    private fun editName(
        textView: TextView,
        hint: String,
        onChanged: () -> Unit
    ) {

        val input = EditText(this).apply {
            setText(textView.text)
            setSelection(text.length)
            this.hint = hint
            setTextColor(white)
            setHintTextColor(muted)
            textSize = 18f
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        AlertDialog.Builder(this)
            .setTitle("Edit $hint")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->

                val newName = input.text.toString().trim()

                if (newName.isNotEmpty()) {
                    textView.text = newName
                    onChanged()
                }
            }
            .show()
    }

    // CHANGE NAME COLORS

    private fun updateNameColors() {

        if (!editMode) {
            placeName.setTextColor(white)
            categoryName.setTextColor(white)
            itemName.setTextColor(white)
            return
        }

        placeName.setTextColor(
            if (placeChanged) white else muted
        )

        categoryName.setTextColor(
            if (categoryChanged) white else muted
        )

        itemName.setTextColor(
            if (itemChanged) white else muted
        )
    }

    // SAVE BUTTON COLOR

    private fun hasChanges(): Boolean {
        return placeChanged || categoryChanged || itemChanged
    }

    private fun updateSaveButton() {

        editButton.text = if (editMode) "Save" else "Edit"

        editButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                if (editMode && hasChanges()) orange else gray
            )
    }

    // EXPANSION VISIBILITY

    private fun updateExpansionVisibility() {

        findViewById<View>(R.id.category_section).visibility =
            if (placeExpanded) View.VISIBLE else View.GONE

        findViewById<View>(R.id.item_section).visibility =
            if (placeExpanded && categoryExpanded) {
                View.VISIBLE
            } else {
                View.GONE
            }

        findViewById<View>(R.id.item_brand_bar).visibility =
            if (placeExpanded && categoryExpanded && itemExpanded) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    // EDIT MODE VISIBILITY

    private fun updateEditVisibility() {

        val visibility = if (editMode) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // PLUS BUTTONS

        findViewById<View>(R.id.c_p_add).visibility = visibility
        findViewById<View>(R.id.c_c_add).visibility = visibility
        findViewById<View>(R.id.c_i_add).visibility = visibility

        // SORT BUTTONS

        findViewById<View>(R.id.c_p_sort).visibility = visibility
        findViewById<View>(R.id.c_c_sort).visibility = visibility
        findViewById<View>(R.id.c_i_sort).visibility = visibility

        // ADD TO LIST BUTTONS

        findViewById<View>(R.id.c_p_add_list).visibility = visibility
        findViewById<View>(R.id.c_c_add_list).visibility = visibility
        findViewById<View>(R.id.c_i_add_list).visibility = visibility
    }
}
