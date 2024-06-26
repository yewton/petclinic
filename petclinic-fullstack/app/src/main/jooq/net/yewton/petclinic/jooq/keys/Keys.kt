/*
 * This file is generated by jOOQ.
 */
package net.yewton.petclinic.jooq.keys


import net.yewton.petclinic.jooq.tables.Owners
import net.yewton.petclinic.jooq.tables.Pets
import net.yewton.petclinic.jooq.tables.Specialties
import net.yewton.petclinic.jooq.tables.Types
import net.yewton.petclinic.jooq.tables.VetSpecialties
import net.yewton.petclinic.jooq.tables.Vets
import net.yewton.petclinic.jooq.tables.Visits
import net.yewton.petclinic.jooq.tables.records.OwnersRecord
import net.yewton.petclinic.jooq.tables.records.PetsRecord
import net.yewton.petclinic.jooq.tables.records.SpecialtiesRecord
import net.yewton.petclinic.jooq.tables.records.TypesRecord
import net.yewton.petclinic.jooq.tables.records.VetSpecialtiesRecord
import net.yewton.petclinic.jooq.tables.records.VetsRecord
import net.yewton.petclinic.jooq.tables.records.VisitsRecord

import org.jooq.ForeignKey
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal



// -------------------------------------------------------------------------
// UNIQUE and PRIMARY KEY definitions
// -------------------------------------------------------------------------

val CONSTRAINT_8: UniqueKey<OwnersRecord> = Internal.createUniqueKey(Owners.OWNERS, DSL.name("CONSTRAINT_8"), arrayOf(Owners.OWNERS.ID), true)
val CONSTRAINT_25: UniqueKey<PetsRecord> = Internal.createUniqueKey(Pets.PETS, DSL.name("CONSTRAINT_25"), arrayOf(Pets.PETS.ID), true)
val CONSTRAINT_4: UniqueKey<SpecialtiesRecord> = Internal.createUniqueKey(Specialties.SPECIALTIES, DSL.name("CONSTRAINT_4"), arrayOf(Specialties.SPECIALTIES.ID), true)
val CONSTRAINT_4C: UniqueKey<TypesRecord> = Internal.createUniqueKey(Types.TYPES, DSL.name("CONSTRAINT_4C"), arrayOf(Types.TYPES.ID), true)
val CONSTRAINT_7AC: UniqueKey<VetSpecialtiesRecord> = Internal.createUniqueKey(VetSpecialties.VET_SPECIALTIES, DSL.name("CONSTRAINT_7AC"), arrayOf(VetSpecialties.VET_SPECIALTIES.VET_ID, VetSpecialties.VET_SPECIALTIES.SPECIALTY_ID), true)
val CONSTRAINT_2: UniqueKey<VetsRecord> = Internal.createUniqueKey(Vets.VETS, DSL.name("CONSTRAINT_2"), arrayOf(Vets.VETS.ID), true)
val CONSTRAINT_9: UniqueKey<VisitsRecord> = Internal.createUniqueKey(Visits.VISITS, DSL.name("CONSTRAINT_9"), arrayOf(Visits.VISITS.ID), true)

// -------------------------------------------------------------------------
// FOREIGN KEY definitions
// -------------------------------------------------------------------------

val CONSTRAINT_256: ForeignKey<PetsRecord, TypesRecord> = Internal.createForeignKey(Pets.PETS, DSL.name("CONSTRAINT_256"), arrayOf(Pets.PETS.TYPE_ID), net.yewton.petclinic.jooq.keys.CONSTRAINT_4C, arrayOf(Types.TYPES.ID), true)
val CONSTRAINT_256B: ForeignKey<PetsRecord, OwnersRecord> = Internal.createForeignKey(Pets.PETS, DSL.name("CONSTRAINT_256B"), arrayOf(Pets.PETS.OWNER_ID), net.yewton.petclinic.jooq.keys.CONSTRAINT_8, arrayOf(Owners.OWNERS.ID), true)
val CONSTRAINT_7: ForeignKey<VetSpecialtiesRecord, VetsRecord> = Internal.createForeignKey(VetSpecialties.VET_SPECIALTIES, DSL.name("CONSTRAINT_7"), arrayOf(VetSpecialties.VET_SPECIALTIES.VET_ID), net.yewton.petclinic.jooq.keys.CONSTRAINT_2, arrayOf(Vets.VETS.ID), true)
val CONSTRAINT_7A: ForeignKey<VetSpecialtiesRecord, SpecialtiesRecord> = Internal.createForeignKey(VetSpecialties.VET_SPECIALTIES, DSL.name("CONSTRAINT_7A"), arrayOf(VetSpecialties.VET_SPECIALTIES.SPECIALTY_ID), net.yewton.petclinic.jooq.keys.CONSTRAINT_4, arrayOf(Specialties.SPECIALTIES.ID), true)
val CONSTRAINT_96: ForeignKey<VisitsRecord, PetsRecord> = Internal.createForeignKey(Visits.VISITS, DSL.name("CONSTRAINT_96"), arrayOf(Visits.VISITS.PET_ID), net.yewton.petclinic.jooq.keys.CONSTRAINT_25, arrayOf(Pets.PETS.ID), true)
