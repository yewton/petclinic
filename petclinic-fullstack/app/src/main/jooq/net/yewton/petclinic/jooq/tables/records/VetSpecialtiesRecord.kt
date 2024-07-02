/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.tables.records


import net.yewton.petclinic.jooq.tables.VetSpecialties

import org.jooq.impl.TableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class VetSpecialtiesRecord() : TableRecordImpl<VetSpecialtiesRecord>(VetSpecialties.VET_SPECIALTIES) {

    open var vetId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var specialtyId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    /**
     * Create a detached, initialised VetSpecialtiesRecord
     */
    constructor(vetId: Int? = null, specialtyId: Int? = null): this() {
        this.vetId = vetId
        this.specialtyId = specialtyId
        resetChangedOnNotNull()
    }
}
