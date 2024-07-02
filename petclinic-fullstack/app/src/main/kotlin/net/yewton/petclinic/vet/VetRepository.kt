package net.yewton.petclinic.vet

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import net.yewton.petclinic.jooq.tables.references.VETS
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.jooq.kotlin.intoList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Component
class VetRepository(private val create: DSLContext) {
    @Transactional(readOnly = true)
    suspend fun findAll(pageable: Pageable): Page<Vet> {
        val query = create.select(
            VETS.ID,
            VETS.FIRST_NAME,
            VETS.LAST_NAME,
            multiset(
                selectFrom(VETS.specialties)
            ).intoList { Specialty(it.name) },
            count().over()
        ).from(VETS)
            .orderBy(VETS.ID.asc())
            .limit(pageable.pageSize)
            .offset(pageable.offset)
        val result = Flux.from(query)
            .map {
                val vet = Vet(it.get(VETS.ID), it.get(VETS.FIRST_NAME), it.get(VETS.LAST_NAME), it.value4())
                val totalCount = it.value5()
                vet to totalCount
            }.asFlow().toList().toMap()
        return PageImpl(result.keys.toList(), pageable, result.values.first().toLong())
    }
}
