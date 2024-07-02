/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.tables.records


import net.yewton.petclinic.jooq.tables.Specialties

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SpecialtiesRecord() : UpdatableRecordImpl<SpecialtiesRecord>(Specialties.SPECIALTIES) {

    open var id: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var name: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised SpecialtiesRecord
     */
    constructor(id: Int? = null, name: String? = null): this() {
        this.id = id
        this.name = name
        resetChangedOnNotNull()
    }
}
