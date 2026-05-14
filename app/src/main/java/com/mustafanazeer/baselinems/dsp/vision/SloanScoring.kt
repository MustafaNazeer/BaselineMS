package com.mustafanazeer.baselinems.dsp.vision

val SLOAN_LETTERS: List<Char> = listOf('C', 'D', 'H', 'K', 'N', 'O', 'R', 'S', 'V', 'Z')

data class LetterResponse(
    val shown: Char,
    val tapped: Char?,
    val contrast: SloanScoring.ContrastLevel
)

data class SloanScore(
    val totalCorrect: Int,
    val correctAt100Pct: Int,
    val correctAt2Pt5Pct: Int,
    val correctAt1Pt25Pct: Int
)

object SloanScoring {
    enum class ContrastLevel(val percent: Double) {
        C100(100.0),
        C2Pt5(2.5),
        C1Pt25(1.25)
    }

    fun compute(responses: List<LetterResponse>): SloanScore {
        var c100 = 0
        var c2pt5 = 0
        var c1pt25 = 0

        for (r in responses) {
            val correct = r.tapped != null && r.tapped.equals(r.shown, ignoreCase = true)
            if (!correct) continue
            when (r.contrast) {
                ContrastLevel.C100 -> c100++
                ContrastLevel.C2Pt5 -> c2pt5++
                ContrastLevel.C1Pt25 -> c1pt25++
            }
        }

        return SloanScore(
            totalCorrect = c100 + c2pt5 + c1pt25,
            correctAt100Pct = c100,
            correctAt2Pt5Pct = c2pt5,
            correctAt1Pt25Pct = c1pt25
        )
    }
}
