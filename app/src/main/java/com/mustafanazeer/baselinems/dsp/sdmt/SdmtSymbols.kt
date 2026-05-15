package com.mustafanazeer.baselinems.dsp.sdmt

import com.mustafanazeer.baselinems.R

enum class SdmtSymbol(val drawableRes: Int) {
    SYMBOL_1(R.drawable.sdmt_symbol_1),
    SYMBOL_2(R.drawable.sdmt_symbol_2),
    SYMBOL_3(R.drawable.sdmt_symbol_3),
    SYMBOL_4(R.drawable.sdmt_symbol_4),
    SYMBOL_5(R.drawable.sdmt_symbol_5),
    SYMBOL_6(R.drawable.sdmt_symbol_6),
    SYMBOL_7(R.drawable.sdmt_symbol_7),
    SYMBOL_8(R.drawable.sdmt_symbol_8),
    SYMBOL_9(R.drawable.sdmt_symbol_9)
}

val SDMT_SYMBOLS: List<SdmtSymbol> = SdmtSymbol.entries.toList()

val SDMT_FIXED_KEY: Map<SdmtSymbol, Int> = SdmtSymbol.entries
    .mapIndexed { idx, sym -> sym to (idx + 1) }
    .toMap()
