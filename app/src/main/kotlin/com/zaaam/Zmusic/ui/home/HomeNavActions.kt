package com.zaaam.Zmusic.ui.home

/**
 * Bundel callback navigasi untuk HomeScreen — supaya signature tidak kepanjangan.
 * Semua nullable + default null (sama persis dengan parameter lama).
 * Callback data (onSongClick/onSongLongClick/onRefresh/onLoadMore) TIDAK di sini —
 * tetap jadi parameter langsung karena bukan navigasi.
 */
data class HomeNavActions(
    val onAboutClick: (() -> Unit)? = null,
    val onDevPageClick: (() -> Unit)? = null,
    val onSearchClick: (() -> Unit)? = null,
    val onLibraryClick: (() -> Unit)? = null,
    val onMoodClick: (() -> Unit)? = null,
    val onStatsClick: (() -> Unit)? = null,
    val onSettingsClick: (() -> Unit)? = null,
    val onViewTrending: (() -> Unit)? = null,
    val onViewRecent: (() -> Unit)? = null,
    val onViewExplore: (() -> Unit)? = null
)
