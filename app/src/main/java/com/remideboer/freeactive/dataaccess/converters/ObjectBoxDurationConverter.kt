package com.remideboer.freeactive.dataaccess.converters

import io.objectbox.converter.PropertyConverter
import org.threeten.bp.Duration

class ObjectBoxDurationConverter: PropertyConverter<Duration, Long>{
    override fun convertToDatabaseValue(entityProperty: Duration?): Long? {
        return entityProperty?.toMillis()
    }

    override fun convertToEntityProperty(databaseValue: Long?): Duration? {
        return databaseValue?.let { Duration.ofMillis(it) }
    }
}
