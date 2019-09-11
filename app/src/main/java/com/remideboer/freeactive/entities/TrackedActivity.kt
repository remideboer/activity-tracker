package com.remideboer.freeactive.entities

import com.remideboer.freeactive.dataaccess.converters.ObjectBoxDurationConverter
import com.remideboer.freeactive.dataaccess.converters.ObjectBoxInstantConverter
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.threeten.bp.Duration
import org.threeten.bp.Instant


/**
 * The container to store tracked activity data
 * route can be used to calculate distance and average speed using duration
 * so those are not stored separately
 *
 * Note: Activity Type will be added later
 */
@Entity
data class TrackedActivity(@Id var id: Long = 0,
                           @Convert(converter = ObjectBoxInstantConverter::class, dbType = Long::class)
                           var startInstant: Instant? = null,
                           @Convert(converter = ObjectBoxInstantConverter::class, dbType = Long::class)
                           var endInstant: Instant? = null,
                           @Convert(converter = ObjectBoxDurationConverter::class, dbType = Long::class)
                           var duration: Duration? = null,
                           var route: String? = null
)