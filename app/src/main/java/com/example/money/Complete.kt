package com.example.shoppinglist

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
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

    // ACTIVE NAME EDITOR

    private var activeInput: EditText? = null
    private var activeTextView: TextView? = null
    private var activeParent: ViewGroup? = null
    private var activeIndex = -1
    private var activeLayoutParams: ViewGroup.LayoutParams? = null
    private var activeOnChanged: (() -> Unit)? = null

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

                // FINISH CURRENT NAME EDITING FIRST

                finishNameEditing()

                // SAVE CHANGES

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

        // FINISH ANY PREVIOUS NAME EDIT

        finishNameEditing()

        val parent = textView.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(textView)

        if (index == -1) return

        val layoutParams = textView.layoutParams

        val input = EditText(this).apply {

            id = textView.id
            setText(textView.text)
            setSelection(textView.text.length)

            this.hint = hint

            setTextColor(white)
            setHintTextColor(muted)

            textSize = textView.textSize /
                    resources.displayMetrics.scaledDensity

            typeface = textView.typeface
            gravity = textView.gravity

            setPadding(
                textView.paddingLeft,
                textView.paddingTop,
                textView.paddingRight,
                textView.paddingBottom
            )

            setSingleLine(true)
            background = null

            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        // REPLACE TEXTVIEW WITH EDITTEXT

        parent.removeViewAt(index)
        parent.addView(input, index, layoutParams)

        // TRACK ACTIVE EDITOR

        activeInput = input
        activeTextView = textView
        activeParent = parent
        activeIndex = index
        activeLayoutParams = layoutParams
        activeOnChanged = onChanged

        // DETECT CHANGES WHILE TYPING

        input.addTextChangedListener(object : android.text.TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                val newName = s.toString().trim()
                val oldName = textView.text.toString()

                val changed = newName.isNotEmpty() && newName != oldName

                when (textView.id) {

                    R.id.a_p_name -> placeChanged = changed
                    R.id.a_c_name -> categoryChanged = changed
                    R.id.a_i_name -> itemChanged = changed
                }

                updateNameColors()
                updateSaveButton()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // FOCUS AND KEYBOARD

        input.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus && activeInput === input) {
                finishNameEditing()
            }
        }

        input.setOnEditorActionListener { _, _, _ ->

            finishNameEditing()
            true
        }

        input.requestFocus()

        input.post {

            if (activeInput === input && input.hasFocus()) {

                val imm = getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

                imm.showSoftInput(
                    input,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
    }

    // FINISH NAME EDITING

    private fun finishNameEditing() {

        val input = activeInput ?: return
        val textView = activeTextView ?: return
        val parent = activeParent ?: return
        val index = activeIndex
        val layoutParams = activeLayoutParams
        val onChanged = activeOnChanged

        // CLEAR ACTIVE EDITOR FIRST

        activeInput = null
        activeTextView = null
        activeParent = null
        activeIndex = -1
        activeLayoutParams = null
        activeOnChanged = null

        // GET NEW NAME

        val newName = input.text.toString().trim()
        val oldName = textView.text.toString()

        // UPDATE ONLY IF NAME IS NOT EMPTY

        if (newName.isNotEmpty() && newName != oldName) {

            textView.text = newName
            onChanged?.invoke()
        }

        // HIDE KEYBOARD AND CLEAR FOCUS

        val imm = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        imm.hideSoftInputFromWindow(input.windowToken, 0)

        input.clearFocus()

        window.decorView.clearFocus()

        // RESTORE ORIGINAL TEXTVIEW

        input.setOnFocusChangeListener(null)

        val currentIndex = parent.indexOfChild(input)

        if (currentIndex != -1) {

            parent.removeView(input)

            parent.addView(
                textView,
                currentIndex,
                layoutParams
            )
        }
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

        // UPDATE DROPDOWN SYMBOLS

        updateDropdownSymbols()
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

    // UPDATE DROPDOWN SYMBOLS

    private fun updateDropdownSymbols() {

        findViewById<TextView>(R.id.a_p_expand).text =
            if (placeExpanded) "Ʌ" else "V"

        findViewById<TextView>(R.id.a_c_expand).text =
            if (categoryExpanded) "Ʌ" else "V"

        findViewById<TextView>(R.id.a_i_expand).text =
            if (itemExpanded) "Ʌ" else "V"
    }
}