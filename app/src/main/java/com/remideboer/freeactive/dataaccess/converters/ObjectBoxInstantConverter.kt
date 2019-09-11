package com.remideboer.freeactive.dataaccess.converters

import io.objectbox.converter.PropertyConverter
import org.threeten.bp.Instant

class ObjectBoxInstantConverter: PropertyConverter<Instant, Long> {
    override fun convertToDatabaseValue(entityProperty: Instant?): Long? {
        return entityProperty?.toEpochMilli()
    }

    override fun convertToEntityProperty(databaseValue: Long?): Instant? {
        return databaseValue?.let { Instant.ofEpochMilli(it) }
    }
}
