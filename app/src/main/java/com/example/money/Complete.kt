
package com.example.shoppinglist

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher

class Complete : AppCompatActivity() {

    private var editMode = false

    private var placeChanged = false
    private var categoryChanged = false
    private var itemChanged = false

    private var brandChanged = false
    private var nameChanged = false
    private var weightChanged = false
    private var quantityChanged = false

    // LAST SAVED NAMES
    private var savedPlaceName = ""
    private var savedCategoryName = ""
    private var savedItemName = ""

    private var savedBrandName = ""
    private var savedNameName = ""
    private var savedWeightName = ""
    private var savedQuantityName = ""

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

    private lateinit var scroll: LinearLayout
    private lateinit var editButton: AppCompatButton

    private lateinit var originalGroup: GroupUi

    private lateinit var placeName: TextView
    private lateinit var categoryName: TextView
    private lateinit var itemName: TextView

    private lateinit var brandName: TextView
    private lateinit var nameName: TextView
    private lateinit var weightName: TextView
    private lateinit var quantityName: TextView

    private data class GroupUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,
        val isOriginal: Boolean,
        var placeExpanded: Boolean = false,
        var categoryExpanded: Boolean = false,
        var itemExpanded: Boolean = false
    )

    private val groups = mutableListOf<GroupUi>()

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

        findViewById<TextView>(R.id.a_shopping).setOnClickListener {
            finish()
        }

        scroll = findViewById(R.id.scroll)
        editButton = findViewById(R.id.c_edit)

        // ORIGINAL GROUP
        val originalRoot = findViewById<LinearLayout>(R.id.group)

        originalGroup = setupGroup(
            root = originalRoot,
            idMap = SparseIntArray(),
            isOriginal = true
        )

        groups.add(originalGroup)

        placeName = findInGroup<TextView>(
            originalGroup,
            R.id.a_p_name
        )!!

        categoryName = findInGroup<TextView>(
            originalGroup,
            R.id.a_c_name
        )!!

        itemName = findInGroup<TextView>(
            originalGroup,
            R.id.a_i_name
        )!!

        brandName = findInGroup<TextView>(
            originalGroup,
            R.id.a_b_brand
        )!!

        nameName = findInGroup<TextView>(
            originalGroup,
            R.id.a_b_name
        )!!

        weightName = findInGroup<TextView>(
            originalGroup,
            R.id.a_b_weight
        )!!

        quantityName = findInGroup<TextView>(
            originalGroup,
            R.id.a_b_quantity
        )!!

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

        brandName.text = dataPreferences.getString(
            "brand_name",
            brandName.text.toString()
        )

        nameName.text = dataPreferences.getString(
            "name_name",
            nameName.text.toString()
        )

        weightName.text = dataPreferences.getString(
            "weight_name",
            weightName.text.toString()
        )

        quantityName.text = dataPreferences.getString(
            "quantity_name",
            quantityName.text.toString()
        )

        // STORE LAST SAVED NAMES
        savedPlaceName = placeName.text.toString()
        savedCategoryName = categoryName.text.toString()
        savedItemName = itemName.text.toString()

        savedBrandName = brandName.text.toString()
        savedNameName = nameName.text.toString()
        savedWeightName = weightName.text.toString()
        savedQuantityName = quantityName.text.toString()

        // LOAD SAVED EXPANSION STATES
        originalGroup.placeExpanded =
            statePreferences.getBoolean("place_expanded", false)

        originalGroup.categoryExpanded =
            statePreferences.getBoolean("category_expanded", false)

        originalGroup.itemExpanded =
            statePreferences.getBoolean("item_expanded", false)

        updateAllExpansionVisibility()

        // EDIT / SAVE
        editButton.setOnClickListener {

            if (!editMode) {

                editMode = true

                placeChanged = false
                categoryChanged = false
                itemChanged = false

                brandChanged = false
                nameChanged = false
                weightChanged = false
                quantityChanged = false

                updateEditVisibility()
                updateNameColors()
                updateSaveButton()

            } else {

                if (
                    hasChanges() &&
                    placeName.text.isNotBlank() &&
                    categoryName.text.isNotBlank() &&
                    itemName.text.isNotBlank() &&
                    brandName.text.isNotBlank() &&
                    nameName.text.isNotBlank() &&
                    weightName.text.isNotBlank() &&
                    quantityName.text.isNotBlank()
                ) {

                    dataPreferences.edit()
                        .putString("place_name", placeName.text.toString())
                        .putString("category_name", categoryName.text.toString())
                        .putString("item_name", itemName.text.toString())
                        .putString("brand_name", brandName.text.toString())
                        .putString("name_name", nameName.text.toString())
                        .putString("weight_name", weightName.text.toString())
                        .putString("quantity_name", quantityName.text.toString())
                        .apply()

                    // UPDATE LAST SAVED NAMES
                    savedPlaceName = placeName.text.toString()
                    savedCategoryName = categoryName.text.toString()
                    savedItemName = itemName.text.toString()

                    savedBrandName = brandName.text.toString()
                    savedNameName = nameName.text.toString()
                    savedWeightName = weightName.text.toString()
                    savedQuantityName = quantityName.text.toString()
                }

                // HIDE KEYBOARD AND REMOVE CURSOR
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                currentFocus?.let { focusedView ->
                    imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                    focusedView.clearFocus()
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
                editName(placeName, "[Place]", savedPlaceName) { changed ->
                    placeChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

        // CATEGORY NAME
        categoryName.setOnClickListener {

            if (editMode) {
                editName(categoryName, "[Category]", savedCategoryName) { changed ->
                    categoryChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

        // ITEM NAME
        itemName.setOnClickListener {

            if (editMode) {
                editName(itemName, "[Item]", savedItemName) { changed ->
                    itemChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

        // BRAND NAME
        brandName.setOnClickListener {
            if (editMode) {
                editName(brandName, "[Brand]", savedBrandName) { changed ->
                    brandChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

// NAME
        nameName.setOnClickListener {
            if (editMode) {
                editName(nameName, "[Name]", savedNameName) { changed ->
                    nameChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

// WEIGHT
        weightName.setOnClickListener {
            if (editMode) {
                editName(weightName, "[Weight]", savedWeightName) { changed ->
                    weightChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

// QUANTITY
        quantityName.setOnClickListener {
            if (editMode) {
                editName(quantityName, "[Quantity]", savedQuantityName) { changed ->
                    quantityChanged = changed
                    updateNameColors()
                    updateSaveButton()
                }
            }
        }

        // INITIAL STATE
        updateEditVisibility()
        updateNameColors()
        updateSaveButton()
    }

    // SETUP GROUP
    private fun setupGroup(
        root: LinearLayout,
        idMap: SparseIntArray,
        isOriginal: Boolean
    ): GroupUi {

        val group = GroupUi(
            root = root,
            idMap = idMap,
            isOriginal = isOriginal
        )

        // PLACE DROPDOWN
        findInGroup<View>(group, R.id.a_p_expand)?.setOnClickListener {

            group.placeExpanded = !group.placeExpanded

            if (group.isOriginal) {
                statePreferences.edit()
                    .putBoolean("place_expanded", group.placeExpanded)
                    .apply()
            }

            updateGroupExpansion(group)
        }

        // CATEGORY DROPDOWN
        findInGroup<View>(group, R.id.a_c_expand)?.setOnClickListener {

            group.categoryExpanded = !group.categoryExpanded

            if (group.isOriginal) {
                statePreferences.edit()
                    .putBoolean("category_expanded", group.categoryExpanded)
                    .apply()
            }

            updateGroupExpansion(group)
        }

        // ITEM DROPDOWN
        findInGroup<View>(group, R.id.a_i_expand)?.setOnClickListener {

            group.itemExpanded = !group.itemExpanded

            if (group.isOriginal) {
                statePreferences.edit()
                    .putBoolean("item_expanded", group.itemExpanded)
                    .apply()
            }

            updateGroupExpansion(group)
        }

        // ADD GROUP
        if (isOriginal) {

            findInGroup<View>(group, R.id.c_p_add)?.setOnClickListener {
                addGroup()
            }
        }

        return group
    }

    // ADD GROUP
    private fun addGroup() {

        // Inflate a fresh copy without attaching it.
        val tempRoot = layoutInflater.inflate(
            R.layout.complete,
            null,
            false
        ) as ViewGroup

        val newGroup = tempRoot.findViewById<LinearLayout>(
            R.id.group
        )

        // Detach the group from its actual parent.
        (newGroup.parent as? ViewGroup)?.removeView(newGroup)

        // Remove edit_place_bar from duplicated groups.
        val editPlaceBar = newGroup.findViewById<View>(
            R.id.edit_place_bar
        )

        (editPlaceBar.parent as? ViewGroup)?.removeView(editPlaceBar)

        // Generate unique IDs for every remaining view.
        val idMap = SparseIntArray()

        regenerateViewIds(newGroup, idMap)

        // Setup duplicated group.
        val newGroupUi = setupGroup(
            root = newGroup,
            idMap = idMap,
            isOriginal = false
        )

        // Duplicates start collapsed.
        newGroupUi.placeExpanded = false
        newGroupUi.categoryExpanded = false
        newGroupUi.itemExpanded = false

        updateDropdownSymbols(newGroupUi)

        updateGroupExpansion(newGroupUi)

        // Add duplicate with 20dp spacing.
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.topMargin = dp(20)

        scroll.addView(newGroup, params)

        groups.add(newGroupUi)

        updateEditVisibility()
    }

    // GENERATE UNIQUE IDS
    private fun regenerateViewIds(
        view: View,
        idMap: SparseIntArray
    ) {

        val originalId = view.id

        if (originalId != View.NO_ID) {

            val newId = View.generateViewId()

            idMap.put(originalId, newId)

            view.id = newId
        }

        if (view is ViewGroup) {

            for (i in 0 until view.childCount) {

                regenerateViewIds(
                    view.getChildAt(i),
                    idMap
                )
            }
        }
    }

    // FIND VIEW USING ORIGINAL XML ID
    private fun <T : View> findInGroup(
        group: GroupUi,
        originalId: Int
    ): T? {

        val actualId = group.idMap.get(
            originalId,
            originalId
        )

        return group.root.findViewById<T>(actualId)
    }

    // UPDATE DROPDOWN SYMBOLS
    private fun updateDropdownSymbols(group: GroupUi) {

        val symbols = listOf(
            R.id.a_p_expand to group.placeExpanded,
            R.id.a_c_expand to group.categoryExpanded,
            R.id.a_i_expand to group.itemExpanded
        )

        for ((id, expanded) in symbols) {

            findInGroup<TextView>(group, id)?.apply {

                this.text = if (expanded) "v" else ">"

                this.textSize = if (id == R.id.a_i_expand) 20f else 20f

                this.gravity = android.view.Gravity.CENTER

                this.includeFontPadding = false
            }
        }
    }

    // UPDATE ONE GROUP'S EXPANSION
    private fun updateGroupExpansion(group: GroupUi) {

        findInGroup<View>(
            group,
            R.id.category_section
        )?.visibility =
            if (group.placeExpanded) View.VISIBLE else View.GONE

        findInGroup<View>(
            group,
            R.id.item_section
        )?.visibility =
            if (group.placeExpanded && group.categoryExpanded) {
                View.VISIBLE
            } else {
                View.GONE
            }

        findInGroup<View>(
            group,
            R.id.item_brand_bar
        )?.visibility =
            if (
                group.placeExpanded &&
                group.categoryExpanded &&
                group.itemExpanded
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        updateDropdownSymbols(group)
    }

    // UPDATE ALL GROUPS' EXPANSION
    private fun updateAllExpansionVisibility() {

        for (group in groups) {
            updateGroupExpansion(group)
        }
    }

    // UPDATE EDIT VISIBILITY
    private fun updateEditVisibility() {

        val visibility =
            if (editMode) View.VISIBLE else View.GONE

        for (group in groups) {

            // PLACE CONTROLS
            findInGroup<View>(group, R.id.c_p_add)?.visibility = visibility
            findInGroup<View>(group, R.id.c_p_sort)?.visibility = visibility
            findInGroup<View>(group, R.id.c_p_add_list)?.visibility = visibility

            // CATEGORY CONTROLS
            findInGroup<View>(group, R.id.c_c_add)?.visibility = visibility
            findInGroup<View>(group, R.id.c_c_sort)?.visibility = visibility
            findInGroup<View>(group, R.id.c_c_add_list)?.visibility = visibility

            // ITEM CONTROLS
            findInGroup<View>(group, R.id.c_i_add)?.visibility = visibility
            findInGroup<View>(group, R.id.c_i_sort)?.visibility = visibility
            findInGroup<View>(group, R.id.c_i_add_list)?.visibility = visibility
        }
    }

    // EDIT NAME INLINE
    private fun editName(
        textView: TextView,
        hint: String,
        savedName: String,
        onChanged: (Boolean) -> Unit
    ) {
        val parent = textView.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(textView)

        val params = textView.layoutParams

        textView.hint = hint

        val input = EditText(this).apply {

            id = textView.id

            setText(textView.text)
            setSelection(text.length)

            this.hint = hint

            setTextColor(white)
            setHintTextColor(muted)

            textSize = textView.textSize / resources.displayMetrics.scaledDensity

            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

            background = textView.background
            gravity = textView.gravity

            setSingleLine(false)
            setHorizontallyScrolling(false)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = textView.gravity
        }

        parent.removeViewAt(index)
        parent.addView(input, index, params)

        input.requestFocus()

        // CHECK CHANGES WHILE TYPING
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
                val typedName = s.toString().trim()

                textView.text = typedName

                val changed = typedName.isNotEmpty() && typedName != savedName

                onChanged(changed)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {

                val finalName = input.text.toString().trim()

                textView.text = finalName

                parent.removeView(input)
                parent.addView(textView, index, params)

                onChanged(finalName.isNotEmpty() && finalName != savedName)
            }
        }

        input.setOnEditorActionListener { _, _, _ ->
            input.clearFocus()
            true
        }
    }


    // UPDATE NAME COLORS
    private fun updateNameColors() {

        if (!editMode) {

            placeName.setTextColor(white)
            categoryName.setTextColor(white)
            itemName.setTextColor(white)

            brandName.setTextColor(muted)
            nameName.setTextColor(muted)
            weightName.setTextColor(muted)
            quantityName.setTextColor(muted)

            return
        }

        // PLACE / CATEGORY / ITEM
        placeName.setTextColor(
            if (placeChanged) white else muted
        )

        categoryName.setTextColor(
            if (categoryChanged) white else muted
        )

        itemName.setTextColor(
            if (itemChanged) white else muted
        )

        // BRAND / NAME / WEIGHT / QUANTITY
        brandName.setTextColor(
            if (brandChanged) muted else white
        )

        nameName.setTextColor(
            if (nameChanged) muted else white
        )

        weightName.setTextColor(
            if (weightChanged) muted else white
        )

        quantityName.setTextColor(
            if (quantityChanged) muted else white
        )
    }

    // CHECK CHANGES
    private fun hasChanges(): Boolean {

        return placeChanged ||
                categoryChanged ||
                itemChanged ||
                brandChanged ||
                nameChanged ||
                weightChanged ||
                quantityChanged
    }

    // UPDATE SAVE BUTTON
    private fun updateSaveButton() {

        val allNamesFilled =
            placeName.text.isNotBlank() &&
                    categoryName.text.isNotBlank() &&
                    itemName.text.isNotBlank() &&
                    brandName.text.isNotBlank() &&
                    nameName.text.isNotBlank() &&
                    weightName.text.isNotBlank() &&
                    quantityName.text.isNotBlank()

        val canSave =
            editMode &&
                    hasChanges() &&
                    allNamesFilled

        editButton.text =
            if (editMode) "Save" else "Edit"

        editButton.isEnabled =
            !editMode || allNamesFilled

        editButton.backgroundTintList =
            ColorStateList.valueOf(
                if (canSave) orange else gray
            )
    }

    // DP CONVERSION
    private fun dp(value: Int): Int {

        return (value * resources.displayMetrics.density).toInt()
    }
}
