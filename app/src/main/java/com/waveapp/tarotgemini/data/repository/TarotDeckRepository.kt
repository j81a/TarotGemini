package com.waveapp.tarotgemini.data.repository

import com.waveapp.tarotgemini.data.model.ArcanaType
import com.waveapp.tarotgemini.data.model.TarotCard

/**
 * Repositorio que provee acceso al mazo completo de cartas del tarot.
 * Por ahora usa datos hardcoded, pero se puede extender para cargar desde JSON o BD.
 */
class TarotDeckRepository {

    /**
     * Obtiene el mazo completo de 78 cartas del tarot.
     * Por ahora incluye solo los 22 Arcanos Mayores para pruebas iniciales.
     */
    fun getFullDeck(): List<TarotCard> {
        return getMajorArcana() // + getMinorArcana() cuando los agreguemos
    }

    /**
     * Los 22 Arcanos Mayores del tarot
     */
    private fun getMajorArcana(): List<TarotCard> {
        return listOf(
            TarotCard(
                id = 0,
                name = "El Loco",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_00_fool",
                uprightMeaning = "Nuevos comienzos, espontaneidad, fe en el futuro, aventura",
                reversedMeaning = "Imprudencia, riesgos innecesarios, falta de dirección"
            ),
            TarotCard(
                id = 1,
                name = "El Mago",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_01_magician",
                uprightMeaning = "Manifestación, poder personal, recursos disponibles, acción",
                reversedMeaning = "Manipulación, talentos desperdiciados, falta de energía"
            ),
            TarotCard(
                id = 2,
                name = "La Sacerdotisa",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_02_high_priestess",
                uprightMeaning = "Intuición, sabiduría interior, conocimiento oculto, misterio",
                reversedMeaning = "Secretos, desconexión de la intuición, represión"
            ),
            TarotCard(
                id = 3,
                name = "La Emperatriz",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_03_empress",
                uprightMeaning = "Abundancia, naturaleza, fertilidad, belleza, crianza",
                reversedMeaning = "Dependencia, sofocación, vacío creativo"
            ),
            TarotCard(
                id = 4,
                name = "El Emperador",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_04_emperor",
                uprightMeaning = "Autoridad, estructura, control, padre, liderazgo",
                reversedMeaning = "Tiranía, rigidez, dominación excesiva"
            ),
            TarotCard(
                id = 5,
                name = "El Hierofante",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_05_hierophant",
                uprightMeaning = "Tradición, conformidad, moralidad, enseñanza",
                reversedMeaning = "Rebelión, subversión, nuevos enfoques"
            ),
            TarotCard(
                id = 6,
                name = "Los Enamorados",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_06_lovers",
                uprightMeaning = "Amor, armonía, relaciones, elecciones importantes",
                reversedMeaning = "Desequilibrio, conflicto de valores, decisiones pobres"
            ),
            TarotCard(
                id = 7,
                name = "El Carro",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_07_chariot",
                uprightMeaning = "Dirección, control, voluntad, victoria, determinación",
                reversedMeaning = "Falta de control, agresividad, obstáculos"
            ),
            TarotCard(
                id = 8,
                name = "La Fuerza",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_08_strength",
                uprightMeaning = "Coraje, persuasión, influencia, compasión, valentía",
                reversedMeaning = "Debilidad interior, duda, baja autoestima"
            ),
            TarotCard(
                id = 9,
                name = "El Ermitaño",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_09_hermit",
                uprightMeaning = "Introspección, soledad, guía interior, búsqueda espiritual",
                reversedMeaning = "Aislamiento, soledad no deseada, rechazo"
            ),
            TarotCard(
                id = 10,
                name = "La Rueda de la Fortuna",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_10_wheel",
                uprightMeaning = "Cambio, ciclos, destino, puntos de inflexión",
                reversedMeaning = "Mala suerte, resistencia al cambio, ciclos negativos"
            ),
            TarotCard(
                id = 11,
                name = "La Justicia",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_11_justice",
                uprightMeaning = "Justicia, equidad, verdad, causa y efecto, ley",
                reversedMeaning = "Injusticia, falta de responsabilidad, deshonestidad"
            ),
            TarotCard(
                id = 12,
                name = "El Colgado",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_12_hanged",
                uprightMeaning = "Pausa, rendición, dejar ir, nueva perspectiva",
                reversedMeaning = "Estancamiento, retraso, resistencia"
            ),
            TarotCard(
                id = 13,
                name = "La Muerte",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_13_death",
                uprightMeaning = "Finales, transformación, transición, liberación",
                reversedMeaning = "Resistencia al cambio, estancamiento, apego"
            ),
            TarotCard(
                id = 14,
                name = "La Templanza",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_14_temperance",
                uprightMeaning = "Balance, moderación, paciencia, propósito, significado",
                reversedMeaning = "Desequilibrio, exceso, falta de visión"
            ),
            TarotCard(
                id = 15,
                name = "El Diablo",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_15_devil",
                uprightMeaning = "Ataduras, adicción, sexualidad, materialismo",
                reversedMeaning = "Liberación, ruptura de cadenas, recuperación"
            ),
            TarotCard(
                id = 16,
                name = "La Torre",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_16_tower",
                uprightMeaning = "Cambio repentino, revelación, destrucción necesaria",
                reversedMeaning = "Evitar el desastre, miedo al cambio, crisis personal"
            ),
            TarotCard(
                id = 17,
                name = "La Estrella",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_17_star",
                uprightMeaning = "Esperanza, fe, rejuvenecimiento, renovación, espiritualidad",
                reversedMeaning = "Desesperanza, desilusión, desconexión"
            ),
            TarotCard(
                id = 18,
                name = "La Luna",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_18_moon",
                uprightMeaning = "Ilusión, miedo, ansiedad, subconsciente, intuición",
                reversedMeaning = "Liberación del miedo, verdad revelada, claridad"
            ),
            TarotCard(
                id = 19,
                name = "El Sol",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_19_sun",
                uprightMeaning = "Alegría, éxito, celebración, positividad, vitalidad",
                reversedMeaning = "Negatividad, depresión, tristeza, pesimismo"
            ),
            TarotCard(
                id = 20,
                name = "El Juicio",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_20_judgement",
                uprightMeaning = "Juicio, renacimiento, perdón, llamado interior",
                reversedMeaning = "Autocrítica, duda, incapacidad de perdonar"
            ),
            TarotCard(
                id = 21,
                name = "El Mundo",
                arcanaType = ArcanaType.MAJOR,
                imageResName = "major_21_world",
                uprightMeaning = "Finalización, logro, viaje, cumplimiento",
                reversedMeaning = "Incompletud, falta de cierre, búsqueda continúa"
            )
        )
    }

    // TODO: Agregar los 56 arcanos menores
    // private fun getMinorArcana(): List<TarotCard> { ... }
}

