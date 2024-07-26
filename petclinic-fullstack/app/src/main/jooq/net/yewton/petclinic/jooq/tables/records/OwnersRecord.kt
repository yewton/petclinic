/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.tables.records


import net.yewton.petclinic.jooq.tables.Owners

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class OwnersRecord private constructor() : UpdatableRecordImpl<OwnersRecord>(Owners.OWNERS) {

    open var id: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var firstName: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var lastName: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var address: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var city: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    open var telephone: String?
        set(value): Unit = set(5, value)
        get(): String? = get(5) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised OwnersRecord
     */
    constructor(id: Int? = null, firstName: String? = null, lastName: String? = null, address: String? = null, city: String? = null, telephone: String? = null): this() {
        this.id = id
        this.firstName = firstName
        this.lastName = lastName
        this.address = address
        this.city = city
        this.telephone = telephone
        resetChangedOnNotNull()
    }
}
