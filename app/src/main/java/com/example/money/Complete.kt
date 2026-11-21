
package com.example.shoppinglist

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
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

    // NEW CODE //
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
    // NEW CODE //

    private data class CategoryUi(
        val root: LinearLayout,
        val idMap: SparseIntArray,

        val categoryName: TextView,
        val itemContainer: LinearLayout,

        var categoryExpanded: Boolean = false,
        var categoryEditExpanded: Boolean = false,

        var savedCategoryName: String = "",
        var categoryChanged: Boolean = false,

        // NEW CODE //
        val items: MutableList<ItemUi> = mutableListOf()
        // NEW CODE //
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

            } else {

                if (hasChanges() && allRequiredNamesFilled()) {
                    persistAllGroups()
                    updateSavedNames()
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
            }
        }

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

        // NEW CODE //
        val itemContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        itemContainer.addView(itemSection)
        // NEW CODE //

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

        // NEW CODE //
        val firstItem = setupItem(
            root = firstItemSection,
            idMap = idMap
        )

        category.items.add(firstItem)
        // NEW CODE //

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

        findInCategory<View>(
            category,
            R.id.c_c_add
        )?.setOnClickListener {
            addCategory(group, category)
        }

        return category
    }

    // NEW CODE //
    // SETUP ITEM
    private fun setupItem(
        root: LinearLayout,
        idMap: SparseIntArray
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
            idMap = idMap
        )

        item.brands.add(firstBrand)

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
                editName(
                    item.itemName,
                    "[Item]",
                    item.savedItemName
                ) { changed ->

                    item.itemChanged = changed

                    updateAllNameColors()
                    updateSaveButton()
                }
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

        return item
    }

    // SETUP BRAND ROW
    private fun setupBrand(
        root: TableRow,
        idMap: SparseIntArray
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
            if (editMode) {
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
            if (editMode) {
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
            if (editMode) {
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
            if (editMode) {
                editBrandField(
                    brand,
                    brand.quantityName,
                    "[Quantity]",
                    brand.savedQuantityName,
                    3
                )
            }
        }

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

        return brand
    }

    // EDIT BRAND FIELD
    private fun editBrandField(
        brand: BrandUi,
        textView: TextView,
        hint: String,
        savedName: String,
        field: Int
    ) {

        editName(textView, hint, savedName) { changed ->

            when (field) {
                0 -> brand.brandChanged = changed
                1 -> brand.nameChanged = changed
                2 -> brand.weightChanged = changed
                3 -> brand.quantityChanged = changed
            }

            updateAllNameColors()
            updateSaveButton()
        }
    }
    // NEW CODE //

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

        categorySection.findViewById<View>(
            R.id.edit_category_bar
        )?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

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

        // NEW CODE //
        for (item in newCategory.items) {
            clearItem(item)
        }
        // NEW CODE //

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

    // NEW CODE //
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
            idMap = idMap
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
            idMap = idMap
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
    // NEW CODE //

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

        newGroup.findViewById<View>(
            R.id.edit_place_bar
        )?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

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

        params.topMargin = dp(20)

        (newGroup.parent as? ViewGroup)?.removeView(newGroup)

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

            // NEW CODE //
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
            // NEW CODE //

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

                // NEW CODE //
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
                // NEW CODE //
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

                // NEW CODE //
                for (item in category.items) {

                    item.savedItemName = item.itemName.text.toString()

                    for (brand in item.brands) {

                        brand.savedBrandName = brand.brandName.text.toString()
                        brand.savedNameName = brand.nameName.text.toString()
                        brand.savedWeightName = brand.weightName.text.toString()
                        brand.savedQuantityName = brand.quantityName.text.toString()
                    }
                }
                // NEW CODE //
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

            // NEW CODE //
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
            // NEW CODE //
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

    // NEW CODE //
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
    // NEW CODE //

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

            // NEW CODE //
            for (item in category.items) {

                findInItem<TextView>(
                    item,
                    R.id.a_i_expand
                )?.apply {

                    text = if (item.itemExpanded) "v" else ">"

                    textSize = 20f
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }
            }
            // NEW CODE //
        }
    }

    // UPDATE ONE GROUP'S EXPANSION
    private fun updateGroupExpansion(group: GroupUi) {

        for (category in group.categories) {

            category.root.visibility =
                if (group.placeExpanded) View.VISIBLE else View.GONE

            // NEW CODE //
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
            }
            // NEW CODE //
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

        for (group in groups) {

            val mainVisibility =
                if (editMode) View.VISIBLE else View.GONE

            findInGroup<View>(
                group,
                R.id.c_p_sort
            )?.visibility =
                if (editMode && group.isOriginal) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            findInGroup<View>(
                group,
                R.id.c_p_edit
            )?.visibility = mainVisibility

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

            for ((index, category) in group.categories.withIndex()) {

                findInCategory<View>(
                    category,
                    R.id.c_c_sort
                )?.visibility =
                    if (editMode && index == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                findInCategory<View>(
                    category,
                    R.id.c_c_edit
                )?.visibility = mainVisibility

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

                // NEW CODE //
                for ((itemIndex, item) in category.items.withIndex()) {

                    findInItem<View>(
                        item,
                        R.id.c_i_edit
                    )?.visibility = mainVisibility

                    findInItem<View>(
                        item,
                        R.id.c_i_sort
                    )?.visibility = mainVisibility

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

                    findInItem<View>(
                        item,
                        R.id.c_i_remove
                    )?.visibility = itemVisibility

                    findInItem<View>(
                        item,
                        R.id.c_i_add_list
                    )?.visibility = itemVisibility

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
                        )?.visibility = mainVisibility
                    }
                }
                // NEW CODE //
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

        input.addTextChangedListener(object : TextWatcher {

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

        for (category in group.categories) {

            category.categoryChanged = false

            // NEW CODE //
            for (item in category.items) {

                item.itemChanged = false

                for (brand in item.brands) {

                    brand.brandChanged = false
                    brand.nameChanged = false
                    brand.weightChanged = false
                    brand.quantityChanged = false
                }
            }
            // NEW CODE //
        }
    }

    // UPDATE NAME COLORS
    private fun updateAllNameColors() {

        for (group in groups) {
            updateNameColors(group)
        }
    }

    private fun updateNameColors(group: GroupUi) {

        group.placeName.setTextColor(
            if (!editMode || group.placeChanged) white else muted
        )

        for (category in group.categories) {

            if (!editMode) {

                category.categoryName.setTextColor(white)

                // NEW CODE //
                for (item in category.items) {

                    item.itemName.setTextColor(white)

                    for (brand in item.brands) {
                        brand.brandName.setTextColor(muted)
                        brand.nameName.setTextColor(muted)
                        brand.weightName.setTextColor(muted)
                        brand.quantityName.setTextColor(muted)
                    }
                }
                // NEW CODE //

                continue
            }

            category.categoryName.setTextColor(
                if (category.categoryChanged) white else muted
            )

            // NEW CODE //
            for (item in category.items) {

                item.itemName.setTextColor(
                    if (item.itemChanged) white else muted
                )

                for (brand in item.brands) {

                    brand.brandName.setTextColor(
                        if (brand.brandChanged) muted else white
                    )

                    brand.nameName.setTextColor(
                        if (brand.nameChanged) muted else white
                    )

                    brand.weightName.setTextColor(
                        if (brand.weightChanged) muted else white
                    )

                    brand.quantityName.setTextColor(
                        if (brand.quantityChanged) muted else white
                    )
                }
            }
            // NEW CODE //
        }
    }

    // CHECK CHANGES
    private fun hasChanges(): Boolean {

        return groups.any { group ->

            group.placeChanged ||
                    group.categories.any { category ->

                        category.categoryChanged ||
                                // NEW CODE //
                                category.items.any { item ->

                                    item.itemChanged ||
                                            item.brands.any { brand ->

                                                brand.brandChanged ||
                                                        brand.nameChanged ||
                                                        brand.weightChanged ||
                                                        brand.quantityChanged
                                            }
                                }
                        // NEW CODE //
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
