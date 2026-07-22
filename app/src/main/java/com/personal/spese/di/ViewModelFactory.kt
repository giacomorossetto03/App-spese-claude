package com.personal.spese.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.personal.spese.SpeseApp

/** Factory minimale per iniettare dipendenze dall'AppContainer nei ViewModel. */
@Suppress("UNCHECKED_CAST")
fun <VM : ViewModel> viewModelFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as SpeseApp).container
