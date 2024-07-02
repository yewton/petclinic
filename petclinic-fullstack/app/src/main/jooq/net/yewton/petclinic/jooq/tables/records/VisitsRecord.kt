/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.tables.records


import java.time.LocalDate

import net.yewton.petclinic.jooq.tables.Visits

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class VisitsRecord() : UpdatableRecordImpl<VisitsRecord>(Visits.VISITS) {

    open var id: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var petId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    open var visitDate: LocalDate?
        set(value): Unit = set(2, value)
        get(): LocalDate? = get(2) as LocalDate?

    open var description: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised VisitsRecord
     */
    constructor(id: Int? = null, petId: Int? = null, visitDate: LocalDate? = null, description: String? = null): this() {
        this.id = id
        this.petId = petId
        this.visitDate = visitDate
        this.description = description
        resetChangedOnNotNull()
    }
}
