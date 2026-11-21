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
import java.util.Locale

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

    private data class CategoryUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,

        val categoryName: TextView,
        val itemName: TextView,

        val brandName: TextView,
        val nameName: TextView,
        val weightName: TextView,
        val quantityName: TextView,

        var categoryExpanded: Boolean = false,
        var itemExpanded: Boolean = false,

        var categoryEditExpanded: Boolean = false,
        var itemEditExpanded: Boolean = false,

        var savedCategoryName: String = "",
        var savedItemName: String = "",

        var savedBrandName: String = "",
        var savedNameName: String = "",
        var savedWeightName: String = "",
        var savedQuantityName: String = "",

        var categoryChanged: Boolean = false,
        var itemChanged: Boolean = false,

        var brandChanged: Boolean = false,
        var nameChanged: Boolean = false,
        var weightChanged: Boolean = false,
        var quantityChanged: Boolean = false
    )

    private data class GroupUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,
        val isOriginal: Boolean,

        val placeName: TextView,
        val categoryContainer: LinearLayout,

        var placeExpanded: Boolean = false,
        var placeEditExpanded: Boolean = false,

        var savedPlaceName: String = "",
        var placeChanged: Boolean = false,

        val categories: MutableList<CategoryUi> = mutableListOf()
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

        for ((index, category) in originalGroup.categories.withIndex()) {

            category.categoryExpanded = statePreferences.getBoolean(
                if (index == 0) "category_expanded"
                else "category_${index}_expanded",
                false
            )

            category.itemExpanded = statePreferences.getBoolean(
                if (index == 0) "item_expanded"
                else "category_${index}_item_expanded",
                false
            )
        }

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

                    for (category in group.categories) {
                        category.categoryEditExpanded = false
                        category.itemEditExpanded = false
                    }
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

                    for (category in group.categories) {
                        category.categoryEditExpanded = false
                        category.itemEditExpanded = false
                    }
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
            categoryContainer = root
        )

        // PLACEHOLDER
        if (group.placeName.text.toString() == "[Place]") {
            group.placeName.text = ""
        }

        group.placeName.hint = "[Place]"

        // FIND CATEGORY AND ITEM SECTIONS
        val categorySection = findInGroup<LinearLayout>(
            root,
            idMap,
            R.id.category_section
        )!!

        val itemSection = findInGroup<LinearLayout>(
            root,
            idMap,
            R.id.item_section
        )!!

        // DETACH BOTH SECTIONS
        (categorySection.parent as? ViewGroup)?.removeView(categorySection)
        (itemSection.parent as? ViewGroup)?.removeView(itemSection)

        // CREATE THE FIRST CATEGORY CONTAINER
        val categoryRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        categoryRoot.addView(categorySection)
        categoryRoot.addView(itemSection)

        // ADD CATEGORY CONTAINER TO PLACE
        root.addView(categoryRoot)

        // SET UP FIRST CATEGORY
        // REUSE THE PLACE ID MAP
        val firstCategory = setupCategory(
            group = group,
            root = categoryRoot,
            idMap = idMap
        )

        group.categories.add(firstCategory)

        // PLACE DROPDOWN
        findInGroup<View>(
            group,
            R.id.a_p_expand
        )?.setOnClickListener {

            group.placeExpanded = !group.placeExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        // PLACE EDIT BUTTON
        findInGroup<View>(
            group,
            R.id.c_p_edit
        )?.setOnClickListener {

            if (editMode) {
                group.placeEditExpanded = !group.placeEditExpanded

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

        // ADD PLACE
        findInGroup<View>(
            group,
            R.id.c_p_add
        )?.setOnClickListener {
            addGroup(afterGroup = group)
        }

        return group
    }

    // SETUP CATEGORY
    private fun setupCategory(
        group: GroupUi,
        root: LinearLayout,
        idMap: SparseIntArray
    ): CategoryUi {

        val category = CategoryUi(
            root = root,
            idMap = idMap,

            categoryName = findInCategory(root, idMap, R.id.a_c_name)!!,
            itemName = findInCategory(root, idMap, R.id.a_i_name)!!,

            brandName = findInCategory(root, idMap, R.id.a_b_brand)!!,
            nameName = findInCategory(root, idMap, R.id.a_b_name)!!,
            weightName = findInCategory(root, idMap, R.id.a_b_weight)!!,
            quantityName = findInCategory(root, idMap, R.id.a_b_quantity)!!
        )

        // PLACEHOLDERS
        val placeholders = listOf(
            category.categoryName to "[Category]",
            category.itemName to "[Item]",
            category.brandName to "[Brand]",
            category.nameName to "[Name]",
            category.weightName to "[Weight]",
            category.quantityName to "[Quantity]"
        )

        for ((textView, placeholder) in placeholders) {

            if (textView.text.toString() == placeholder) {
                textView.text = ""
            }

            textView.hint = placeholder
        }

        // CATEGORY DROPDOWN
        findInCategory<View>(category, R.id.a_c_expand)?.setOnClickListener {

            category.categoryExpanded = !category.categoryExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        // ITEM DROPDOWN
        findInCategory<View>(category, R.id.a_i_expand)?.setOnClickListener {

            category.itemExpanded = !category.itemExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        // CATEGORY EDIT BUTTON
        findInCategory<View>(category, R.id.c_c_edit)?.setOnClickListener {

            if (editMode) {
                category.categoryEditExpanded =
                    !category.categoryEditExpanded

                updateEditVisibility()
            }
        }

        // ITEM EDIT BUTTON
        findInCategory<View>(category, R.id.c_i_edit)?.setOnClickListener {

            if (editMode) {
                category.itemEditExpanded =
                    !category.itemEditExpanded

                updateEditVisibility()
            }
        }

        // CATEGORY NAME
        category.categoryName.setOnClickListener {

            if (editMode) {
                editName(
                    category.categoryName,
                    "[Category]",
                    category.savedCategoryName
                ) { changed ->

                    category.categoryChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // ITEM NAME
        category.itemName.setOnClickListener {

            if (editMode) {
                editName(
                    category.itemName,
                    "[Item]",
                    category.savedItemName
                ) { changed ->

                    category.itemChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // BRAND HEADER
        category.brandName.setOnClickListener {

            if (editMode) {
                editName(
                    category.brandName,
                    "[Brand]",
                    category.savedBrandName
                ) { changed ->

                    category.brandChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // NAME HEADER
        category.nameName.setOnClickListener {

            if (editMode) {
                editName(
                    category.nameName,
                    "[Name]",
                    category.savedNameName
                ) { changed ->

                    category.nameChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // WEIGHT HEADER
        category.weightName.setOnClickListener {

            if (editMode) {
                editName(
                    category.weightName,
                    "[Weight]",
                    category.savedWeightName
                ) { changed ->

                    category.weightChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // QUANTITY HEADER
        category.quantityName.setOnClickListener {

            if (editMode) {
                editName(
                    category.quantityName,
                    "[Quantity]",
                    category.savedQuantityName
                ) { changed ->

                    category.quantityChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
            }
        }

        // ADD CATEGORY
        findInCategory<View>(category, R.id.c_c_add)?.setOnClickListener {
            addCategory(group, category)
        }

        return category
    }

    // ADD DUPLICATED CATEGORY
    private fun addCategory(
        group: GroupUi,
        afterCategory: CategoryUi
    ) {

        // INFLATE A FRESH LAYOUT
        val tempRoot = layoutInflater.inflate(
            R.layout.complete,
            null,
            false
        ) as ViewGroup

        val tempGroup = tempRoot.findViewById<LinearLayout>(
            R.id.group
        )

        val categorySection = tempGroup.findViewById<LinearLayout>(
            R.id.category_section
        )

        val itemSection = tempGroup.findViewById<LinearLayout>(
            R.id.item_section
        )

        // DETACH BOTH SECTIONS
        (categorySection.parent as? ViewGroup)?.removeView(categorySection)
        (itemSection.parent as? ViewGroup)?.removeView(itemSection)

        // CREATE CATEGORY ROOT
        val categoryRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        categoryRoot.addView(categorySection)
        categoryRoot.addView(itemSection)

        // REMOVE THE EXTRA CATEGORY SORT BAR
        categoryRoot.findViewById<View>(
            R.id.edit_category_bar
        )?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        // GENERATE UNIQUE IDS
        val idMap = SparseIntArray()
        regenerateViewIds(categoryRoot, idMap)

        // SETUP NEW CATEGORY
        val newCategory = setupCategory(
            group = group,
            root = categoryRoot,
            idMap = idMap
        )

        // CLEAR NEW CATEGORY VALUES
        newCategory.categoryName.text = ""
        newCategory.itemName.text = ""

        newCategory.brandName.text = ""
        newCategory.nameName.text = ""
        newCategory.weightName.text = ""
        newCategory.quantityName.text = ""

        newCategory.savedCategoryName = ""
        newCategory.savedItemName = ""

        newCategory.savedBrandName = ""
        newCategory.savedNameName = ""
        newCategory.savedWeightName = ""
        newCategory.savedQuantityName = ""

        // INSERT AFTER THE SELECTED CATEGORY
        val categoryIndex = group.categories.indexOf(afterCategory) + 1
        val viewIndex = group.categoryContainer.indexOfChild(afterCategory.root) + 1

        group.categories.add(categoryIndex, newCategory)

        group.categoryContainer.addView(
            categoryRoot,
            viewIndex
        )

        updateAllExpansionVisibility()
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
    }

    // ADD DUPLICATED PLACE
    private fun addGroup(
        restoreIndex: Int? = null,
        persist: Boolean = true,
        afterGroup: GroupUi? = null
    ) {

        val tempRoot = layoutInflater.inflate(
            R.layout.complete,
            null,
            false
        ) as ViewGroup

        val newGroup = tempRoot.findViewById<LinearLayout>(
            R.id.group
        )

        // REMOVE DUPLICATE PLACE EDIT BAR
        newGroup.findViewById<View>(
            R.id.edit_place_bar
        )?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        // GENERATE UNIQUE IDS
        val idMap = SparseIntArray()
        regenerateViewIds(newGroup, idMap)

        // SETUP DUPLICATE PLACE
        val newGroupUi = setupGroup(
            root = newGroup,
            idMap = idMap,
            isOriginal = false
        )

        // INSERT AFTER SELECTED PLACE
        val insertIndex = if (afterGroup != null) {
            groups.indexOf(afterGroup) + 1
        } else {
            groups.size
        }

        groups.add(insertIndex, newGroupUi)

        // LOAD SAVED DATA OR INITIALIZE NEW PLACE
        if (restoreIndex != null) {

            loadGroupData(newGroupUi, restoreIndex)

        } else {

            newGroupUi.placeName.text = ""
            newGroupUi.savedPlaceName = ""

            for (category in newGroupUi.categories) {

                category.categoryName.text = ""
                category.itemName.text = ""

                category.brandName.text = ""
                category.nameName.text = ""
                category.weightName.text = ""
                category.quantityName.text = ""

                category.savedCategoryName = ""
                category.savedItemName = ""

                category.savedBrandName = ""
                category.savedNameName = ""
                category.savedWeightName = ""
                category.savedQuantityName = ""

                category.categoryExpanded = false
                category.itemExpanded = false
            }

            newGroupUi.placeExpanded = false
        }

        // ADD WITH 20DP SPACING
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.topMargin = dp(20)

        // REMOVE FROM ORIGINAL INFLATED PARENT
        (newGroup.parent as? android.view.ViewGroup)?.removeView(newGroup)

        // ADD TO SCROLL CONTAINER
        scroll.addView(newGroup, insertIndex, params)

        updateDropdownSymbols(newGroupUi)
        updateGroupExpansion(newGroupUi)
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
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

        // LOAD PLACE
        group.placeName.text = dataPreferences.getString(
            key("place_name"),
            ""
        ) ?: ""

        group.savedPlaceName = group.placeName.text.toString()

        // LOAD CATEGORY COUNT
        val categoryCount = dataPreferences.getInt(
            key("category_count"),
            1
        )

        // CREATE ADDITIONAL CATEGORIES
        while (group.categories.size < categoryCount) {

            addCategory(
                group,
                group.categories.last()
            )
        }

        // LOAD EACH CATEGORY
        for ((categoryIndex, category) in group.categories.withIndex()) {

            fun categoryKey(name: String): String {
                return if (categoryIndex == 0) {
                    key(name)
                } else {
                    key("category_${categoryIndex}_$name")
                }
            }

            category.categoryName.text = dataPreferences.getString(
                categoryKey("category_name"),
                ""
            ) ?: ""

            category.itemName.text = dataPreferences.getString(
                categoryKey("item_name"),
                ""
            ) ?: ""

            category.brandName.text = dataPreferences.getString(
                categoryKey("brand_name"),
                ""
            ) ?: ""

            category.nameName.text = dataPreferences.getString(
                categoryKey("name_name"),
                ""
            ) ?: ""

            category.weightName.text = dataPreferences.getString(
                categoryKey("weight_name"),
                ""
            ) ?: ""

            category.quantityName.text = dataPreferences.getString(
                categoryKey("quantity_name"),
                ""
            ) ?: ""

            // UPDATE SAVED VALUES
            category.savedCategoryName = category.categoryName.text.toString()
            category.savedItemName = category.itemName.text.toString()

            category.savedBrandName = category.brandName.text.toString()
            category.savedNameName = category.nameName.text.toString()
            category.savedWeightName = category.weightName.text.toString()
            category.savedQuantityName = category.quantityName.text.toString()

            // CATEGORY EXPANSION KEYS
            val categoryExpandedKey = if (categoryIndex == 0) {
                key("category_expanded")
            } else {
                key("category_${categoryIndex}_expanded")
            }

            val itemExpandedKey = if (categoryIndex == 0) {
                key("item_expanded")
            } else {
                key("category_${categoryIndex}_item_expanded")
            }

            category.categoryExpanded = statePreferences.getBoolean(
                categoryExpandedKey,
                false
            )

            category.itemExpanded = statePreferences.getBoolean(
                itemExpandedKey,
                false
            )
        }

        // LOAD PLACE EXPANSION
        group.placeExpanded = statePreferences.getBoolean(
            key("place_expanded"),
            false
        )
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

            // SAVE PLACE
            dataEditor.putString(
                key("place_name"),
                group.placeName.text.toString()
            )

            // SAVE CATEGORY COUNT
            dataEditor.putInt(
                key("category_count"),
                group.categories.size
            )

            // SAVE PLACE EXPANSION
            stateEditor.putBoolean(
                key("place_expanded"),
                group.placeExpanded
            )

            // SAVE CATEGORIES
            for ((categoryIndex, category) in group.categories.withIndex()) {

                fun categoryKey(name: String): String {
                    return if (categoryIndex == 0) {
                        key(name)
                    } else {
                        key("category_${categoryIndex}_$name")
                    }
                }

                dataEditor.putString(
                    categoryKey("category_name"),
                    category.categoryName.text.toString()
                )

                dataEditor.putString(
                    categoryKey("item_name"),
                    category.itemName.text.toString()
                )

                dataEditor.putString(
                    categoryKey("brand_name"),
                    category.brandName.text.toString()
                )

                dataEditor.putString(
                    categoryKey("name_name"),
                    category.nameName.text.toString()
                )

                dataEditor.putString(
                    categoryKey("weight_name"),
                    category.weightName.text.toString()
                )

                dataEditor.putString(
                    categoryKey("quantity_name"),
                    category.quantityName.text.toString()
                )

                stateEditor.putBoolean(
                    key(
                        if (categoryIndex == 0) {
                            "category_expanded"
                        } else {
                            "category_${categoryIndex}_expanded"
                        }
                    ),
                    category.categoryExpanded
                )

                stateEditor.putBoolean(
                    key(
                        if (categoryIndex == 0) {
                            "item_expanded"
                        } else {
                            "category_${categoryIndex}_item_expanded"
                        }
                    ),
                    category.itemExpanded
                )
            }
        }

        dataEditor.apply()
        stateEditor.apply()
    }

    // UPDATE SAVED NAMES
    private fun updateSavedNames() {

        for (group in groups) {

            group.savedPlaceName = group.placeName.text.toString()

            for (category in group.categories) {

                category.savedCategoryName =
                    category.categoryName.text.toString()

                category.savedItemName =
                    category.itemName.text.toString()

                category.savedBrandName =
                    category.brandName.text.toString()

                category.savedNameName =
                    category.nameName.text.toString()

                category.savedWeightName =
                    category.weightName.text.toString()

                category.savedQuantityName =
                    category.quantityName.text.toString()
            }
        }
    }

    // SAVE EXPANSION STATE
    private fun saveExpansionState(group: GroupUi) {

        val groupIndex = groups.indexOf(group)

        fun key(name: String): String {
            return if (groupIndex == 0) {
                name
            } else {
                "group_${groupIndex}_$name"
            }
        }

        val editor = statePreferences.edit()

        editor.putBoolean(
            key("place_expanded"),
            group.placeExpanded
        )

        for ((categoryIndex, category) in group.categories.withIndex()) {

            val categoryExpandedKey = if (categoryIndex == 0) {
                key("category_expanded")
            } else {
                key("category_${categoryIndex}_expanded")
            }

            val itemExpandedKey = if (categoryIndex == 0) {
                key("item_expanded")
            } else {
                key("category_${categoryIndex}_item_expanded")
            }

            editor.putBoolean(
                categoryExpandedKey,
                category.categoryExpanded
            )

            editor.putBoolean(
                itemExpandedKey,
                category.itemExpanded
            )
        }

        editor.apply()
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

    // FIND VIEW IN GROUP
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

    // FIND VIEW IN CATEGORY
    private fun <T : View> findInCategory(
        category: CategoryUi,
        originalId: Int
    ): T? {

        val actualId = category.idMap.get(
            originalId,
            originalId
        )

        return category.root.findViewById(actualId)
    }

    // FIND VIEW IN CATEGORY ROOT
    private fun <T : View> findInCategory(
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

        findInGroup<TextView>(
            group,
            R.id.a_p_expand
        )?.apply {

            text = if (group.placeExpanded) "v" else ">"

            textSize = 20f
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        for (category in group.categories) {

            findInCategory<TextView>(
                category,
                R.id.a_c_expand
            )?.apply {

                text = if (category.categoryExpanded) "v" else ">"

                textSize = 20f
                gravity = Gravity.CENTER
                includeFontPadding = false
            }

            findInCategory<TextView>(
                category,
                R.id.a_i_expand
            )?.apply {

                text = if (category.itemExpanded) "v" else ">"

                textSize = 20f
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
        }
    }

    // UPDATE ONE GROUP'S EXPANSION
    private fun updateGroupExpansion(group: GroupUi) {

        for (category in group.categories) {

            // CATEGORY AND ITEM CONTAINER
            category.root.visibility =
                if (group.placeExpanded) View.VISIBLE else View.GONE

            // ITEM SECTION
            findInCategory<View>(
                category,
                R.id.item_section
            )?.visibility =
                if (group.placeExpanded && category.categoryExpanded) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            // BRAND HEADER
            findInCategory<View>(
                category,
                R.id.item_brand_bar
            )?.visibility =
                if (
                    group.placeExpanded &&
                    category.categoryExpanded &&
                    category.itemExpanded
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
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

            val mainVisibility =
                if (editMode) View.VISIBLE else View.GONE

            // PLACE SORT
            findInGroup<View>(
                group,
                R.id.c_p_sort
            )?.visibility =
                if (editMode && group.isOriginal) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            // PLACE EDIT BUTTON
            findInGroup<View>(
                group,
                R.id.c_p_edit
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

            // EACH CATEGORY HAS ITS OWN CONTROLS
            for ((index, category) in group.categories.withIndex()) {

                // CATEGORY SORT ONLY ONCE PER PLACE
                findInCategory<View>(
                    category,
                    R.id.c_c_sort
                )?.visibility =
                    if (editMode && index == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                // CATEGORY EDIT BUTTON
                findInCategory<View>(
                    category,
                    R.id.c_c_edit
                )?.visibility = mainVisibility

                // ITEM EDIT BUTTON
                findInCategory<View>(
                    category,
                    R.id.c_i_edit
                )?.visibility = mainVisibility

                // ITEM SORT
                findInCategory<View>(
                    category,
                    R.id.c_i_sort
                )?.visibility = mainVisibility

                // BRAND CONTROLS
                findInCategory<View>(
                    category,
                    R.id.c_i_b_add
                )?.visibility = mainVisibility

                findInCategory<View>(
                    category,
                    R.id.c_i_b_remove
                )?.visibility = mainVisibility

                // CATEGORY CONTROLS
                val categoryVisibility =
                    if (editMode && category.categoryEditExpanded) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                findInCategory<View>(
                    category,
                    R.id.c_c_add
                )?.visibility = categoryVisibility

                findInCategory<View>(
                    category,
                    R.id.c_c_remove
                )?.visibility = categoryVisibility

                findInCategory<View>(
                    category,
                    R.id.c_c_add_list
                )?.visibility = categoryVisibility

                // ITEM CONTROLS
                val itemVisibility =
                    if (editMode && category.itemEditExpanded) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                findInCategory<View>(
                    category,
                    R.id.c_i_add
                )?.visibility = itemVisibility

                findInCategory<View>(
                    category,
                    R.id.c_i_remove
                )?.visibility = itemVisibility

                findInCategory<View>(
                    category,
                    R.id.c_i_add_list
                )?.visibility = itemVisibility
            }
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

        for (category in group.categories) {

            category.categoryChanged = false
            category.itemChanged = false

            category.brandChanged = false
            category.nameChanged = false
            category.weightChanged = false
            category.quantityChanged = false
        }
    }

    // UPDATE NAME COLORS
    private fun updateAllNameColors() {

        for (group in groups) {
            updateNameColors(group)
        }
    }

    private fun updateNameColors(group: GroupUi) {

        // PLACE
        group.placeName.setTextColor(
            if (!editMode || group.placeChanged) white else muted
        )

        // EACH CATEGORY
        for (category in group.categories) {

            if (!editMode) {

                category.categoryName.setTextColor(white)
                category.itemName.setTextColor(white)

                category.brandName.setTextColor(muted)
                category.nameName.setTextColor(muted)
                category.weightName.setTextColor(muted)
                category.quantityName.setTextColor(muted)

                continue
            }

            // CATEGORY / ITEM
            category.categoryName.setTextColor(
                if (category.categoryChanged) white else muted
            )

            category.itemName.setTextColor(
                if (category.itemChanged) white else muted
            )

            // BRAND / NAME / WEIGHT / QUANTITY
            category.brandName.setTextColor(
                if (category.brandChanged) muted else white
            )

            category.nameName.setTextColor(
                if (category.nameChanged) muted else white
            )

            category.weightName.setTextColor(
                if (category.weightChanged) muted else white
            )

            category.quantityName.setTextColor(
                if (category.quantityChanged) muted else white
            )
        }
    }

    // CHECK CHANGES
    private fun hasChanges(): Boolean {

        return groups.any { group ->

            group.placeChanged ||
                    group.categories.any { category ->

                        category.categoryChanged ||
                                category.itemChanged ||
                                category.brandChanged ||
                                category.nameChanged ||
                                category.weightChanged ||
                                category.quantityChanged
                    }
        }
    }

    // REQUIRED NAMES AND DUPLICATE VALIDATION
    private fun allRequiredNamesFilled(): Boolean {

        // NORMALIZE TEXT
        fun normalize(text: CharSequence): String {
            return text.toString()
                .trim()
                .lowercase(Locale.ROOT)
        }

        // PLACE NAMES CANNOT BE EMPTY
        if (groups.any {
                normalize(it.placeName.text).isEmpty()
            }) {
            return false
        }

        // PLACE NAMES MUST BE UNIQUE
        val placeNames = groups.map {
            normalize(it.placeName.text)
        }

        if (placeNames.size != placeNames.toSet().size) {
            return false
        }

        // VALIDATE EACH PLACE
        for (group in groups) {

            if (group.categories.isEmpty()) {
                return false
            }

            // CATEGORY AND ITEM NAMES CANNOT BE EMPTY
            if (group.categories.any { category ->

                    normalize(category.categoryName.text).isEmpty() ||
                            normalize(category.itemName.text).isEmpty()

                }) {
                return false
            }

            // CATEGORY NAMES MUST BE UNIQUE WITHIN THIS PLACE
            val categoryNames = group.categories.map {
                normalize(it.categoryName.text)
            }

            if (categoryNames.size != categoryNames.toSet().size) {
                return false
            }
        }

        return true
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