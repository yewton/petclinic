/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.tables


import java.time.LocalDate

import kotlin.collections.Collection
import kotlin.collections.List

import net.yewton.petclinic.jooq.Public
import net.yewton.petclinic.jooq.keys.CONSTRAINT_25
import net.yewton.petclinic.jooq.keys.CONSTRAINT_256
import net.yewton.petclinic.jooq.keys.CONSTRAINT_256B
import net.yewton.petclinic.jooq.keys.CONSTRAINT_96
import net.yewton.petclinic.jooq.tables.Owners.OwnersPath
import net.yewton.petclinic.jooq.tables.Types.TypesPath
import net.yewton.petclinic.jooq.tables.Visits.VisitsPath
import net.yewton.petclinic.jooq.tables.records.PetsRecord

import org.jooq.Condition
import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Identity
import org.jooq.InverseForeignKey
import org.jooq.Name
import org.jooq.Path
import org.jooq.PlainSQL
import org.jooq.QueryPart
import org.jooq.Record
import org.jooq.SQL
import org.jooq.Schema
import org.jooq.Select
import org.jooq.Stringly
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class Pets(
    alias: Name,
    path: Table<out Record>?,
    childPath: ForeignKey<out Record, PetsRecord>?,
    parentPath: InverseForeignKey<out Record, PetsRecord>?,
    aliased: Table<PetsRecord>?,
    parameters: Array<Field<*>?>?,
    where: Condition?
): TableImpl<PetsRecord>(
    alias,
    Public.PUBLIC,
    path,
    childPath,
    parentPath,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table(),
    where,
) {
    companion object {

        /**
         * The reference instance of <code>PUBLIC.PETS</code>
         */
        val PETS: Pets = Pets()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<PetsRecord> = PetsRecord::class.java

    /**
     * The column <code>PUBLIC.PETS.ID</code>.
     */
    val ID: TableField<PetsRecord, Int?> = createField(DSL.name("ID"), SQLDataType.INTEGER.nullable(false).identity(true), this, "")

    /**
     * The column <code>PUBLIC.PETS.NAME</code>.
     */
    val NAME: TableField<PetsRecord, String?> = createField(DSL.name("NAME"), SQLDataType.CLOB, this, "")

    /**
     * The column <code>PUBLIC.PETS.BIRTH_DATE</code>.
     */
    val BIRTH_DATE: TableField<PetsRecord, LocalDate?> = createField(DSL.name("BIRTH_DATE"), SQLDataType.LOCALDATE, this, "")

    /**
     * The column <code>PUBLIC.PETS.TYPE_ID</code>.
     */
    val TYPE_ID: TableField<PetsRecord, Int?> = createField(DSL.name("TYPE_ID"), SQLDataType.INTEGER.nullable(false), this, "")

    /**
     * The column <code>PUBLIC.PETS.OWNER_ID</code>.
     */
    val OWNER_ID: TableField<PetsRecord, Int?> = createField(DSL.name("OWNER_ID"), SQLDataType.INTEGER, this, "")

    private constructor(alias: Name, aliased: Table<PetsRecord>?): this(alias, null, null, null, aliased, null, null)
    private constructor(alias: Name, aliased: Table<PetsRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, null, aliased, parameters, null)
    private constructor(alias: Name, aliased: Table<PetsRecord>?, where: Condition?): this(alias, null, null, null, aliased, null, where)

    /**
     * Create an aliased <code>PUBLIC.PETS</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>PUBLIC.PETS</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>PUBLIC.PETS</code> table reference
     */
    constructor(): this(DSL.name("PETS"), null)

    constructor(path: Table<out Record>, childPath: ForeignKey<out Record, PetsRecord>?, parentPath: InverseForeignKey<out Record, PetsRecord>?): this(Internal.createPathAlias(path, childPath, parentPath), path, childPath, parentPath, PETS, null, null)

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    open class PetsPath : Pets, Path<PetsRecord> {
        constructor(path: Table<out Record>, childPath: ForeignKey<out Record, PetsRecord>?, parentPath: InverseForeignKey<out Record, PetsRecord>?): super(path, childPath, parentPath)
        private constructor(alias: Name, aliased: Table<PetsRecord>): super(alias, aliased)
        override fun `as`(alias: String): PetsPath = PetsPath(DSL.name(alias), this)
        override fun `as`(alias: Name): PetsPath = PetsPath(alias, this)
        override fun `as`(alias: Table<*>): PetsPath = PetsPath(alias.qualifiedName, this)
    }
    override fun getSchema(): Schema? = if (aliased()) null else Public.PUBLIC
    override fun getIdentity(): Identity<PetsRecord, Int?> = super.getIdentity() as Identity<PetsRecord, Int?>
    override fun getPrimaryKey(): UniqueKey<PetsRecord> = CONSTRAINT_25
    override fun getReferences(): List<ForeignKey<PetsRecord, *>> = listOf(CONSTRAINT_256, CONSTRAINT_256B)

    private lateinit var _types_: TypesPath

    /**
     * Get the implicit join path to the <code>PUBLIC.TYPES</code> table.
     */
    fun types_(): TypesPath {
        if (!this::_types_.isInitialized)
            _types_ = TypesPath(this, CONSTRAINT_256, null)

        return _types_;
    }

    val types_: TypesPath
        get(): TypesPath = types_()

    private lateinit var _owners: OwnersPath

    /**
     * Get the implicit join path to the <code>PUBLIC.OWNERS</code> table.
     */
    fun owners(): OwnersPath {
        if (!this::_owners.isInitialized)
            _owners = OwnersPath(this, CONSTRAINT_256B, null)

        return _owners;
    }

    val owners: OwnersPath
        get(): OwnersPath = owners()

    private lateinit var _visits: VisitsPath

    /**
     * Get the implicit to-many join path to the <code>PUBLIC.VISITS</code>
     * table
     */
    fun visits(): VisitsPath {
        if (!this::_visits.isInitialized)
            _visits = VisitsPath(this, null, CONSTRAINT_96.inverseKey)

        return _visits;
    }

    val visits: VisitsPath
        get(): VisitsPath = visits()
    override fun `as`(alias: String): Pets = Pets(DSL.name(alias), this)
    override fun `as`(alias: Name): Pets = Pets(alias, this)
    override fun `as`(alias: Table<*>): Pets = Pets(alias.qualifiedName, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): Pets = Pets(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): Pets = Pets(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): Pets = Pets(name.qualifiedName, null)

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Condition?): Pets = Pets(qualifiedName, if (aliased()) this else null, condition)

    /**
     * Create an inline derived table from this table
     */
    override fun where(conditions: Collection<Condition>): Pets = where(DSL.and(conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(vararg conditions: Condition?): Pets = where(DSL.and(*conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Field<Boolean?>?): Pets = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(condition: SQL): Pets = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String): Pets = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg binds: Any?): Pets = where(DSL.condition(condition, *binds))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg parts: QueryPart): Pets = where(DSL.condition(condition, *parts))

    /**
     * Create an inline derived table from this table
     */
    override fun whereExists(select: Select<*>): Pets = where(DSL.exists(select))

    /**
     * Create an inline derived table from this table
     */
    override fun whereNotExists(select: Select<*>): Pets = where(DSL.notExists(select))
}
