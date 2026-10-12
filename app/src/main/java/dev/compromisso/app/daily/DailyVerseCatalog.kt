package dev.compromisso.app.daily

import java.time.LocalDate

/** Curated short excerpts in classic Portuguese Biblical wording, stored fully offline.
 * Before a public release, recheck translation provenance and redistribution rights.
 * No reading history or personal data is recorded.
 */
data class DailyVerse(val reference: String, val text: String)

object DailyVerseCatalog {
    private val verses = listOf(
        DailyVerse("Salmos 119:105", "Lâmpada para os meus pés é tua palavra, e luz para o meu caminho."),
        DailyVerse("Salmos 46:1", "Deus é o nosso refúgio e fortaleza, socorro bem presente na angústia."),
        DailyVerse("Tiago 4:7", "Sujeitai-vos, pois, a Deus, resisti ao diabo, e ele fugirá de vós."),
        DailyVerse("Provérbios 4:23", "Sobre tudo o que se deve guardar, guarda o teu coração."),
        DailyVerse("Romanos 12:21", "Não te deixes vencer do mal, mas vence o mal com o bem."),
        DailyVerse("Mateus 26:41", "Vigiai e orai, para que não entreis em tentação."),
        DailyVerse("Filipenses 4:13", "Posso todas as coisas naquele que me fortalece."),
        DailyVerse("2 Timóteo 1:7", "Porque Deus não nos deu o espírito de temor, mas de fortaleza, e de amor, e de moderação."),
    )

    fun forDate(date: LocalDate): DailyVerse = verses[Math.floorMod(date.toEpochDay(), verses.size.toLong()).toInt()]
}
