package com.example.projektiop.activeHandshake.NFC

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val nfcKoinModule = module {
    viewModel { NFCVIewModel() }
}