/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.tables.records


import java.time.LocalDate

import net.yewton.petclinic.jooq.tables.Pets

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class PetsRecord private constructor() : UpdatableRecordImpl<PetsRecord>(Pets.PETS) {

    open var id: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var name: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var birthDate: LocalDate?
        set(value): Unit = set(2, value)
        get(): LocalDate? = get(2) as LocalDate?

    open var typeId: Int
        set(value): Unit = set(3, value)
        get(): Int = get(3) as Int

    open var ownerId: Int?
        set(value): Unit = set(4, value)
        get(): Int? = get(4) as Int?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised PetsRecord
     */
    constructor(id: Int? = null, name: String? = null, birthDate: LocalDate? = null, typeId: Int, ownerId: Int? = null): this() {
        this.id = id
        this.name = name
        this.birthDate = birthDate
        this.typeId = typeId
        this.ownerId = ownerId
        resetChangedOnNotNull()
    }
}
