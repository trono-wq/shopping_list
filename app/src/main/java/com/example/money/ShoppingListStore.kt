
package com.example.shoppinglist

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ShoppingListStore {

    private const val PREFS = "shopping_selection"
    private const val KEY = "entries"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun encode(value: String): String =
        android.util.Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )

    private fun decode(value: String): String =
        String(
            android.util.Base64.decode(
                value,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            ),
            Charsets.UTF_8
        )

    fun path(type: String, place: String, category: String = "", item: String = ""): String {
        return listOf(type, place, category, item)
            .joinToString("|") { encode(it) }
    }

    fun getSelections(context: Context): MutableSet<String> {
        return preferences(context).getStringSet(KEY, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
    }

    fun saveSelections(context: Context, selections: Set<String>) {
        preferences(context).edit()
            .putStringSet(KEY, selections.toSet())
            .apply()
    }

    fun isSelected(context: Context, key: String): Boolean {
        return getSelections(context).contains(key)
    }

    fun toggle(context: Context, key: String) {
        val selections = getSelections(context)

        if (!selections.add(key)) {
            selections.remove(key)
        }

        saveSelections(context, selections)
    }

    fun setSelected(
        context: Context,
        keys: Collection<String>,
        selected: Boolean
    ) {
        val selections = getSelections(context)

        if (selected) {
            selections.addAll(keys)
        } else {
            selections.removeAll(keys.toSet())
        }

        saveSelections(context, selections)
    }

    fun clear(context: Context) {
        preferences(context).edit().remove(KEY).apply()
    }

    fun type(key: String): String =
        decode(key.split("|")[0])

    fun parts(key: String): List<String> =
        key.split("|").map { decode(it) }

    fun saveShoppingData(context: Context, data: JSONArray) {
        preferences(context).edit()
            .putString("shopping_data", data.toString())
            .apply()
    }

    fun getShoppingData(context: Context): JSONArray {
        return try {
            JSONArray(
                preferences(context).getString("shopping_data", "[]")
            )
        } catch (_: Exception) {
            JSONArray()
        }
    }
}
