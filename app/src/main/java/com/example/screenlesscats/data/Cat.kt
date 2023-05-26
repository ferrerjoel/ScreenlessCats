package com.example.screenlesscats.data

/**
 * Data class for cat data
 *
 * @property catId The id of the image that represents the cat
 * @property catName Name of the cat
 * @property catRarity Rarity of the cat
 */
data class Cat(
    val catId: Int,
    val catName: String,
    val catRarity: String,
)
