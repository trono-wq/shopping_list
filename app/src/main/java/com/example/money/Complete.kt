
package com.example.shoppinglist

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class Complete : AppCompatActivity() {

    private var editMode = false

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

    private data class GroupUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,
        val isOriginal: Boolean,

        val placeName: TextView,
        val categoryName: TextView,
        val itemName: TextView,

        val brandName: TextView,
        val nameName: TextView,
        val weightName: TextView,
        val quantityName: TextView,

        var placeExpanded: Boolean = false,
        var categoryExpanded: Boolean = false,
        var itemExpanded: Boolean = false,

        var placeEditExpanded: Boolean = false,
        var categoryEditExpanded: Boolean = false,
        var itemEditExpanded: Boolean = false,

        var savedPlaceName: String = "",
        var savedCategoryName: String = "",
        var savedItemName: String = "",

        var savedBrandName: String = "",
        var savedNameName: String = "",
        var savedWeightName: String = "",
        var savedQuantityName: String = "",

        var placeChanged: Boolean = false,
        var categoryChanged: Boolean = false,
        var itemChanged: Boolean = false,

        var brandChanged: Boolean = false,
        var nameChanged: Boolean = false,
        var weightChanged: Boolean = false,
        var quantityChanged: Boolean = false
    )

    private val groups = mutableListOf<GroupUi>()

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

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

        val originalGroup = setupGroup(
            root = originalRoot,
            idMap = SparseIntArray(),
            isOriginal = true
        )

        groups.add(originalGroup)

        // LOAD ORIGINAL GROUP DATA
        loadGroupData(originalGroup, 0)

        // LOAD ORIGINAL EXPANSION STATE
        originalGroup.placeExpanded =
            statePreferences.getBoolean("place_expanded", false)

        originalGroup.categoryExpanded =
            statePreferences.getBoolean("category_expanded", false)

        originalGroup.itemExpanded =
            statePreferences.getBoolean("item_expanded", false)

        // RESTORE DUPLICATED PLACES
        val savedGroupCount = dataPreferences.getInt(
            "group_count",
            1
        )

        for (index in 1 until savedGroupCount) {
            addGroup(
                restoreIndex = index,
                persist = false
            )
        }

        updateAllExpansionVisibility()

        // EDIT / SAVE BUTTON
        editButton.setOnClickListener {

            if (!editMode) {

                editMode = true

                for (group in groups) {
                    resetChangeFlags(group)

                    group.placeEditExpanded = false
                    group.categoryEditExpanded = false
                    group.itemEditExpanded = false
                }

                updateEditVisibility()
                updateAllNameColors()
                updateSaveButton()

            } else {

                if (hasChanges() && allRequiredNamesFilled()) {
                    persistAllGroups()
                    updateSavedNames()
                }

                // HIDE KEYBOARD
                val imm = getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

                currentFocus?.let { focusedView ->
                    imm.hideSoftInputFromWindow(
                        focusedView.windowToken,
                        0
                    )

                    focusedView.clearFocus()
                }

                editMode = false

                for (group in groups) {
                    resetChangeFlags(group)

                    group.placeEditExpanded = false
                    group.categoryEditExpanded = false
                    group.itemEditExpanded = false
                }

                updateEditVisibility()
                updateAllNameColors()
                updateSaveButton()
            }
        }

        // INITIAL STATE
        updateEditVisibility()
        updateAllNameColors()
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
            isOriginal = isOriginal,

            placeName = findInGroup(root, idMap, R.id.a_p_name)!!,
            categoryName = findInGroup(root, idMap, R.id.a_c_name)!!,
            itemName = findInGroup(root, idMap, R.id.a_i_name)!!,

            brandName = findInGroup(root, idMap, R.id.a_b_brand)!!,
            nameName = findInGroup(root, idMap, R.id.a_b_name)!!,
            weightName = findInGroup(root, idMap, R.id.a_b_weight)!!,
            quantityName = findInGroup(root, idMap, R.id.a_b_quantity)!!
        )

        // PLACE DROPDOWN
        findInGroup<View>(group, R.id.a_p_expand)?.setOnClickListener {

            group.placeExpanded = !group.placeExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        // CATEGORY DROPDOWN
        findInGroup<View>(group, R.id.a_c_expand)?.setOnClickListener {

            group.categoryExpanded = !group.categoryExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        // ITEM DROPDOWN
        findInGroup<View>(group, R.id.a_i_expand)?.setOnClickListener {

            group.itemExpanded = !group.itemExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        // PLACE EDIT BUTTON
        findInGroup<View>(group, R.id.c_p_edit)?.setOnClickListener {

            if (editMode) {
                group.placeEditExpanded =
                    !group.placeEditExpanded

                updateEditVisibility()
            }
        }

        // CATEGORY EDIT BUTTON
        findInGroup<View>(group, R.id.c_c_edit)?.setOnClickListener {

            if (editMode) {
                group.categoryEditExpanded =
                    !group.categoryEditExpanded

                updateEditVisibility()
            }
        }

        // ITEM EDIT BUTTON
        findInGroup<View>(group, R.id.c_i_edit)?.setOnClickListener {

            if (editMode) {
                group.itemEditExpanded =
                    !group.itemEditExpanded

                updateEditVisibility()
            }
        }

        // PLACE NAME
        group.placeName.setOnClickListener {
            if (editMode) {
                editName(
                    group.placeName,
                    "[Place]",
                    group.savedPlaceName
                ) { changed ->
                    group.placeChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // CATEGORY NAME
        group.categoryName.setOnClickListener {
            if (editMode) {
                editName(
                    group.categoryName,
                    "[Category]",
                    group.savedCategoryName
                ) { changed ->
                    group.categoryChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // ITEM NAME
        group.itemName.setOnClickListener {
            if (editMode) {
                editName(
                    group.itemName,
                    "[Item]",
                    group.savedItemName
                ) { changed ->
                    group.itemChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // BRAND HEADER
        group.brandName.setOnClickListener {
            if (editMode) {
                editName(
                    group.brandName,
                    "[Brand]",
                    group.savedBrandName
                ) { changed ->
                    group.brandChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // NAME HEADER
        group.nameName.setOnClickListener {
            if (editMode) {
                editName(
                    group.nameName,
                    "[Name]",
                    group.savedNameName
                ) { changed ->
                    group.nameChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // WEIGHT HEADER
        group.weightName.setOnClickListener {
            if (editMode) {
                editName(
                    group.weightName,
                    "[Weight]",
                    group.savedWeightName
                ) { changed ->
                    group.weightChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // QUANTITY HEADER
        group.quantityName.setOnClickListener {
            if (editMode) {
                editName(
                    group.quantityName,
                    "[Quantity]",
                    group.savedQuantityName
                ) { changed ->
                    group.quantityChanged = changed
                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // ADD PLACE
        if (isOriginal) {
            findInGroup<View>(group, R.id.c_p_add)?.setOnClickListener {
                addGroup()
            }
        }

        return group
    }

    // ADD DUPLICATED PLACE
    private fun addGroup(
        restoreIndex: Int? = null,
        persist: Boolean = true
    ) {

        val tempRoot = layoutInflater.inflate(
            R.layout.complete,
            null,
            false
        ) as ViewGroup

        val newGroup = tempRoot.findViewById<LinearLayout>(
            R.id.group
        )

        (newGroup.parent as? ViewGroup)?.removeView(newGroup)

        // REMOVE DUPLICATE PLACE EDIT BAR
        val editPlaceBar = newGroup.findViewById<View>(
            R.id.edit_place_bar
        )

        (editPlaceBar.parent as? ViewGroup)?.removeView(editPlaceBar)

        // GENERATE UNIQUE IDS
        val idMap = SparseIntArray()

        regenerateViewIds(newGroup, idMap)

        // SETUP DUPLICATE
        val newGroupUi = setupGroup(
            root = newGroup,
            idMap = idMap,
            isOriginal = false
        )

        groups.add(newGroupUi)

        // LOAD SAVED DATA OR DEFAULT DATA
        val index = restoreIndex ?: groups.lastIndex

        loadGroupData(newGroupUi, index)

        if (restoreIndex == null) {
            newGroupUi.placeExpanded = false
            newGroupUi.categoryExpanded = false
            newGroupUi.itemExpanded = false
        }

        updateDropdownSymbols(newGroupUi)
        updateGroupExpansion(newGroupUi)

        // ADD WITH 20DP SPACING
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.topMargin = dp(20)

        scroll.addView(newGroup, params)

        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()

        if (persist) {
            persistAllGroups()
        }
    }

    // LOAD GROUP DATA
    private fun loadGroupData(
        group: GroupUi,
        index: Int
    ) {

        fun key(name: String): String {
            return if (index == 0) {
                name
            } else {
                "group_${index}_$name"
            }
        }

        group.placeName.text = dataPreferences.getString(
            key("place_name"),
            group.placeName.text.toString()
        )

        group.categoryName.text = dataPreferences.getString(
            key("category_name"),
            group.categoryName.text.toString()
        )

        group.itemName.text = dataPreferences.getString(
            key("item_name"),
            group.itemName.text.toString()
        )

        group.brandName.text = dataPreferences.getString(
            key("brand_name"),
            group.brandName.text.toString()
        )

        group.nameName.text = dataPreferences.getString(
            key("name_name"),
            group.nameName.text.toString()
        )

        group.weightName.text = dataPreferences.getString(
            key("weight_name"),
            group.weightName.text.toString()
        )

        group.quantityName.text = dataPreferences.getString(
            key("quantity_name"),
            group.quantityName.text.toString()
        )

        group.savedPlaceName = group.placeName.text.toString()
        group.savedCategoryName = group.categoryName.text.toString()
        group.savedItemName = group.itemName.text.toString()

        group.savedBrandName = group.brandName.text.toString()
        group.savedNameName = group.nameName.text.toString()
        group.savedWeightName = group.weightName.text.toString()
        group.savedQuantityName = group.quantityName.text.toString()

        if (index > 0) {

            group.placeExpanded = statePreferences.getBoolean(
                "group_${index}_place_expanded",
                false
            )

            group.categoryExpanded = statePreferences.getBoolean(
                "group_${index}_category_expanded",
                false
            )

            group.itemExpanded = statePreferences.getBoolean(
                "group_${index}_item_expanded",
                false
            )
        }
    }

    // SAVE ALL GROUPS
    private fun persistAllGroups() {

        val dataEditor = dataPreferences.edit()
        val stateEditor = statePreferences.edit()

        dataEditor.putInt("group_count", groups.size)

        for ((index, group) in groups.withIndex()) {

            fun key(name: String): String {
                return if (index == 0) {
                    name
                } else {
                    "group_${index}_$name"
                }
            }

            dataEditor.putString(
                key("place_name"),
                group.placeName.text.toString()
            )

            dataEditor.putString(
                key("category_name"),
                group.categoryName.text.toString()
            )

            dataEditor.putString(
                key("item_name"),
                group.itemName.text.toString()
            )

            dataEditor.putString(
                key("brand_name"),
                group.brandName.text.toString()
            )

            dataEditor.putString(
                key("name_name"),
                group.nameName.text.toString()
            )

            dataEditor.putString(
                key("weight_name"),
                group.weightName.text.toString()
            )

            dataEditor.putString(
                key("quantity_name"),
                group.quantityName.text.toString()
            )

            stateEditor.putBoolean(
                if (index == 0) "place_expanded"
                else "group_${index}_place_expanded",
                group.placeExpanded
            )

            stateEditor.putBoolean(
                if (index == 0) "category_expanded"
                else "group_${index}_category_expanded",
                group.categoryExpanded
            )

            stateEditor.putBoolean(
                if (index == 0) "item_expanded"
                else "group_${index}_item_expanded",
                group.itemExpanded
            )
        }

        dataEditor.apply()
        stateEditor.apply()
    }

    // UPDATE SAVED NAMES
    private fun updateSavedNames() {

        for (group in groups) {

            group.savedPlaceName = group.placeName.text.toString()
            group.savedCategoryName = group.categoryName.text.toString()
            group.savedItemName = group.itemName.text.toString()

            group.savedBrandName = group.brandName.text.toString()
            group.savedNameName = group.nameName.text.toString()
            group.savedWeightName = group.weightName.text.toString()
            group.savedQuantityName = group.quantityName.text.toString()
        }
    }

    // SAVE EXPANSION STATE
    private fun saveExpansionState(group: GroupUi) {

        val index = groups.indexOf(group)

        val prefix = if (index == 0) {
            ""
        } else {
            "group_${index}_"
        }

        statePreferences.edit()
            .putBoolean(
                "${prefix}place_expanded",
                group.placeExpanded
            )
            .putBoolean(
                "${prefix}category_expanded",
                group.categoryExpanded
            )
            .putBoolean(
                "${prefix}item_expanded",
                group.itemExpanded
            )
            .apply()
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

    // FIND VIEW
    private fun <T : View> findInGroup(
        group: GroupUi,
        originalId: Int
    ): T? {

        val actualId = group.idMap.get(
            originalId,
            originalId
        )

        return group.root.findViewById(actualId)
    }

    // FIND VIEW BEFORE GROUP SETUP
    private fun <T : View> findInGroup(
        root: View,
        idMap: SparseIntArray,
        originalId: Int
    ): T? {

        val actualId = idMap.get(
            originalId,
            originalId
        )

        return root.findViewById(actualId)
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

                text = if (expanded) "v" else ">"

                textSize = 20f
                gravity = Gravity.CENTER
                includeFontPadding = false
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

    // UPDATE ALL EXPANSION STATES
    private fun updateAllExpansionVisibility() {

        for (group in groups) {
            updateGroupExpansion(group)
        }
    }

    // UPDATE EDIT VISIBILITY
    private fun updateEditVisibility() {

        for (group in groups) {

            // MAIN EDIT MODE CONTROLS
            val mainVisibility =
                if (editMode) View.VISIBLE else View.GONE

            findInGroup<View>(
                group,
                R.id.c_p_sort
            )?.visibility = mainVisibility

            findInGroup<View>(
                group,
                R.id.c_p_edit
            )?.visibility = mainVisibility

            findInGroup<View>(
                group,
                R.id.c_c_edit
            )?.visibility = mainVisibility

            findInGroup<View>(
                group,
                R.id.c_i_edit
            )?.visibility = mainVisibility

            // BRAND CONTROLS APPEAR AUTOMATICALLY IN EDIT MODE
            findInGroup<View>(
                group,
                R.id.c_i_b_add
            )?.visibility = mainVisibility

            findInGroup<View>(
                group,
                R.id.c_i_b_remove
            )?.visibility = mainVisibility

            // PLACE CONTROLS
            val placeVisibility =
                if (editMode && group.placeEditExpanded) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<View>(
                group,
                R.id.c_p_add
            )?.visibility = placeVisibility

            findInGroup<View>(
                group,
                R.id.c_p_remove
            )?.visibility = placeVisibility

            findInGroup<View>(
                group,
                R.id.c_p_add_list
            )?.visibility = placeVisibility

            // CATEGORY CONTROLS
            val categoryVisibility =
                if (editMode && group.categoryEditExpanded) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<View>(
                group,
                R.id.c_c_add
            )?.visibility = categoryVisibility

            findInGroup<View>(
                group,
                R.id.c_c_remove
            )?.visibility = categoryVisibility

            findInGroup<View>(
                group,
                R.id.c_c_add_list
            )?.visibility = categoryVisibility

            // ITEM CONTROLS
            val itemVisibility =
                if (editMode && group.itemEditExpanded) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<View>(
                group,
                R.id.c_i_add
            )?.visibility = itemVisibility

            findInGroup<View>(
                group,
                R.id.c_i_remove
            )?.visibility = itemVisibility

            findInGroup<View>(
                group,
                R.id.c_i_add_list
            )?.visibility = itemVisibility
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

        val input = EditText(this).apply {

            id = textView.id

            setText(textView.text)
            setSelection(this.text.length)

            this.hint = hint

            setTextColor(white)
            setHintTextColor(muted)

            textSize =
                textView.textSize /
                        resources.displayMetrics.scaledDensity

            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE

            background = textView.background
            gravity = textView.gravity

            setSingleLine(false)
            setHorizontallyScrolling(false)
        }

        parent.removeViewAt(index)
        parent.addView(input, index, params)

        input.requestFocus()

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

                onChanged(
                    typedName.isNotEmpty() &&
                            typedName != savedName
                )
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        input.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus) {

                val finalName = input.text.toString().trim()

                textView.text = finalName

                parent.removeView(input)
                parent.addView(textView, index, params)

                onChanged(
                    finalName.isNotEmpty() &&
                            finalName != savedName
                )
            }
        }

        input.setOnEditorActionListener { _, _, _ ->
            input.clearFocus()
            true
        }
    }

    // RESET CHANGE FLAGS
    private fun resetChangeFlags(group: GroupUi) {

        group.placeChanged = false
        group.categoryChanged = false
        group.itemChanged = false

        group.brandChanged = false
        group.nameChanged = false
        group.weightChanged = false
        group.quantityChanged = false
    }

    // UPDATE NAME COLORS
    private fun updateAllNameColors() {

        for (group in groups) {
            updateNameColors(group)
        }
    }

    private fun updateNameColors(group: GroupUi) {

        if (!editMode) {

            group.placeName.setTextColor(white)
            group.categoryName.setTextColor(white)
            group.itemName.setTextColor(white)

            group.brandName.setTextColor(muted)
            group.nameName.setTextColor(muted)
            group.weightName.setTextColor(muted)
            group.quantityName.setTextColor(muted)

            return
        }

        // PLACE / CATEGORY / ITEM
        group.placeName.setTextColor(
            if (group.placeChanged) white else muted
        )

        group.categoryName.setTextColor(
            if (group.categoryChanged) white else muted
        )

        group.itemName.setTextColor(
            if (group.itemChanged) white else muted
        )

        // BRAND / NAME / WEIGHT / QUANTITY
        group.brandName.setTextColor(
            if (group.brandChanged) muted else white
        )

        group.nameName.setTextColor(
            if (group.nameChanged) muted else white
        )

        group.weightName.setTextColor(
            if (group.weightChanged) muted else white
        )

        group.quantityName.setTextColor(
            if (group.quantityChanged) muted else white
        )
    }

    // CHECK CHANGES
    private fun hasChanges(): Boolean {

        return groups.any { group ->

            group.placeChanged ||
                    group.categoryChanged ||
                    group.itemChanged ||
                    group.brandChanged ||
                    group.nameChanged ||
                    group.weightChanged ||
                    group.quantityChanged
        }
    }

    // REQUIRED NAMES
    private fun allRequiredNamesFilled(): Boolean {

        return groups.all { group ->

            group.placeName.text.isNotBlank() &&
                    group.categoryName.text.isNotBlank() &&
                    group.itemName.text.isNotBlank()
        }
    }

    // UPDATE SAVE BUTTON
    private fun updateSaveButton() {

        val allNamesFilled = allRequiredNamesFilled()

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
}