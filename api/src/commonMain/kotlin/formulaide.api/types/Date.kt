package formulaide.api.types

import kotlinx.serialization.Serializable

/**
 * A date, represented in the format `yyyy-mm-dd`.
 *
 * @property year `0..3000`
 * @property month `1..12`
 * @property day `1..31`
 */
@Serializable
data class Date(
	val year: Int,
	val month: Int,
	val day: Int,
) {

	init {
		require(year in 0..3_000) { "L'année est invalide : $year" }
		require(month in 1..12) { "Le mois est invalide : $month" }
		require(day in 1..31) { "Le jour est invalide : $day" }
	}

	override fun toString() = "$year-$month-$day"
}

/**
 * A time, represented in `hh:mm`.
 *
 * @property hour `0..23`
 * @property minute `0..59`
 */
@Serializable
data class Time(
	val hour: Int,
	val minute: Int,
) {

	init {
		require(hour in 0..23) { "L'heure est invalide : $hour" }
		require(minute in 0..59) { "Les minutes sont invalides : $minute" }
	}

	override fun toString() = "$hour:$minute"
}
