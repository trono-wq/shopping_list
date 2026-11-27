package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.util.SparseIntArray
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import android.content.ClipData
import android.view.View
import android.view.ViewGroup

class Complete : AppCompatActivity() {

    private var editMode = false
    private var structureChanged = false
    private var searchQuery = ""

    private val gray = Color.rgb(102, 102, 102)
    private val muted = Color.rgb(153, 153, 153)
    private val white = Color.WHITE

    private val statePreferences by lazy {
        getSharedPreferences("complete_list_state", MODE_PRIVATE)
    }

    private val dataPreferences by lazy {
        getSharedPreferences("complete_list_data", MODE_PRIVATE)
    }

    private lateinit var scroll: LinearLayout
    private lateinit var editButton: AppCompatButton

    private data class BrandUi(
        val root: TableRow,
        val idMap: SparseIntArray,

        val brandName: TextView,
        val nameName: TextView,
        val weightName: TextView,
        val quantityName: TextView,

        var savedBrandName: String = "",
        var savedNameName: String = "",
        var savedWeightName: String = "",
        var savedQuantityName: String = "",

        var brandChanged: Boolean = false,
        var nameChanged: Boolean = false,
        var weightChanged: Boolean = false,
        var quantityChanged: Boolean = false
    )

    private data class ItemUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,

        val itemName: TextView,
        val brandContainer: TableLayout,

        var itemExpanded: Boolean = false,
        var itemEditExpanded: Boolean = false,

        var savedItemName: String = "",
        var itemChanged: Boolean = false,

        val brands: MutableList<BrandUi> = mutableListOf()
    )

    private data class CategoryUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,

        val categoryName: TextView,
        val itemContainer: LinearLayout,

        var categoryExpanded: Boolean = false,
        var categoryEditExpanded: Boolean = false,

        var savedCategoryName: String = "",
        var categoryChanged: Boolean = false,

        val items: MutableList<ItemUi> = mutableListOf()
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

    private var originalStructure: List<Any> = emptyList()

    private val workingSelections = mutableSetOf<String>()
    private var savedSelections = mutableSetOf<String>()

    private fun hasSelectionChanges(): Boolean {
        return workingSelections != savedSelections
    }

    private fun setWorkingSelected(keys: Collection<String>, selected: Boolean) {
        if (selected) {
            workingSelections.addAll(keys)

            android.util.Log.d(
                "SELECTION_DEBUG",
                "setWorkingSelected ADD: ${keys.map { ShoppingListStore.parts(it) }}"
            )
        } else {
            workingSelections.removeAll(keys.toSet())
        }

        updateSaveButton()
    }

    private fun toggleWorkingSelection(key: String) {
        if (!workingSelections.add(key)) {
            workingSelections.remove(key)
        } else {
            android.util.Log.d(
                "SELECTION_DEBUG",
                "toggleWorkingSelection ADD: ${ShoppingListStore.parts(key)}"
            )
        }

        updateSaveButton()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.complete)

        workingSelections.clear()
        workingSelections.addAll(
            ShoppingListStore.getSelections(this)
        )

        savedSelections = workingSelections.toMutableSet()

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

        findViewById<TextView>(R.id.a_gucci_mane)
            .setOnClickListener {

                val intent = Intent(this, Gucci_Mane::class.java)

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                startActivity(intent)
            }

        findViewById<TextView>(R.id.a_shopping).setOnClickListener {
            finish()
        }

        scroll = findViewById(R.id.scroll)
        editButton = findViewById(R.id.c_edit)

        findViewById<EditText>(R.id.a_search).addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    searchQuery = s?.toString()?.trim().orEmpty()
                    updateAllExpansionVisibility()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }
        )

// ORIGINAL GROUP
        val originalRoot = findViewById<LinearLayout>(R.id.group)

        val originalGroup = setupGroup(
            root = originalRoot,
            idMap = SparseIntArray(),
            isOriginal = true
        )

        groups.add(originalGroup)

        enableListDragging(
            scroll,
            groups,
            { it.root }
        ) {
            structureChanged = true

            updateGroupSpacing()
            updateAllExpansionVisibility()
            updateEditVisibility()
            updateSaveButton()
            updateSortToggles()
        }

        loadGroupData(originalGroup, 0)

        originalGroup.placeExpanded =
            statePreferences.getBoolean("place_expanded", false)

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

                originalStructure = currentStructure()

                for (group in groups) {

                    resetChangeFlags(group)

                    group.placeEditExpanded = false

                    for (category in group.categories) {

                        category.categoryEditExpanded = false

                        for (item in category.items) {
                            item.itemEditExpanded = false
                        }
                    }
                }

                updateEditVisibility()
                updateAllNameColors()
                updateSaveButton()
                updateSortToggles()

            } else {

                if (hasChanges() && allRequiredNamesFilled()) {
                    persistAllGroups()
                    updateSavedNames()
                }

                if (hasSelectionChanges() && allRequiredNamesFilled()) {

                    android.util.Log.d(
                        "SELECTION_DEBUG",
                        "Complete saving selections: ${workingSelections.map {
                            ShoppingListStore.parts(it)
                        }}"
                    )

                    ShoppingListStore.saveSelections(
                        this,
                        workingSelections
                    )

                    savedSelections = workingSelections.toMutableSet()

                    refreshShoppingData(persist = true)
                }

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

                        for (item in category.items) {
                            item.itemEditExpanded = false
                        }
                    }
                }

                updateEditVisibility()
                updateAllNameColors()
                updateSaveButton()
                updateSortToggles()
            }
        }

        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        refreshShoppingData()
        updateShoppingButtonColors()
        updateSortToggles()
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

        if (group.placeName.text.toString() == "[Place]") {
            group.placeName.text = ""

        }

        group.placeName.hint = "[Place]"

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

        (categorySection.parent as? ViewGroup)?.removeView(categorySection)
        (itemSection.parent as? ViewGroup)?.removeView(itemSection)

        val itemContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        itemContainer.addView(itemSection)

        val categoryRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        categoryRoot.addView(categorySection)
        categoryRoot.addView(itemContainer)

        root.addView(categoryRoot)

        val firstCategory = setupCategory(
            group = group,
            root = categoryRoot,
            categorySection = categorySection,
            itemContainer = itemContainer,

            firstItemSection = itemSection,
            idMap = idMap
        )

        group.categories.add(firstCategory)

        enableListDragging(
            group.categoryContainer,
            group.categories,
            { it.root }
        ) {
            structureChanged = true
            updateAllExpansionVisibility()
            updateEditVisibility()
            updateSaveButton()
            updateSortToggles()
        }

        findInGroup<View>(
            group,
            R.id.a_p_expand
        )?.setOnClickListener {

            group.placeExpanded = !group.placeExpanded

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        findInGroup<View>(
            group,
            R.id.c_p_edit
        )?.setOnClickListener {

            if (editMode) {
                group.placeEditExpanded = !group.placeEditExpanded
                updateEditVisibility()
            }
        }

        group.placeName.setOnClickListener {

            if (editMode && !isPlaceLocked(group)) {
                editName(
                    group.placeName,
                    "[Place]",
                    group.savedPlaceName
                ) { changed ->

                    group.placeChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                    updateSortToggles()
                }
            }
        }

        makeEntryDraggable(
            group.placeName,
            group.root,
            scroll
        ) {
            group.placeExpanded = false

            for (category in group.categories) {
                category.categoryExpanded = false

                for (item in category.items) {
                    item.itemExpanded = false
                }
            }

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        findInGroup<View>(
            group,
            R.id.c_p_add
        )?.setOnClickListener {
            addGroup(afterGroup = group)
        }

        findInGroup<View>(
            group,
            R.id.c_p_remove
        )?.setOnClickListener {
            removeGroup(group)
        }

        findInGroup<View>(
            group,
            R.id.c_p_sort
        )?.setOnClickListener {
            sortGroups()
        }


// SHOPPING LIST BUTTONS

        findInGroup<View>(group, R.id.c_p_add_list)
            ?.setOnClickListener {

                val keys = allItemKeysInPlace(group)
                val selected = workingSelections
                val shouldSelect = keys.isNotEmpty() && !keys.all { it in selected }

                clearAggregateSelectionKeys(group)
                android.util.Log.d("SELECTION_DEBUG", "CALLER: c_p_add_list - ${group.placeName.text}")
                setWorkingSelected(keys, shouldSelect)

                refreshShoppingData()
                updateShoppingButtonColors()
                updateAllNameColors()
            }

        findInGroup<View>(group, R.id.c_p_add_all_list)
            ?.setOnClickListener {

                val keys = allItemKeysInShoppingList()
                val selected = workingSelections
                val shouldSelect = keys.isNotEmpty() && !keys.all { it in selected }

                clearAllAggregateSelectionKeys()
                android.util.Log.d("SELECTION_DEBUG", "CALLER: c_p_add_all_list")
                setWorkingSelected(keys, shouldSelect)

                refreshShoppingData()
                updateShoppingButtonColors()
                updateAllNameColors()
            }

        return group
    }

    // SETUP CATEGORY
    private fun setupCategory(
        group: GroupUi,
        root: LinearLayout,
        categorySection: LinearLayout,
        itemContainer: LinearLayout,
        firstItemSection: LinearLayout,

        idMap: SparseIntArray
    ): CategoryUi {

        val category = CategoryUi(
            root = root,
            idMap = idMap,

            categoryName = findInCategory(
                categorySection,
                idMap,
                R.id.a_c_name
            )!!,

            itemContainer = itemContainer
        )

        if (category.categoryName.text.toString() == "[Category]") {
            category.categoryName.text = ""
        }

        category.categoryName.hint = "[Category]"

        val firstItem = setupItem(
            root = firstItemSection,
            idMap = idMap,
            category = category
        )

        category.items.add(firstItem)

        enableListDragging(
            category.itemContainer,
            category.items,
            { it.root }
        ) {
            structureChanged = true
            updateAllExpansionVisibility()
            updateEditVisibility()
            updateSaveButton()
            updateSortToggles()
        }

        findInCategory<View>(
            category,
            R.id.a_c_expand
        )?.setOnClickListener {

            category.categoryExpanded = !category.categoryExpanded

            saveExpansionState(group)

            updateGroupExpansion(group)
        }

        findInCategory<View>(
            category,
            R.id.c_c_edit
        )?.setOnClickListener {

            if (editMode) {
                category.categoryEditExpanded =
                    !category.categoryEditExpanded

                updateEditVisibility()
            }
        }

        category.categoryName.setOnClickListener {

            if (editMode && !isCategoryLocked(group, category)) {
                editName(
                    category.categoryName,
                    "[Category]",
                    category.savedCategoryName
                ) { changed ->

                    category.categoryChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                    updateSortToggles()
                }
            }
        }

        makeEntryDraggable(
            category.categoryName,
            category.root,
            group.categoryContainer
        ) {
            category.categoryExpanded = false

            for (item in category.items) {
                item.itemExpanded = false
            }

            saveExpansionState(group)
            updateGroupExpansion(group)
        }

        findInCategory<View>(

            category,
            R.id.c_c_add
        )?.setOnClickListener {
            addCategory(group, category)
        }

        findInCategory<View>(category, R.id.c_c_remove)?.setOnClickListener {
            removeCategory(group, category)
        }

        findInCategory<View>(
            category,
            R.id.c_c_sort
        )?.setOnClickListener {
            sortCategories(group)
        }

        // SHOPPING LIST BUTTONS

        findInCategory<View>(category, R.id.c_c_add_list)
            ?.setOnClickListener {

                if (isPlaceFullySelected(group)) return@setOnClickListener

                val keys = allItemKeysInCategory(group, category)
                val selected = workingSelections
                val shouldSelect = keys.isNotEmpty() && !keys.all { it in selected }

                clearAggregateSelectionKeys(group)
                android.util.Log.d(
                    "SELECTION_DEBUG",
                    "CALLER: c_c_add_list - ${category.categoryName.text}"
                )
                setWorkingSelected(keys, shouldSelect)

                refreshShoppingData()
                updateShoppingButtonColors()
                updateAllNameColors()
            }

        return category
    }

    // SETUP ITEM
    private fun setupItem(
        root: LinearLayout,
        idMap: SparseIntArray,
        category: CategoryUi
    ): ItemUi {

        val item = ItemUi(

            root = root,
            idMap = idMap,

            itemName = findInItem(
                root,
                idMap,
                R.id.a_i_name
            )!!,

            brandContainer = findInItem(
                root,
                idMap,
                R.id.item_brand_bar
            )!!
        )

        if (item.itemName.text.toString() == "[Item]") {
            item.itemName.text = ""
        }

        item.itemName.hint = "[Item]"

        val firstRow = item.brandContainer.getChildAt(0) as TableRow

        val firstBrand = setupBrand(
            root = firstRow,
            idMap = idMap,
            item = item
        )

        item.brands.add(firstBrand)

        enableListDragging(
            item.brandContainer,
            item.brands,
            { it.root }
        ) {
            structureChanged = true
            updateEditVisibility()
            updateSaveButton()
            updateSortToggles()
        }

        findInItem<View>(
            item,
            R.id.a_i_expand
        )?.setOnClickListener {

            item.itemExpanded = !item.itemExpanded

            val group = groups.firstOrNull { group ->
                group.categories.any { category ->
                    category.items.contains(item)
                }
            }

            if (group != null) {
                saveExpansionState(group)
                updateGroupExpansion(group)
            }
        }

        findInItem<View>(
            item,
            R.id.c_i_edit
        )?.setOnClickListener {

            if (editMode) {
                item.itemEditExpanded = !item.itemEditExpanded
                updateEditVisibility()
            }
        }

        item.itemName.setOnClickListener {

            if (editMode) {

                val parentGroup = groups.firstOrNull {
                    it.categories.any { parentCategory ->
                        parentCategory.items.contains(item)
                    }
                }

                if (parentGroup == null ||
                    isItemLocked(parentGroup, category, item)
                ) {
                    return@setOnClickListener
                }

                editName(
                    item.itemName,
                    "[Item]",
                    item.savedItemName
                ) { changed ->

                    item.itemChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                    updateSortToggles()

                }
            }
        }

        makeEntryDraggable(
            item.itemName,
            item.root,
            category.itemContainer
        ) {
            item.itemExpanded = false

            val group = groups.firstOrNull {
                it.categories.contains(category)
            }

            if (group != null) {
                saveExpansionState(group)
                updateGroupExpansion(group)
            } else {
                item.brandContainer.visibility = View.GONE
            }
        }

// ADD ITEM
        findInItem<View>(
            item,
            R.id.c_i_add
        )?.setOnClickListener {

            val category = groups
                .flatMap { it.categories }
                .firstOrNull { it.items.contains(item) }

            if (category != null) {
                addItem(category, item)
            }
        }

        findInItem<View>(item, R.id.c_i_remove)?.setOnClickListener {
            removeItem(category, item)
        }

        findInItem<View>(
            item,
            R.id.c_i_sort
        )?.setOnClickListener {
            sortItems(category)
        }

        // SHOPPING LIST BUTTONS

        findInItem<View>(item, R.id.c_i_add_list)
            ?.setOnClickListener {

                val parentGroup = groups.firstOrNull {
                    it.categories.any { category ->
                        category.items.contains(item)
                    }
                }

                val parentCategory = groups
                    .flatMap { it.categories }
                    .firstOrNull { it.items.contains(item) }

                if (parentGroup != null && parentCategory != null) {

                    if (
                        isPlaceFullySelected(parentGroup) ||
                        isCategoryFullySelected(parentGroup, parentCategory)
                    ) {
                        return@setOnClickListener
                    }

                    toggleWorkingSelection(
                        itemKey(parentGroup, parentCategory, item)
                    )

                    clearAggregateSelectionKeys(parentGroup)
                    refreshShoppingData()
                    updateShoppingButtonColors()
                    updateAllNameColors()
                }
            }

        return item
    }

    // SETUP BRAND ROW
    private fun setupBrand(
        root: TableRow,
        idMap: SparseIntArray,
        item: ItemUi
    ): BrandUi {

        val brand = BrandUi(
            root = root,
            idMap = idMap,

            brandName = findInBrand(root, idMap, R.id.a_b_brand)!!,
            nameName = findInBrand(root, idMap, R.id.a_b_name)!!,
            weightName = findInBrand(root, idMap, R.id.a_b_weight)!!,
            quantityName = findInBrand(root, idMap, R.id.a_b_quantity)!!
        )

        val placeholders = listOf(

            brand.brandName to "[Brand]",
            brand.nameName to "[Name]",
            brand.weightName to "[Weight]",
            brand.quantityName to "[Quantity]"
        )

        for ((textView, placeholder) in placeholders) {

            if (textView.text.toString() == placeholder) {
                textView.text = ""
            }

            textView.hint = placeholder
        }

        brand.brandName.setOnClickListener {
            if (editMode && !isItemLockedForBrand(item)) {
                editBrandField(
                    brand,
                    brand.brandName,
                    "[Brand]",
                    brand.savedBrandName,
                    0
                )
            }
        }

        brand.nameName.setOnClickListener {
            if (editMode && !isItemLockedForBrand(item)) {
                editBrandField(
                    brand,
                    brand.nameName,
                    "[Name]",
                    brand.savedNameName,
                    1
                )
            }
        }

        brand.weightName.setOnClickListener {
            if (editMode && !isItemLockedForBrand(item)) {
                editBrandField(
                    brand,
                    brand.weightName,
                    "[Weight]",
                    brand.savedWeightName,
                    2
                )
            }

        }

        brand.quantityName.setOnClickListener {
            if (editMode && !isItemLockedForBrand(item)) {
                editBrandField(
                    brand,
                    brand.quantityName,
                    "[Quantity]",
                    brand.savedQuantityName,
                    3
                )
            }
        }

        makeEntryDraggable(
            brand.brandName,
            brand.root,
            item.brandContainer
        ) {}

        makeEntryDraggable(
            brand.nameName,
            brand.root,
            item.brandContainer
        ) {}

        makeEntryDraggable(
            brand.weightName,
            brand.root,
            item.brandContainer
        ) {}

        makeEntryDraggable(
            brand.quantityName,
            brand.root,
            item.brandContainer
        ) {}

// ADD BRAND ROW
        findInBrand<View>(
            root,
            idMap,
            R.id.c_i_b_add
        )?.setOnClickListener {

            val item = groups
                .flatMap { it.categories }
                .flatMap { it.items }
                .firstOrNull { it.brands.contains(brand) }

            if (item != null) {
                addBrand(item, brand)
            }
        }

        findInBrand<View>(
            root,
            idMap,
            R.id.c_i_b_remove
        )?.setOnClickListener {
            removeBrand(item, brand)
        }

        return brand
    }

    // EDIT BRAND FIELD
// EDIT BRAND FIELD
    private fun editBrandField(
        brand: BrandUi,
        textView: TextView,
        hint: String,
        savedName: String,
        field: Int
    ) {

        val keyboardType = if (field == 3) {
            InputType.TYPE_CLASS_NUMBER
        } else {
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        editName(
            textView,
            hint,
            savedName,
            keyboardType
        ) { changed ->

            when (field) {
                0 -> brand.brandChanged = changed
                1 -> brand.nameChanged = changed
                2 -> brand.weightChanged = changed
                3 -> brand.quantityChanged = changed
            }

            updateAllNameColors()
            updateSaveButton()
            updateSortToggles()
        }
    }


    // ADD DUPLICATED CATEGORY
    private fun addCategory(
        group: GroupUi,
        afterCategory: CategoryUi
    ) {

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

        (categorySection.parent as? ViewGroup)?.removeView(categorySection)
        (itemSection.parent as? ViewGroup)?.removeView(itemSection)

        val itemContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        itemContainer.addView(itemSection)

        val categoryRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,

                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        categoryRoot.addView(categorySection)
        categoryRoot.addView(itemContainer)

        val idMap = SparseIntArray()
        regenerateViewIds(categoryRoot, idMap)

        val newCategory = setupCategory(
            group = group,
            root = categoryRoot,
            categorySection = categorySection,
            itemContainer = itemContainer,
            firstItemSection = itemSection,
            idMap = idMap
        )

        newCategory.categoryName.text = ""
        newCategory.savedCategoryName = ""

        for (item in newCategory.items) {
            clearItem(item)
        }

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
        updateSortToggles()
        updateRemoveVisibility(group)
        refreshShoppingData()
        updateShoppingButtonColors()
    }

    // ADD DUPLICATED ITEM
    private fun addItem(
        category: CategoryUi,
        afterItem: ItemUi
    ) {

        val tempRoot = layoutInflater.inflate(
            R.layout.complete,
            null,
            false
        ) as ViewGroup

        val tempGroup = tempRoot.findViewById<LinearLayout>(
            R.id.group
        )

        val itemSection = tempGroup.findViewById<LinearLayout>(
            R.id.item_section
        )

        (itemSection.parent as? ViewGroup)?.removeView(itemSection)

        val idMap = SparseIntArray()
        regenerateViewIds(itemSection, idMap)

        val newItem = setupItem(
            root = itemSection,
            idMap = idMap,
            category = category
        )

        clearItem(newItem)

        val itemIndex = category.items.indexOf(afterItem) + 1
        val viewIndex = category.itemContainer.indexOfChild(afterItem.root) + 1

        category.items.add(itemIndex, newItem)

        category.itemContainer.addView(
            itemSection,
            viewIndex
        )

        updateAllExpansionVisibility()
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
        updateRemoveVisibilityForCategory(category)
        refreshShoppingData()
        updateShoppingButtonColors()
    }

    // ADD DUPLICATED BRAND ROW
    private fun addBrand(
        item: ItemUi,
        afterBrand: BrandUi

    ) {

        val tempRoot = layoutInflater.inflate(
            R.layout.complete,
            null,
            false
        ) as ViewGroup

        val tempGroup = tempRoot.findViewById<LinearLayout>(
            R.id.group
        )

        val tempItemSection = tempGroup.findViewById<LinearLayout>(
            R.id.item_section
        )

        val tempBrandContainer = tempItemSection.findViewById<TableLayout>(
            R.id.item_brand_bar
        )

        val brandRow = tempBrandContainer.getChildAt(0) as TableRow

        (brandRow.parent as? ViewGroup)?.removeView(brandRow)

        val idMap = SparseIntArray()
        regenerateViewIds(brandRow, idMap)

        val newBrand = setupBrand(
            root = brandRow,
            idMap = idMap,
            item = item
        )

        clearBrand(newBrand)

        val brandIndex = item.brands.indexOf(afterBrand) + 1
        val viewIndex = item.brandContainer.indexOfChild(afterBrand.root) + 1

        item.brands.add(brandIndex, newBrand)

        item.brandContainer.addView(
            brandRow,
            viewIndex
        )

        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
        updateRemoveVisibilityForItem(item)

    }

    private fun clearItem(item: ItemUi) {

        item.itemName.text = ""
        item.savedItemName = ""
        item.itemChanged = false
        item.itemExpanded = false
        item.itemEditExpanded = false

        for (brand in item.brands) {
            clearBrand(brand)
        }
    }

    private fun clearBrand(brand: BrandUi) {

        brand.brandName.text = ""
        brand.nameName.text = ""
        brand.weightName.text = ""
        brand.quantityName.text = ""

        brand.savedBrandName = ""
        brand.savedNameName = ""
        brand.savedWeightName = ""
        brand.savedQuantityName = ""

        brand.brandChanged = false
        brand.nameChanged = false
        brand.weightChanged = false
        brand.quantityChanged = false
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

        val idMap = SparseIntArray()
        regenerateViewIds(newGroup, idMap)

        val newGroupUi = setupGroup(
            root = newGroup,
            idMap = idMap,
            isOriginal = false
        )

        val insertIndex = if (afterGroup != null) {
            groups.indexOf(afterGroup) + 1
        } else {
            groups.size
        }

        groups.add(insertIndex, newGroupUi)

        if (restoreIndex != null) {

            loadGroupData(newGroupUi, restoreIndex)

        } else {

            newGroupUi.placeName.text = ""
            newGroupUi.savedPlaceName = ""

            for (category in newGroupUi.categories) {

                category.categoryName.text = ""
                category.savedCategoryName = ""

                for (item in category.items) {
                    clearItem(item)
                }

                category.categoryExpanded = false
            }

            newGroupUi.placeExpanded = false
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.topMargin = if (insertIndex == 0) 0 else dp(20)

        (newGroup.parent as? ViewGroup)?.removeView(newGroup)

        scroll.addView(newGroup, insertIndex, params)

        updateDropdownSymbols(newGroupUi)
        updateGroupExpansion(newGroupUi)
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
        if (restoreIndex == null) {
            refreshShoppingData()
            updateShoppingButtonColors()
        }
    }


    // UPDATE PLACE SPACING
    private fun updateGroupSpacing() {

        for ((index, group) in groups.withIndex()) {

            val params = group.root.layoutParams as? LinearLayout.LayoutParams
                ?: continue

            params.topMargin = if (index == 0) 0 else dp(20)

            group.root.layoutParams = params
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
            ""
        ) ?: ""

        group.savedPlaceName = group.placeName.text.toString()

        val categoryCount = dataPreferences.getInt(
            key("category_count"),

            1
        )

        while (group.categories.size < categoryCount) {
            addCategory(group, group.categories.last())
        }

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

            category.savedCategoryName = category.categoryName.text.toString()

            val itemCount = dataPreferences.getInt(
                categoryKey("item_count"),
                1
            )

            while (category.items.size < itemCount) {
                addItem(category, category.items.last())
            }

            for ((itemIndex, item) in category.items.withIndex()) {

                fun itemKey(name: String): String {
                    return if (itemIndex == 0) {
                        categoryKey(name)
                    } else {
                        categoryKey("item_${itemIndex}_$name")
                    }
                }

                item.itemName.text = dataPreferences.getString(
                    itemKey("item_name"),
                    ""
                ) ?: ""

                item.savedItemName = item.itemName.text.toString()

                val brandCount = dataPreferences.getInt(
                    itemKey("brand_count"),
                    1
                )

                while (item.brands.size < brandCount) {
                    addBrand(item, item.brands.last())
                }

                for ((brandIndex, brand) in item.brands.withIndex()) {

                    fun brandKey(name: String): String {
                        return if (itemIndex == 0 && brandIndex == 0) {
                            categoryKey(name)
                        } else {
                            itemKey("brand_${brandIndex}_$name")
                        }
                    }

                    brand.brandName.text = dataPreferences.getString(
                        brandKey("brand_name"),
                        ""
                    ) ?: ""

                    brand.nameName.text = dataPreferences.getString(
                        brandKey("name_name"),
                        ""
                    ) ?: ""

                    brand.weightName.text = dataPreferences.getString(
                        brandKey("weight_name"),
                        ""
                    ) ?: ""

                    brand.quantityName.text = dataPreferences.getString(
                        brandKey("quantity_name"),
                        ""
                    ) ?: ""

                    brand.savedBrandName = brand.brandName.text.toString()
                    brand.savedNameName = brand.nameName.text.toString()
                    brand.savedWeightName = brand.weightName.text.toString()
                    brand.savedQuantityName = brand.quantityName.text.toString()
                }

                item.itemExpanded = statePreferences.getBoolean(
                    itemKey("item_expanded"),
                    if (itemIndex == 0) {

                        statePreferences.getBoolean(
                            if (categoryIndex == 0) key("item_expanded")
                            else key("category_${categoryIndex}_item_expanded"),
                            false
                        )
                    } else {
                        false
                    }
                )
            }

            val categoryExpandedKey = if (categoryIndex == 0) {
                key("category_expanded")
            } else {
                key("category_${categoryIndex}_expanded")
            }

            category.categoryExpanded = statePreferences.getBoolean(
                categoryExpandedKey,
                false
            )
        }

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

            dataEditor.putString(
                key("place_name"),

                group.placeName.text.toString()
            )

            dataEditor.putInt(
                key("category_count"),
                group.categories.size
            )

            stateEditor.putBoolean(
                key("place_expanded"),
                group.placeExpanded
            )

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

                stateEditor.putBoolean(
                    if (categoryIndex == 0) key("category_expanded")
                    else key("category_${categoryIndex}_expanded"),
                    category.categoryExpanded
                )

                dataEditor.putInt(
                    categoryKey("item_count"),
                    category.items.size
                )

                for ((itemIndex, item) in category.items.withIndex()) {

                    fun itemKey(name: String): String {
                        return if (itemIndex == 0) {
                            categoryKey(name)
                        } else {
                            categoryKey("item_${itemIndex}_$name")
                        }
                    }

                    dataEditor.putString(
                        itemKey("item_name"),
                        item.itemName.text.toString()
                    )

                    dataEditor.putInt(
                        itemKey("brand_count"),
                        item.brands.size
                    )

                    stateEditor.putBoolean(
                        itemKey("item_expanded"),
                        item.itemExpanded
                    )

                    for ((brandIndex, brand) in item.brands.withIndex()) {

                        fun brandKey(name: String): String {
                            return if (itemIndex == 0 && brandIndex == 0) {
                                categoryKey(name)
                            } else {
                                itemKey("brand_${brandIndex}_$name")
                            }
                        }

                        dataEditor.putString(
                            brandKey("brand_name"),
                            brand.brandName.text.toString()
                        )

                        dataEditor.putString(
                            brandKey("name_name"),
                            brand.nameName.text.toString()
                        )

                        dataEditor.putString(
                            brandKey("weight_name"),
                            brand.weightName.text.toString()
                        )

                        dataEditor.putString(
                            brandKey("quantity_name"),
                            brand.quantityName.text.toString()
                        )
                    }
                }
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

                for (item in category.items) {

                    item.savedItemName = item.itemName.text.toString()

                    for (brand in item.brands) {

                        brand.savedBrandName = brand.brandName.text.toString()
                        brand.savedNameName = brand.nameName.text.toString()
                        brand.savedWeightName = brand.weightName.text.toString()
                        brand.savedQuantityName =
                            brand.quantityName.text.toString()
                    }
                }
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

            editor.putBoolean(
                if (categoryIndex == 0) key("category_expanded")
                else key("category_${categoryIndex}_expanded"),
                category.categoryExpanded
            )

            for ((itemIndex, item) in category.items.withIndex()) {

                val itemKey = if (itemIndex == 0) {
                    if (categoryIndex == 0) key("item_expanded")
                    else key("category_${categoryIndex}_item_expanded")
                } else {

                    key("category_${categoryIndex}_item_${itemIndex}_item_expanded")
                }

                editor.putBoolean(
                    itemKey,
                    item.itemExpanded
                )
            }
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

    private fun <T : View> findInItem(
        item: ItemUi,
        originalId: Int
    ): T? {

        val actualId = item.idMap.get(
            originalId,
            originalId
        )

        return item.root.findViewById(actualId)
    }

    private fun <T : View> findInItem(
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

    private fun <T : View> findInBrand(
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

        val searching = searchQuery.isNotBlank()

        val placeMatches = group.placeName.text
            .toString()
            .contains(searchQuery, ignoreCase = true)

        findInGroup<TextView>(
            group,
            R.id.a_p_expand
        )?.let { view ->

            view.text = if (
                group.placeExpanded &&
                (!searching || placeMatches)
            ) "v" else ">"

            view.textSize = 20f
            view.gravity = Gravity.CENTER
            view.includeFontPadding = false
        }

        for (category in group.categories) {

            findInCategory<TextView>(
                category,
                R.id.a_c_expand
            )?.let { view ->

                view.text = if (category.categoryExpanded) "v" else ">"

                view.textSize = 20f
                view.gravity = Gravity.CENTER
                view.includeFontPadding = false
            }

            for (item in category.items) {

                findInItem<TextView>(

                    item,
                    R.id.a_i_expand
                )?.let { view ->

                    view.text = if (item.itemExpanded) "v" else ">"

                    view.textSize = 20f
                    view.gravity = Gravity.CENTER
                    view.includeFontPadding = false
                }
            }
        }
    }

    // UPDATE ONE GROUP'S EXPANSION
    private fun updateGroupExpansion(group: GroupUi) {

        val query = searchQuery.trim()

        // SEARCH MODE
        if (query.isNotBlank()) {

            val placeMatches = group.placeName.text
                .toString()
                .contains(query, ignoreCase = true)

            var groupHasMatch = placeMatches

            for (category in group.categories) {

                val categoryMatches = category.categoryName.text
                    .toString()
                    .contains(query, ignoreCase = true)

                var categoryHasMatch = categoryMatches

                val itemMatches = mutableMapOf<ItemUi, Boolean>()
                val brandMatches = mutableMapOf<BrandUi, Boolean>()

                for (item in category.items) {

                    val matchesItem = item.itemName.text
                        .toString()
                        .contains(query, ignoreCase = true)

                    var matchesBrand = false

                    for (brand in item.brands) {

                        val matches = listOf(
                            brand.brandName,
                            brand.nameName,
                            brand.weightName,
                            brand.quantityName
                        ).any {
                            it.text.toString()
                                .contains(query, ignoreCase = true)
                        }

                        brandMatches[brand] = matches

                        if (matches) {
                            matchesBrand = true
                        }
                    }

                    val matchesEntry = matchesItem || matchesBrand

                    itemMatches[item] = matchesEntry

                    if (matchesEntry) {
                        categoryHasMatch = true
                    }
                }

                if (categoryHasMatch) {
                    groupHasMatch = true
                }

                val categoryVisible =
                    categoryHasMatch ||
                            (placeMatches && group.placeExpanded)

                category.root.visibility =
                    if (categoryVisible) View.VISIBLE else View.GONE

                for (item in category.items) {

                    val matchesEntry = itemMatches[item] == true

                    val hasMatchingItemOrBrand = itemMatches.values.any { it }

                    val showChildren =
                        categoryVisible &&
                                category.categoryExpanded &&
                                !hasMatchingItemOrBrand

                    val itemVisible =
                        matchesEntry || showChildren

                    item.root.visibility =
                        if (itemVisible) View.VISIBLE else View.GONE

                    val showAllBrands =
                        itemVisible && item.itemExpanded

                    var matchingBrandFound = false

                    for (brand in item.brands) {

                        val matches = brandMatches[brand] == true

                        if (matches) {
                            matchingBrandFound = true
                        }

                        brand.root.visibility =
                            if (matches || showAllBrands) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }

                    item.brandContainer.visibility =
                        if (matchingBrandFound || showAllBrands) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }
            }

            group.root.visibility =
                if (groupHasMatch) View.VISIBLE else View.GONE

            updateDropdownSymbols(group)
            return
        }

// NORMAL EXPANSION

        group.root.visibility = View.VISIBLE

        for (category in group.categories) {

            category.root.visibility =
                if (group.placeExpanded) View.VISIBLE else View.GONE

            for (item in category.items) {

                item.root.visibility =
                    if (
                        group.placeExpanded &&
                        category.categoryExpanded
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                item.brandContainer.visibility =
                    if (
                        group.placeExpanded &&
                        category.categoryExpanded &&
                        item.itemExpanded
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                for (brand in item.brands) {
                    brand.root.visibility = View.VISIBLE
                }
            }
        }

        updateDropdownSymbols(group)
    }


    private fun updateAllExpansionVisibility() {

        for (group in groups) {
            updateGroupExpansion(group)
        }
    }


// UPDATE EDIT VISIBILITY

    private fun updateEditVisibility() {

        val selected = workingSelections
        val allKeys = allItemKeysInShoppingList()
        val globalSelected = allKeys.isNotEmpty() && allKeys.all { it in selected }

        for (group in groups) {

            val mainVisibility =
                if (editMode) View.VISIBLE else View.GONE

            findInGroup<View>(

                group,
                R.id.c_p_sort
            )?.visibility =
                if (editMode && groups.indexOf(group) == 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<View>(
                group,
                R.id.c_p_add_all_list
            )?.visibility =
                if (editMode && groups.indexOf(group) == 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<TextView>(
                group,
                R.id.c_p_edit
            )?.apply {
                visibility = mainVisibility
                text = if (group.placeEditExpanded) "<" else "v"

                textSize = 20f
                gravity = Gravity.CENTER
                includeFontPadding = false
            }

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
            )?.visibility =
                if (placeVisibility == View.VISIBLE &&
                    groups.size > 1 &&
                    !isPlaceFullySelected(group)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<View>(
                group,
                R.id.c_p_add_list
            )?.visibility =
                if (placeVisibility == View.VISIBLE && !globalSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            for (category in group.categories) {

                findInCategory<View>(
                    category,
                    R.id.c_c_sort
                )?.visibility =
                    if (editMode && group.categories.indexOf(category) == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                findInCategory<TextView>(
                    category,
                    R.id.c_c_edit
                )?.apply {
                    visibility = mainVisibility
                    text = if (category.categoryEditExpanded) "<" else "v"

                    textSize = 20f
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }

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
                )?.visibility =
                    if (categoryVisibility == View.VISIBLE &&
                        group.categories.size > 1 &&
                        !isCategoryFullySelected(group, category) &&
                        !isPlaceFullySelected(group)) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                findInCategory<View>(
                    category,
                    R.id.c_c_add_list
                )?.visibility =
                    if (categoryVisibility == View.VISIBLE &&
                        !isPlaceFullySelected(group)) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                for (item in category.items) {

                    findInItem<TextView>(
                        item,
                        R.id.c_i_edit
                    )?.apply {
                        visibility = mainVisibility
                        text = if (item.itemEditExpanded) "<" else "v"

                        textSize = 20f
                        gravity = Gravity.CENTER
                        includeFontPadding = false
                    }

                    findInItem<View>(
                        item,
                        R.id.c_i_sort
                    )?.visibility =
                        if (editMode && category.items.indexOf(item) == 0) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    val itemVisibility =
                        if (editMode && item.itemEditExpanded) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    findInItem<View>(
                        item,
                        R.id.c_i_add
                    )?.visibility = itemVisibility

                    val itemSelected = itemKey(group, category, item) in selected

                    findInItem<View>(
                        item,
                        R.id.c_i_remove
                    )?.visibility =
                        if (itemVisibility == View.VISIBLE &&
                            category.items.size > 1 &&
                            !itemSelected &&
                            !isCategoryFullySelected(group, category) &&
                            !isPlaceFullySelected(group)) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    findInItem<View>(
                        item,
                        R.id.c_i_add_list
                    )?.visibility =
                        if (itemVisibility == View.VISIBLE &&
                            !isCategoryFullySelected(group, category) &&
                            !isPlaceFullySelected(group)) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    for (brand in item.brands) {

                        findInBrand<View>(
                            brand.root,
                            brand.idMap,
                            R.id.c_i_b_add
                        )?.visibility = mainVisibility

                        findInBrand<View>(
                            brand.root,
                            brand.idMap,
                            R.id.c_i_b_remove
                        )?.visibility =
                            if (editMode && item.brands.size > 1) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
            }
        }
    }




    // EDIT NAME INLINE
    private fun editName(
        textView: TextView,
        hint: String,
        savedName: String,
        keyboardType: Int = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE,

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

            inputType = keyboardType

            background = textView.background
            gravity = textView.gravity

            setSingleLine(false)
            setHorizontallyScrolling(false)
        }

        parent.removeViewAt(index)
        parent.addView(input, index, params)

        input.requestFocus()

        input.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

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

            override fun afterTextChanged(s: Editable?) {}
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
        structureChanged = false

        for (category in group.categories) {

            category.categoryChanged = false

            for (item in category.items) {

                item.itemChanged = false

                for (brand in item.brands) {

                    brand.brandChanged = false
                    brand.nameChanged = false
                    brand.weightChanged = false
                    brand.quantityChanged = false
                }
            }
        }
    }

    // UPDATE NAME COLORS
    private fun updateAllNameColors() {

        for (group in groups) {
            updateNameColors(group)
        }
    }

    private fun updateNameColors(group: GroupUi) {

        val placeLocked = isPlaceLocked(group)

        group.placeName.setTextColor(
            if (placeLocked) muted
            else if (!editMode || group.placeChanged) white
            else muted
        )

        for (category in group.categories) {

            val categoryLocked = isCategoryLocked(group, category)

            category.categoryName.setTextColor(
                if (categoryLocked) muted
                else if (!editMode || category.categoryChanged) white
                else muted
            )

            for (item in category.items) {

                val itemLocked = isItemLocked(group, category, item)

                item.itemName.setTextColor(
                    if (itemLocked) muted
                    else if (!editMode || item.itemChanged) white
                    else muted
                )

                for (brand in item.brands) {

                    val brandColor = if (itemLocked || !editMode) {
                        muted
                    } else {
                        white
                    }

                    brand.brandName.setTextColor(
                        if (!itemLocked && editMode && brand.brandChanged) muted
                        else brandColor
                    )

                    brand.nameName.setTextColor(
                        if (!itemLocked && editMode && brand.nameChanged) muted
                        else brandColor
                    )

                    brand.weightName.setTextColor(
                        if (!itemLocked && editMode && brand.weightChanged)
                            muted
                        else brandColor
                    )

                    brand.quantityName.setTextColor(
                        if (!itemLocked && editMode && brand.quantityChanged)
                            muted
                        else brandColor
                    )
                }
            }
        }
    }

    // GET CURRENT STRUCTURE
    private fun currentStructure(): List<Any> {

        val structure = mutableListOf<Any>()

        for (group in groups) {

            structure.add(group)

            for (category in group.categories) {

                structure.add(category)

                for (item in category.items) {

                    structure.add(item)

                    for (brand in item.brands) {
                        structure.add(brand)
                    }
                }
            }
        }

        return structure
    }

    // CHECK STRUCTURE CHANGES
    private fun hasStructureChanged(): Boolean {

        val current = currentStructure()

        if (current.size != originalStructure.size) {
            return true
        }

        return current.indices.any { index ->
            current[index] !== originalStructure[index]
        }
    }

    // CHECK CHANGES
    private fun hasChanges(): Boolean {

        return hasStructureChanged() || groups.any { group ->

            group.placeChanged ||
                    group.categories.any { category ->

                        category.categoryChanged ||
                                category.items.any { item ->

                                    item.itemChanged ||
                                            item.brands.any { brand ->

                                                brand.brandChanged ||
                                                        brand.nameChanged ||
                                                        brand.weightChanged ||
                                                        brand.quantityChanged
                                            }
                                }
                    }
        }
    }

    // REQUIRED NAMES AND DUPLICATE VALIDATION
    private fun allRequiredNamesFilled(): Boolean {

        fun normalize(text: CharSequence): String {
            return text.toString()
                .trim()
                .lowercase(Locale.ROOT)
        }

        if (groups.any {
                normalize(it.placeName.text).isEmpty()
            }) {
            return false
        }

        val placeNames = groups.map {
            normalize(it.placeName.text)
        }

        if (placeNames.size != placeNames.toSet().size) {
            return false
        }

        for (group in groups) {

            if (group.categories.isEmpty()) {
                return false
            }

            if (group.categories.any { category ->

                    normalize(category.categoryName.text).isEmpty() ||
                            category.items.isEmpty() ||
                            category.items.any { item ->
                                normalize(item.itemName.text).isEmpty()
                            }

                }) {
                return false
            }

            val categoryNames = group.categories.map {
                normalize(it.categoryName.text)
            }

            if (categoryNames.size != categoryNames.toSet().size) {
                return false
            }

            // BRAND + NAME + WEIGHT MUST NOT ALL MATCH
            for (category in group.categories) {

                for (item in category.items) {

                    val brandCombinations = item.brands.map { brand ->

                        listOf(
                            normalize(brand.brandName.text),
                            normalize(brand.nameName.text),
                            normalize(brand.weightName.text)
                        )
                    }

                    // Ignore completely empty rows when checking duplicates.
                    val filledCombinations = brandCombinations.filter { values
                        ->
                        values.any { it.isNotEmpty() }
                    }

                    if (
                        filledCombinations.size !=
                        filledCombinations.toSet().size
                    ) {
                        return false
                    }
                }
            }
        }

        return true
    }

    // UPDATE SAVE BUTTON
    private fun updateSaveButton() {

        val allNamesFilled = allRequiredNamesFilled()

        val canSave =
            editMode &&
                    (hasChanges() || hasSelectionChanges()) &&
                    allNamesFilled

        editButton.text =
            if (editMode) "Save" else "Edit"

        editButton.isEnabled =
            !editMode || allNamesFilled

        editButton.backgroundTintList =
            ColorStateList.valueOf(
                if (canSave) Color.rgb(255, 128, 0) else gray
            )
    }

    // REMOVE PLACE
    private fun removeGroup(group: GroupUi) {

        if (groups.size <= 1) return

        groups.remove(group)
        scroll.removeView(group.root)

        structureChanged = true

        updateGroupSpacing()
        updateAllExpansionVisibility()
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
    }

    // DRAG PAYLOAD
    private data class DragPayload(
        val draggedView: View,
        val parent: ViewGroup
    )


    // MAKE ENTRY DRAGGABLE
    private fun makeEntryDraggable(
        nameView: View,
        entryRoot: View,
        container: ViewGroup,
        onLongPressCollapse: () -> Unit
    ) {

        nameView.setOnLongClickListener {

            if (!editMode) {

                return@setOnLongClickListener false
            }

            onLongPressCollapse()

            val payload = DragPayload(
                draggedView = entryRoot,
                parent = container
            )

            val clipData = ClipData.newPlainText(
                "shopping_list_entry",
                "move"
            )

            nameView.startDragAndDrop(
                clipData,
                View.DragShadowBuilder(nameView),
                payload,
                0
            )
        }
    }


    // ENABLE LIST DRAGGING


    private fun <T> enableListDragging(
        parent: ViewGroup,
        entries: MutableList<T>,
        rootOf: (T) -> View,
        onReordered: () -> Unit
    ) {
        parent.setOnDragListener { _, event ->

            when (event.action) {

                android.view.DragEvent.ACTION_DRAG_STARTED -> {
                    val payload = event.localState as? DragPayload
                    payload?.parent === parent
                }

                android.view.DragEvent.ACTION_DROP -> {

                    val payload = event.localState as? DragPayload

                    if (payload == null || payload.parent !== parent) {
                        return@setOnDragListener false
                    }

                    val draggedView = payload.draggedView
                    val currentViews = entries.map { rootOf(it) }

                    if (!currentViews.contains(draggedView)) {
                        return@setOnDragListener false
                    }

                    val remainingViews = currentViews.filter {
                        it !== draggedView
                    }

                    val firstEntryTop = remainingViews.firstOrNull()?.top ?: 0
                    val dropY = event.y.coerceAtLeast(firstEntryTop.toFloat())

                    val insertIndex = remainingViews.indexOfFirst { child ->
                        dropY < child.top + child.height / 2f
                    }.let { index ->
                        if (index < 0) remainingViews.size else index
                    }

                    val orderedViews = remainingViews.toMutableList().apply {
                        add(insertIndex, draggedView)
                    }

                    val orderedEntries = orderedViews.mapNotNull { view ->
                        entries.firstOrNull { rootOf(it) === view }
                    }

                    if (orderedEntries.size == entries.size) {

                        val changed = entries.indices.any { index ->
                            entries[index] !== orderedEntries[index]
                        }

                        if (changed) {

                            entries.clear()
                            entries.addAll(orderedEntries)

                            reorderViews(parent, orderedViews)

                            onReordered()
                            updateSortToggles()
                        }
                    }

                    true
                }

                android.view.DragEvent.ACTION_DRAG_ENDED -> true

                else -> true
            }
        }
    }


    // SORT VIEWS
// REORDER VIEWS
    private fun reorderViews(
        parent: ViewGroup,
        orderedViews: List<View>
    ) {

        val existingViews = orderedViews.filter {
            parent.indexOfChild(it) >= 0
        }

        if (existingViews.isEmpty()) return

        // Save the original positions occupied by draggable entries
        val originalPositions = existingViews
            .map { parent.indexOfChild(it) }
            .sorted()

        // Save each view's layout parameters
        val savedParams = existingViews.associateWith {
            it.layoutParams
        }

        // Remove only draggable entries
        existingViews.forEach {
            parent.removeView(it)
        }

        // Put reordered entries into the same original positions
        for ((index, view) in orderedViews.withIndex()) {

            if (view !in existingViews) continue

            parent.addView(
                view,
                originalPositions[index],
                savedParams[view]
            )
        }
    }

    // SORT PLACES
    private fun sortGroups() {

        val sortedGroups = groups.sortedWith(
            compareBy<GroupUi> {
                if (it.placeName.text.toString().isBlank()) 0 else 1
            }.thenBy {
                it.placeName.text.toString()
                    .trim()
                    .lowercase(Locale.ROOT)
            }
        )

        val changed = sortedGroups.indices.any {
            sortedGroups[it] !== groups[it]
        }

        if (!changed) {
            updateSortToggles()
            return
        }

        groups.clear()
        groups.addAll(sortedGroups)

        reorderViews(
            scroll,
            groups.map { it.root }
        )

        structureChanged = true

        updateGroupSpacing()
        updateAllExpansionVisibility()
        updateEditVisibility()
        updateSaveButton()
        updateSortToggles()
    }

    // SORT CATEGORIES
    private fun sortCategories(group: GroupUi) {

        val sortedCategories = group.categories.sortedWith(
            compareBy<CategoryUi> {
                if (it.categoryName.text.toString().isBlank()) 0 else 1
            }.thenBy {
                it.categoryName.text.toString()
                    .trim()
                    .lowercase(Locale.ROOT)
            }
        )

        val changed = sortedCategories.indices.any {
            sortedCategories[it] !== group.categories[it]
        }

        if (!changed) {
            updateSortToggles()
            return
        }

        group.categories.clear()
        group.categories.addAll(sortedCategories)

        reorderViews(
            group.categoryContainer,
            group.categories.map { it.root }
        )

        structureChanged = true

        updateAllExpansionVisibility()
        updateEditVisibility()
        updateSaveButton()
        updateSortToggles()
    }

    // SORT ITEMS
    private fun sortItems(category: CategoryUi) {

        val sortedItems = category.items.sortedWith(
            compareBy<ItemUi> {
                if (it.itemName.text.toString().isBlank()) 0 else 1
            }.thenBy {
                it.itemName.text.toString()
                    .trim()
                    .lowercase(Locale.ROOT)
            }
        )

        val changed = sortedItems.indices.any {
            sortedItems[it] !== category.items[it]

        }

        if (!changed) {
            updateSortToggles()
            return
        }

        category.items.clear()
        category.items.addAll(sortedItems)

        reorderViews(
            category.itemContainer,
            category.items.map { it.root }
        )

        structureChanged = true

        updateAllExpansionVisibility()
        updateEditVisibility()
        updateSaveButton()
        updateSortToggles()
    }

    // REMOVE CATEGORY
    private fun removeCategory(group: GroupUi, category: CategoryUi) {

        if (group.categories.size <= 1) return

        group.categories.remove(category)
        group.categoryContainer.removeView(category.root)

        structureChanged = true

        updateAllExpansionVisibility()
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
    }

    // REMOVE ITEM
    private fun removeItem(category: CategoryUi, item: ItemUi) {

        if (category.items.size <= 1) return

        category.items.remove(item)
        category.itemContainer.removeView(item.root)

        structureChanged = true

        updateAllExpansionVisibility()
        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()

    }

    // REMOVE BRAND
    private fun removeBrand(item: ItemUi, brand: BrandUi) {

        if (item.brands.size <= 1) return

        item.brands.remove(brand)
        item.brandContainer.removeView(brand.root)

        structureChanged = true

        updateEditVisibility()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
    }

    // UPDATE REMOVE BUTTON VISIBILITY
    private fun updateRemoveVisibility(group: GroupUi) {
        updateEditVisibility()
    }

    private fun updateRemoveVisibilityForCategory(category: CategoryUi) {
        updateEditVisibility()
    }

    private fun updateRemoveVisibilityForItem(item: ItemUi) {
        updateEditVisibility()
    }

// SHOPPING LIST SELECTION

    private fun placeKey(group: GroupUi): String {
        return ShoppingListStore.path("P", group.placeName.text.toString())
    }

    private fun categoryKey(group: GroupUi, category: CategoryUi): String {
        return ShoppingListStore.path(
            "C",
            group.placeName.text.toString(),
            category.categoryName.text.toString()
        )
    }

    private fun itemKey(
        group: GroupUi,
        category: CategoryUi,
        item: ItemUi
    ): String {
        return ShoppingListStore.path(
            "I",
            group.placeName.text.toString(),
            category.categoryName.text.toString(),
            item.itemName.text.toString()
        )
    }

    private fun allItemKeysInCategory(
        group: GroupUi,
        category: CategoryUi
    ): List<String> {
        return category.items.map { item -> itemKey(group, category, item) }
    }

    private fun allItemKeysInPlace(group: GroupUi): List<String> {
        return group.categories.flatMap { category ->
            allItemKeysInCategory(group, category)
        }
    }

    private fun allItemKeysInShoppingList(): List<String> {
        return groups.flatMap { group -> allItemKeysInPlace(group) }
    }

    private fun clearAggregateSelectionKeys(group: GroupUi) {
        val keys = mutableListOf(placeKey(group))

        for (category in group.categories) {
            keys.add(categoryKey(group, category))
        }

        setWorkingSelected(keys, false)
    }

    private fun clearAllAggregateSelectionKeys() {
        for (group in groups) {
            clearAggregateSelectionKeys(group)
        }
    }

    private fun isPlaceFullySelected(group: GroupUi): Boolean {
        val keys = allItemKeysInPlace(group)
        if (keys.isEmpty()) return false
        val selected = workingSelections
        return keys.all { it in selected }
    }

    private fun isCategoryFullySelected(
        group: GroupUi,
        category: CategoryUi
    ): Boolean {
        val keys = allItemKeysInCategory(group, category)
        if (keys.isEmpty()) return false
        val selected = workingSelections
        return keys.all { it in selected }
    }

    private fun isPlaceLocked(group: GroupUi): Boolean {
        return isPlaceFullySelected(group)
    }

    private fun isCategoryLocked(
        group: GroupUi,
        category: CategoryUi
    ): Boolean {
        return isPlaceFullySelected(group) ||
                isCategoryFullySelected(group, category)
    }

    private fun isItemLocked(
        group: GroupUi,
        category: CategoryUi,
        item: ItemUi
    ): Boolean {
        val selected = workingSelections
        return isPlaceFullySelected(group) ||
                isCategoryFullySelected(group, category) ||
                itemKey(group, category, item) in selected
    }

    private fun refreshShoppingData(persist: Boolean = false) {

        val selected = workingSelections
        val result = org.json.JSONArray()

        for (group in groups) {
            val place = group.placeName.text.toString()
            if (place.isBlank()) continue

            val placeSelected = isPlaceFullySelected(group)
            val placeObject = org.json.JSONObject()
            placeObject.put("place", place)
            val categoriesArray = org.json.JSONArray()
            var hasSelectedContent = false

            for (category in group.categories) {
                val categoryName = category.categoryName.text.toString()
                if (categoryName.isBlank()) continue

                val categorySelected = isCategoryFullySelected(group, category)
                val categoryObject = org.json.JSONObject()
                categoryObject.put("category", categoryName)
                val itemsArray = org.json.JSONArray()

                for (item in category.items) {
                    val itemName = item.itemName.text.toString()
                    if (itemName.isBlank()) continue

                    val itemSelected = itemKey(group, category, item) in selected
                    if (!itemSelected) continue

                    val itemObject = org.json.JSONObject()
                    itemObject.put("item", itemName)
                    val brandsArray = org.json.JSONArray()

                    for (brand in item.brands) {
                        if (
                            brand.brandName.text.isBlank() &&
                            brand.nameName.text.isBlank() &&
                            brand.weightName.text.isBlank() &&
                            brand.quantityName.text.isBlank()
                        ) continue

                        val brandObject = org.json.JSONObject()
                        brandObject.put("brand", brand.brandName.text.toString())
                        brandObject.put("name", brand.nameName.text.toString())
                        brandObject.put("weight", brand.weightName.text.toString())
                        brandObject.put("quantity", brand.quantityName.text.toString())
                        brandsArray.put(brandObject)
                    }

                    itemObject.put("brands", brandsArray)
                    itemsArray.put(itemObject)
                    hasSelectedContent = true
                }

                if (categorySelected || itemsArray.length() > 0) {
                    categoryObject.put("items", itemsArray)
                    categoriesArray.put(categoryObject)
                    hasSelectedContent = true
                }
            }

            if (placeSelected || hasSelectedContent) {
                placeObject.put("categories", categoriesArray)
                result.put(placeObject)
            }
        }
        if (persist) {
            ShoppingListStore.saveShoppingData(this, result)
        }
    }

    private fun updateShoppingButtonColors() {

        finishActiveNameEdit()
        val selected = workingSelections
        val dark = Color.rgb(29, 27, 32)

        fun setColor(view: View?, isSelected: Boolean, isDisabled: Boolean = false) {
            val button = view as? AppCompatButton ?: return
            button.isEnabled = !isDisabled

            button.backgroundTintList = ColorStateList.valueOf(
                if (isSelected) muted else gray
            )

            button.setTextColor(
                if (isSelected) gray else dark
            )
        }

        val allKeys = allItemKeysInShoppingList()
        val globalSelected = allKeys.isNotEmpty() && allKeys.all { it in selected }

        for (group in groups) {
            val placeSelected = isPlaceFullySelected(group)
            val placeDisabled = globalSelected

            val placeButton = findInGroup<View>(group, R.id.c_p_add_list)
            setColor(placeButton, placeSelected, placeDisabled)

            val placeRemove = findInGroup<View>(group, R.id.c_p_remove)
            if (placeSelected) placeRemove?.visibility = View.GONE

            setColor(
                findInGroup(group, R.id.c_p_add_all_list),
                globalSelected
            )

            for (category in group.categories) {
                val categorySelected = isCategoryFullySelected(group, category)
                val categoryDisabled = placeSelected
                setColor(
                    findInCategory(category, R.id.c_c_add_list),
                    categorySelected,
                    categoryDisabled
                )

                val categoryRemove = findInCategory<View>(category, R.id.c_c_remove)
                if (categorySelected || categoryDisabled) {
                    categoryRemove?.visibility = View.GONE
                }

                for (item in category.items) {
                    val itemSelected = itemKey(group, category, item) in selected
                    val itemDisabled = placeSelected || categorySelected
                    setColor(
                        findInItem(item, R.id.c_i_add_list),
                        itemSelected,
                        itemDisabled
                    )

                    val itemRemove = findInItem<View>(item, R.id.c_i_remove)
                    if (itemSelected || itemDisabled) {
                        itemRemove?.visibility = View.GONE
                    }
                }
            }
        }

        updateEditVisibility()

// Hide disabled Add to List buttons without forcing enabled buttons visible.
        for (group in groups) {
            val placeSelected = isPlaceFullySelected(group)

            if (globalSelected) {
                findInGroup<View>(group, R.id.c_p_add_list)?.visibility = View.GONE
            }

            if (placeSelected) {
                findInGroup<View>(group, R.id.c_p_remove)?.visibility = View.GONE
            }

            for (category in group.categories) {
                val categorySelected = isCategoryFullySelected(group, category)

                if (placeSelected) {
                    findInCategory<View>(category, R.id.c_c_add_list)?.visibility = View.GONE
                }

                if (categorySelected || placeSelected) {
                    findInCategory<View>(category, R.id.c_c_remove)?.visibility = View.GONE
                }

                for (item in category.items) {
                    val itemSelected = itemKey(group, category, item) in selected

                    if (placeSelected || categorySelected) {
                        findInItem<View>(item, R.id.c_i_add_list)?.visibility = View.GONE
                    }

                    if (itemSelected || categorySelected || placeSelected) {
                        findInItem<View>(item, R.id.c_i_remove)?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun isItemLockedForBrand(item: ItemUi): Boolean {

        val group = groups.firstOrNull { parentGroup ->
            parentGroup.categories.any { category ->
                category.items.contains(item)
            }
        } ?: return false

        val category = group.categories.firstOrNull {
            it.items.contains(item)

        } ?: return false

        return isItemLocked(group, category, item)
    }

    private fun finishActiveNameEdit() {

        val focusedView = currentFocus

        if (focusedView is EditText) {

            val token = focusedView.windowToken

            focusedView.clearFocus()

            val keyboard = getSystemService(
                INPUT_METHOD_SERVICE
            ) as android.view.inputmethod.InputMethodManager

            keyboard.hideSoftInputFromWindow(token, 0)
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()

        android.util.Log.d(
            "SELECTION_DEBUG",
            "ONSTART BEFORE: ${workingSelections.map { ShoppingListStore.parts(it) }}"
        )

        workingSelections.clear()
        workingSelections.addAll(
            ShoppingListStore.getSelections(this)
        )

        savedSelections = workingSelections.toMutableSet()

        android.util.Log.d(
            "SELECTION_DEBUG",
            "ONSTART AFTER: ${workingSelections.map { ShoppingListStore.parts(it) }}"
        )

        updateShoppingButtonColors()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
    }

    override fun onResume() {
        super.onResume()

        updateShoppingButtonColors()
        updateAllNameColors()
        updateSaveButton()
        updateSortToggles()
    }

    private fun isAlphabetical(names: List<String>): Boolean {

        val sortedNames = names.sortedWith(
            compareBy<String> {
                if (it.isBlank()) 0 else 1
            }.thenBy {
                it.trim().lowercase(Locale.ROOT)
            }
        )

        return names == sortedNames
    }

    private fun updateSortToggles() {

        // PLACES
        groups.firstOrNull()?.let { firstGroup ->

            findInGroup<androidx.appcompat.widget.SwitchCompat>(
                firstGroup,
                R.id.c_p_sort
            )?.isChecked = isAlphabetical(
                groups.map { it.placeName.text.toString() }
            )
        }

        // CATEGORIES
        for (group in groups) {

            group.categories.firstOrNull()?.let { firstCategory ->

                findInCategory<androidx.appcompat.widget.SwitchCompat>(
                    firstCategory,
                    R.id.c_c_sort
                )?.isChecked = isAlphabetical(
                    group.categories.map {
                        it.categoryName.text.toString()
                    }
                )
            }

            // ITEMS
            for (category in group.categories) {

                category.items.firstOrNull()?.let { firstItem ->

                    findInItem<androidx.appcompat.widget.SwitchCompat>(
                        firstItem,
                        R.id.c_i_sort
                    )?.isChecked = isAlphabetical(
                        category.items.map {
                            it.itemName.text.toString()
                        }
                    )
                }
            }
        }
    }


}