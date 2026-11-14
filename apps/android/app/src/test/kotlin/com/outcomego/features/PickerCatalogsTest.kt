package com.outcomego.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerCatalogsTest {
    @Test
    fun timeZonesIncludeStandardAndCustomAndFilterSpaces() {
        val zones = PickerCatalogs.knownTimeZones("Custom/My_Zone")
        assertTrue("UTC" in zones)
        assertTrue("Asia/Shanghai" in zones)
        assertTrue("America/New_York" in zones)
        assertTrue("Custom/My_Zone" in zones)
        assertEquals("America/New York", PickerCatalogs.formatTimeZoneDisplay("America/New_York"))

        val matched = PickerCatalogs.filterTimeZones(listOf("Asia/Shanghai", "America/New_York"), "new york")
        assertEquals(listOf("America/New_York"), matched)
    }

    @Test
    fun schedulePickersIncludePresetsAndCurrentValues() {
        val minutes = PickerCatalogs.minuteOptions(17)
        assertEquals(13, minutes.size)
        assertTrue(0 in minutes && 55 in minutes && 17 in minutes)
        assertEquals(":05", PickerCatalogs.formatHourlyMinute(5))

        val times = PickerCatalogs.timeOptions(9 * 60 + 7)
        assertEquals(97, times.size)
        assertTrue(9 * 60 + 7 in times)
        assertEquals("09:07", PickerCatalogs.formatClockTime(9 * 60 + 7))

        assertEquals((1..7).toList(), PickerCatalogs.weekdayOptions())
        assertEquals((1..31).toList(), PickerCatalogs.monthDayOptions())

        val initial = RoutineSchedule(frequency = RoutineSchedule.Frequency.Interval, intervalAmount = 45, intervalUnit = RoutineSchedule.IntervalUnit.Minutes)
        val changedToHours = PickerCatalogs.changeIntervalUnit(initial, RoutineSchedule.IntervalUnit.Hours)
        assertEquals(RoutineSchedule.IntervalUnit.Hours, changedToHours.intervalUnit)
        assertEquals(1, changedToHours.intervalAmount)

        val validInDays = RoutineSchedule(frequency = RoutineSchedule.Frequency.Interval, intervalAmount = 7, intervalUnit = RoutineSchedule.IntervalUnit.Days)
        val preservedInHours = PickerCatalogs.changeIntervalUnit(validInDays.copy(intervalAmount = 2), RoutineSchedule.IntervalUnit.Hours)
        assertEquals(2, preservedInHours.intervalAmount)
    }

    @Test
    fun botAvatarAndVolcengineCatalogsMatchExpectedCountsAndSearch() {
        assertEquals(12, PickerCatalogs.robotAvatarShapes.size)
        assertEquals(11, PickerCatalogs.botAvatarColors.size)
        assertTrue(PickerCatalogs.robotAvatarShapes.any { it.id == "chip" })
        assertTrue(PickerCatalogs.botAvatarColors.any { it.hex.equals("#a47952", ignoreCase = true) })

        assertEquals(294, PickerCatalogs.volcengineVoices.size)
        assertNotNull(PickerCatalogs.findVolcengineVoice("zh_female_vv_uranus_bigtts"))
        val searched = PickerCatalogs.filterVolcengineVoices("Vivi")
        assertTrue(searched.any { it.id == "zh_female_vv_uranus_bigtts" })
    }
}
